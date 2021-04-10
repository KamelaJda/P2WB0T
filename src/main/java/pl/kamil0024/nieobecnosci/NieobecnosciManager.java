/*
 *
 *    Copyright 2020 P2WB0T
 *
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package pl.kamil0024.nieobecnosci;


import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import pl.kamil0024.bdate.Timespan;
import pl.kamil0024.commands.system.CytujCommand;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.config.NieobecnosciConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;
import pl.kamil0024.nieobecnosci.config.Zmiana;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NieobecnosciManager {

    private final ShardManager api;
    private final NieobecnosciDao nieobecnosciDao;

    public NieobecnosciManager(ShardManager api, NieobecnosciDao nieobecnosciDao) {
        this.api = api;
        this.nieobecnosciDao = nieobecnosciDao;

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::xd, 0, 30, TimeUnit.MINUTES);
    }

    public synchronized void put(Message msg, long start, String powod, long end) {
        if (powod.isEmpty()) return;

        Nieobecnosc nb = new Nieobecnosc();
        nb.setUserId(msg.getAuthor().getId());
        nb.setId(nieobecnosciDao.getNextId(msg.getAuthor().getId()));
        nb.setStart(start);
        nb.setPowod(powod);
        nb.setEnd(end);
        nb.setAktywna(true);

        TextChannel txt = msg.getGuild().getTextChannelById(Ustawienia.instance.channel.nieobecnosci);
        if (txt == null) {
            Log.newError("Kanał do nieobecnosci jest nullem!", NieobecnosciManager.class);
            return;
        }

        MessageEmbed eb = getEmbed(nb, msg.getMember()).build();

        Message botmsg = txt.sendMessage(eb).complete();
        botmsg.addReaction(Objects.requireNonNull(CommandExecute.getReaction(msg.getAuthor(), false))).queue();

        nb.setMsgId(botmsg.getId());

        NieobecnosciConfig xd = nieobecnosciDao.get(msg.getAuthor().getId());
        xd.getNieobecnosc().add(nb);
        nieobecnosciDao.save(xd);
        msg.delete().queue();
        Zmiana.startNieobecnosci(nb, msg.getMember());
    }

    public static EmbedBuilder getEmbed(Nieobecnosc nieobecnosc, Member member) {
        return getEmbed(nieobecnosc, member, false);
    }

    public static EmbedBuilder getEmbed(Nieobecnosc nieobecnosc, Member member, boolean priv) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        EmbedBuilder eb = new EmbedBuilder();

        eb.setColor(UserUtil.getColor(member));
        eb.setAuthor(UserUtil.getMcNick(member), null, member.getUser().getAvatarUrl());
        eb.setThumbnail(member.getUser().getAvatarUrl());

        eb.addField("Osoba zgłaszająca", member.getAsMention() + " " + MarkdownSanitizer.escape(UserUtil.getMcNick(member)), false);
        if (priv) eb.addField("Powód", nieobecnosc.getPowod(), false);
        else eb.addField("Powód", "Dostępne pod /nieobecnosc " + member.getId(), false);
        if (nieobecnosc.isAktywna()) {
            eb.addField("Czas rozpoczęcia", sdf.format(new Date(nieobecnosc.getStart())), false);
            eb.addField("Czas zakończenia", sdf.format(new Date(nieobecnosc.getEnd())), false);
            eb.addField("Pozostało", new Timespan(new Date().getTime(), ModLog.getLang()).difference(nieobecnosc.getEnd()), false);
        }
        eb.setFooter("ID: " + nieobecnosc.getId() + " | Ostatnia aktualizacja:");
        eb.setTimestamp(Instant.now());

        return eb;
    }

    public void xd() { update(); }

    @SuppressWarnings("DuplicatedCode")
    public synchronized void update() {
        TextChannel txt = api.getTextChannelById(Ustawienia.instance.channel.nieobecnosci);

        if (txt == null) {
            Log.newError("Kanał do nieobecnosci jest nullem", NieobecnosciManager.class);
            return;
        }
        long now = new Date().getTime();
        for (Nieobecnosc nb : nieobecnosciDao.getAllAktywne()) {
            try {
                Message msg = CytujCommand.kurwaJDA(txt, nb.getMsgId());
                Member mem = null;
                try {
                    mem = Objects.requireNonNull(api.getGuildById(Ustawienia.instance.rekrutacyjny.guildId)).retrieveMemberById(nb.getUserId()).complete();
                } catch (Exception ignored) { }
                if (mem == null) {
                    Log.newError("Jezu " + nb.getUserId() + " wyszedł z serwera i nie mogę zaaktualizować nieobecności", NieobecnosciManager.class);
                    continue;
                }

                if (msg == null) {
                    Log.newError("Nieobecnosc usera " + nb.getUserId() + " o ID " + nb.getId() + " nie ma wiadomosci!", NieobecnosciManager.class);
                    continue;
                }

                if (nb.getEnd() - now <= 0) {
                    try {
                        NieobecnosciConfig nbc = nieobecnosciDao.get(nb.getUserId());
                        msg.delete().queue();
                        nbc.getNieobecnosc().remove(nb);
                        nb.setAktywna(false);
                        nbc.getNieobecnosc().add(nb);
                        nieobecnosciDao.save(nbc);
                        Zmiana.endNieobecnosci(nb, mem);
                        PrivateChannel pv = mem.getUser().openPrivateChannel().complete();
                        pv.sendMessage("Twój urlop właśnie się zakończył!").complete();
                    } catch (Exception ignored) {}
                    continue;
                }
                msg.editMessage(getEmbed(nb, mem).build()).queue();
            } catch (Exception e) {
                e.printStackTrace();
                Log.newError(e, getClass());
            }
        }
    }
}
