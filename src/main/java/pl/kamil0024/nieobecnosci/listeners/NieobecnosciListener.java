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

package pl.kamil0024.nieobecnosci.listeners;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.commands.system.CytujCommand;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.config.NieobecnosciConfig;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.nieobecnosci.NieobecnosciManager;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;
import pl.kamil0024.nieobecnosci.config.Zmiana;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NieobecnosciListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NieobecnosciListener.class);

    private final NieobecnosciDao nieobecnosciDao;
    private final NieobecnosciManager nieobecnosciManager;

    public NieobecnosciListener(NieobecnosciDao nieobecnosciDao, NieobecnosciManager nieobecnosciManager) {
        this.nieobecnosciDao = nieobecnosciDao;
        this.nieobecnosciManager = nieobecnosciManager;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (!e.getChannel().getId().equals(Ustawienia.instance.channel.nieobecnosci) || e.getAuthor().isBot()) return;

        String[] msg = e.getMessage().getContentRaw().split("\n");
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy");
        String log = "Chciano dać urlop dla " + e.getAuthor().getId() + " ale ";
        String powod;
        long start, end;

        NieobecnosciConfig nbc = nieobecnosciDao.get(e.getAuthor().getId());
        if (e.getMessage().getContentRaw().contains("Przedłużam:")) {
            Nieobecnosc xd = null;
            for (Nieobecnosc nieobecnosc : nbc.getNieobecnosc()) {
                if (nieobecnosc.isAktywna()) {
                    xd = nieobecnosc;
                    nbc.getNieobecnosc().remove(nieobecnosc);
                    break;
                }
            }
            if (xd == null) {
                e.getChannel().sendMessage(e.getAuthor().getAsMention() + " nie masz aktywnej nieobecnosci").
                        queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                return;
            }

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                Date nowyCzas = sfd.parse(e.getMessage().getContentRaw().split("Przedłużam: ")[1]);
                Date staryCzas = new Date(xd.getEnd());
                if (nowyCzas.before(staryCzas)) {
                    e.getChannel().sendMessage(e.getAuthor().getAsMention() + ", nie bądź jak Ewitan, nie przedłużaj nieobecności w tył").
                            queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                Zmiana zmiana = new Zmiana();
                zmiana.setCoZmienia(Zmiana.Enum.EXTENSION);
                zmiana.setKiedy(new Date().getTime());
                zmiana.setKtoZmienia(e.getAuthor().getId());

                String stary = String.format("Stary czas: `%s`\n", sdf.format(staryCzas));
                String nowy = String.format("Nowy czas: `%s`", sdf.format(nowyCzas));
                zmiana.setKomentarz(stary + nowy);
                zmiana.sendLog(e.getGuild(), xd.getUserId(), xd.getId());
                if (xd.getZmiany() == null) xd.setZmiany(new ArrayList<>());
                xd.getZmiany().add(zmiana);
                xd.setEnd(nowyCzas.getTime());
                nbc.getNieobecnosc().add(xd);
                nieobecnosciDao.save(nbc);
                nieobecnosciManager.update();
                e.getMessage().delete().queue();
            } catch (Exception takkk) {
                logger.debug(log + "parser przedłużania jest zły");
            }
            return;
        }

        // msg[0] - data rozpoczęcia
        // msg[1] - powód
        // msg[2] - data zakończenia

        try {
            start = sfd.parse(msg[0].split(": ")[1]).getTime();
            powod = msg[1].split(": ")[1];
            end = sfd.parse(msg[2].split(": ")[1]).getTime();
        } catch (Exception xd) {
            e.getChannel().sendMessage(e.getAuthor().getAsMention() + ", brak zastosowania do wzoru!").
                    queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
            logger.debug(log + "parser jest zły");
            return;
        }

        for (Nieobecnosc nieobecnosc : nbc.getNieobecnosc()) {
            if (nieobecnosc.isAktywna()) {
                e.getChannel().sendMessage(e.getAuthor().getAsMention() + " masz jeszcze niezakończoną nieobecność o ID" + nieobecnosc.getId() + "!").
                        queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                return;
            }
        }

        if (start >= end) {
            e.getChannel().sendMessage(e.getAuthor().getAsMention() + " data zakończenia jest wcześniejsza od daty rozpoczęcia").
                    queue( m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            return;
        }

        nieobecnosciManager.put(e.getMessage(), start, powod, end);
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        synchronized (e.getGuild().getId()) {
            if (!e.getChannel().getId().equals(Ustawienia.instance.channel.nieobecnosci) || e.getMember().getUser().isBot()) return;

            if (e.getReactionEmote().isEmoji() || !e.getReactionEmote().getEmote().getId().equals(Ustawienia.instance.emote.red)) return;

            for (Nieobecnosc nieobecnosc : nieobecnosciDao.getAllAktywne()) {
                if (nieobecnosc.getMsgId().equals(e.getMessageId())) {
                    if (nieobecnosc.getUserId().equals(e.getMember().getId()) || UserUtil.getPermLevel(e.getMember()).getNumer() >= PermLevel.ADMINISTRATOR.getNumer()) {
                        NieobecnosciConfig nbc = nieobecnosciDao.get(nieobecnosc.getUserId());
                        nbc.getNieobecnosc().remove(nieobecnosc);
                        nieobecnosc.setAktywna(false);
                        Zmiana zmiana = new Zmiana();
                        zmiana.setKiedy(new Date().getTime());
                        zmiana.setKtoZmienia(e.getUser().getId());
                        zmiana.setCoZmienia(Zmiana.Enum.CANCEL);
                        nbc.getNieobecnosc().add(nieobecnosc);
                        nieobecnosciDao.save(nbc);

                        try {
                            PrivateChannel pv = e.getJDA().retrieveUserById(nieobecnosc.getUserId()).complete().openPrivateChannel().complete();
                            pv.sendMessage("Twój urlop o ID " + nieobecnosc.getId() + " został anulowany przez administratora!").complete();
                        } catch (Exception ignored) { }

                        Message msg = CytujCommand.kurwaJDA(e.getChannel(), e.getMessageId());
                        if (msg == null) continue;
                        msg.delete().queue();
                        break;
                    }
                }
            }

        }
    }
}
