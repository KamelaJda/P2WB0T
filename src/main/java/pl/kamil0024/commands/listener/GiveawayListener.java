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

package pl.kamil0024.commands.listener;

import com.google.inject.Inject;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.GiveawayDao;
import pl.kamil0024.core.database.config.GiveawayConfig;
import pl.kamil0024.core.logger.Log;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
public class GiveawayListener {

    private static final String TADA = "\uD83C\uDF89";

    @Inject private GiveawayDao giveawayDao;
    @Inject private ShardManager api;

    public List<String> konkursMsg;

    public GiveawayListener(GiveawayDao giveawayDao, ShardManager api) {
        this.giveawayDao = giveawayDao;
        this.api = api;
        List<GiveawayConfig> tak = giveawayDao.getAll();
        this.konkursMsg = new ArrayList<>();
        tak.removeIf(e -> !e.isAktywna() && e.getMessageId() == null);
        tak.forEach(e -> getKonkursMsg().add(String.valueOf(e.getMessageId())));

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::update, 0, 8, TimeUnit.MINUTES);
    }

    public void createMessage(GiveawayConfig kc) {
        if (kc.getMessageId() != null) throw new UnsupportedOperationException("Próbowano zdublować wiadomość o konkursie " + kc.getId());

        TextChannel tc = api.getTextChannelById(kc.getKanalId());
        if (tc == null) throw new UnsupportedOperationException("Próbowano stworzyć wiadomość o konkursie " + kc.getId() + " ale kanał jest nieprawidłowy");
        if (!tc.canTalk()) throw new UnsupportedOperationException("Próbowano stworzyć konkurs o id " + kc.getId() + " ale bot nie ma permów do pisania na kanale " + tc.getIdLong());

        Message msg = tc.sendMessage(createEmbed(kc).build()).complete();
        msg.addReaction(TADA).queue();
        kc.setMessageId(msg.getId());
        getKonkursMsg().add(msg.getId());
        giveawayDao.save(kc);
    }

    public EmbedBuilder createEmbed(GiveawayConfig kc) {
        EmbedBuilder eb = new EmbedBuilder();

        if (kc.isAktywna()) eb.setColor(Color.cyan);
        else eb.setColor(Color.red);
        eb.setTitle(TADA + " Konkurs");
        eb.addField("Nagroda", kc.getNagroda(), false);
        eb.addField("Wygra osób", String.valueOf(kc.getWygranychOsob()), false);

        User u = api.retrieveUserById(kc.getOrganizator()).complete();
        eb.addField("Organizowany przez", u.getAsMention(), false);
        eb.setThumbnail(u.getAvatarUrl());

        if (kc.isAktywna()) {
            BDate bd = new BDate(ModLog.getLang());
            SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy @ HH:mm:ss");
            try {
                eb.addField("Koniec o", String.format("%s (%s)",
                        sfd.format(kc.getEnd()),
                        bd.difference(kc.getEnd())), false);
            } catch (Exception e) {
                eb.addField("Koniec o", "? (?)", false);
            }
        } else {
            eb.addField("Koniec o", "Konkurs się zakończył!", false);

            StringBuilder sb = new StringBuilder();
            String f = "<@%s>";
            for (String winner : kc.getWinners()) {
                sb.append(String.format(f, winner)).append("`,` ");
            }
            eb.addField("Wygrali", sb.toString(), false);
        }

        eb.setTimestamp(Instant.now());
        eb.setFooter("ID: " + kc.getId() + " | Ostatnia aktualizacja");

        return eb;
    }

    public void update() {
        xd();
    }

    private synchronized void xd() {
        Random rand = new Random();
        List<GiveawayConfig> kc = giveawayDao.getAll();
        kc.removeIf(k -> !k.isAktywna());

        for (GiveawayConfig config : kc) {
            try {
                TextChannel tc = api.getTextChannelById(config.getKanalId());

                boolean end = false;
                if (config.getEnd() - new Date().getTime() <= 0) {
                    config.setAktywna(false);
                    end = true;
                    giveawayDao.save(config);
                }

                Message msg = null;
                try {
                    msg = Objects.requireNonNull(tc).retrieveMessageById(config.getMessageId()).complete();
                } catch (Exception ignored) { }

                if (msg == null) {
                    config.setMessageId(null);
                    createMessage(config);
                    continue;
                }

                if (end) {
                    List<String> listaLudzi = new ArrayList<>();
                    List<String> wygrani = new ArrayList<>();

                    for (MessageReaction rec : msg.getReactions()) {
                        if (rec.getReactionEmote().isEmoji() && rec.getReactionEmote().getEmoji().equals(TADA)) {
                            listaLudzi = rec.retrieveUsers().complete().stream()
                                    .filter(u -> !u.isBot())
                                    .map(User::getId)
                                    .collect(Collectors.toList());
                        }
                    }

                    while (config.getWygranychOsob() != wygrani.size()) {
                        String wygral = listaLudzi.get(rand.nextInt(listaLudzi.size() - 1));
                        wygrani.add(wygral);
                        listaLudzi.remove(wygral);
                    }

                    config.setWinners(wygrani);
//                    msg.clearReactions().queue();

                    StringBuilder sb = new StringBuilder();
                    String f = "<@%s>";
                    String link = "https://discord.com/channels/%s/%s/%s";
                    link = String.format(link, Ustawienia.instance.bot.guildId, config.getKanalId(), config.getMessageId());
                    for (String winner : wygrani) {
                        sb.append(String.format(f, winner)).append("`,` ");
                    }
                    msg.getChannel().sendMessage(TADA + " Gratulacje dla tych osób: " + sb.toString() +
                            "\nWygrali: " + config.getNagroda() +
                            "\n\n" + link).allowedMentions(Collections.singleton(Message.MentionType.USER)).queue();
                    giveawayDao.save(config);
                }

                msg.editMessage(createEmbed(config).build()).queue();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
