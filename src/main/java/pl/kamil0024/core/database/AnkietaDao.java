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

package pl.kamil0024.core.database;

import gg.amy.pgorm.PgMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.config.AnkietaConfig;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.moderation.listeners.ModLog;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AnkietaDao extends ListenerAdapter implements Dao<AnkietaConfig> {

    private final PgMapper<AnkietaConfig> mapper;
    private final ShardManager api;

    public AnkietaDao(DatabaseManager databaseManager, ShardManager api) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(AnkietaConfig.class);
        this.api = api;

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::update, 0, 7, TimeUnit.MINUTES);
        api.addEventListener(this);
    }

    @Override
    public AnkietaConfig get(String id) {
        return mapper.load(id).orElse(null);
    }

    @Override
    public void save(AnkietaConfig toCos) {
        mapper.save(toCos);
    }

    @Override
    public List<AnkietaConfig> getAll() {
        return mapper.loadAll();
    }

    public List<AnkietaConfig> getAllAktywne() {
        return mapper.loadRaw("SELECT * FROM ankieta WHERE data::jsonb @> '{\"aktywna\": true}'");
    }

    public void send(AnkietaConfig config) {
        if (config.getMessageId() != null) {
            Log.newError("Próbowano drugi raz stworzyć wiadomość dla ankiety o id " + config.getId(), getClass());
            throw new UnsupportedOperationException("Próbowano drugi raz stworzyć wiadomość dla ankiety o id " + config.getId());
        }

        TextChannel txt = api.getTextChannelById(Ustawienia.instance.rekrutacyjny.ankietyId);
        if (txt == null) {
            Log.newError("Kanał do ankiet jest nullem", getClass());
            return;
        }

        try {
            Message msg = txt.sendMessage(generateEmbed(config).build()).complete();
            config.getOpcje().forEach(o -> msg.addReaction(o.getEmoji()).queue());
            config.setMessageId(msg.getId());
            save(config);
        } catch (Exception e) {
            Log.newError("Nie udało się wysłać wiadomość o ankiecie o id" + config.getId(), getClass());
            Log.newError(e, getClass());
        }
    }

    public EmbedBuilder generateEmbed(AnkietaConfig config) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd HH:mm");
        Date d = new Date();
        EmbedBuilder eb = new EmbedBuilder();
        eb.setFooter("ID: " + config.getId() + " | Ostatnia aktualizacja");
        eb.setTimestamp(Instant.now());
        if (config.getSendAt() > d.getTime()) {
            eb.setColor(Color.magenta);
            eb.setDescription("Tutaj będzie ankieta :D");
            return eb;
        }

        User autor = null;
        try {
            autor = api.retrieveUserById(config.getAutorId()).complete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (autor != null) eb.setAuthor(UserUtil.getName(autor), null, autor.getAvatarUrl());
        else eb.setAuthor("??? (" + config.getAutorId() + ")");

        BetterStringBuilder bsb = new BetterStringBuilder();
        if (config.isAktywna()) {
            eb.setColor(Color.cyan);
            if (config.getDescription() != null) eb.setDescription(config.getDescription());
            eb.addField("Ankieta rozpoczęta o", sdf.format(new Date(config.getSendAt())), false);
            eb.addField("Koniec ankiety o", sdf.format(new Date(config.getEndAt())) + " (" + new BDate(ModLog.getLang()).difference(config.getEndAt()) + ")", false);
            eb.addField("Opcje ankiety", "1. " + (config.isMultiOptions() ? "Możesz zaznaczyć kilka opcji" : "Możesz zaznaczyć tylko jedną opcje"), false);
            for (AnkietaConfig.Opcja opcja : config.getOpcje()) {
                bsb.appendLine(opcja.getEmoji() + " **-** " + MarkdownSanitizer.escape(opcja.getText()));
            }
            eb.addField("", bsb.toString(), false);
        }
        else {
            eb.setColor(Color.red);
            for (AnkietaConfig.Opcja opcja : config.getOpcje()) {
                Integer glosy = config.getGlosy().getOrDefault(opcja.getId(), 0);
                bsb.appendLine(MarkdownSanitizer.escape(opcja.getText()) + "**:** " + glosy + " głosów");
            }
            eb.setDescription(bsb.toString());
        }
        return eb;
    }

    public void update() {
        long time = new Date().getTime();
        for (AnkietaConfig ac : getAllAktywne()) {
            try {
                TextChannel txt = api.getTextChannelById(Ustawienia.instance.rekrutacyjny.ankietyId);
                if (txt == null) throw new NullPointerException("Kanał do ankiet jest nullem");

                Message msg = txt.retrieveMessageById(ac.getMessageId()).complete();
                if (msg == null) throw new NullPointerException("Wiadomość ankiety o ID " + ac.getId() + " jest nullem");

                if (time >= ac.getEndAt()) {
                    ac.setAktywna(false);

                    for (MessageReaction reaction : msg.getReactions()) {
                        if (reaction.getReactionEmote().isEmoji()) {
                            for (AnkietaConfig.Opcja opcja : ac.getOpcje()) {
                                if (opcja.getEmoji().equals(reaction.getReactionEmote().getEmoji())) {
                                    int glosy = ac.getGlosy().getOrDefault(opcja.getId(), 0);
                                    ac.getGlosy().put(opcja.getId(), glosy + reaction.getCount());
                                }
                            }
                        }
                    }
                    
                    try {
                        msg.clearReactions().complete();
                    } catch (Exception ignored) { }
                    save(ac);
                }

                msg.editMessage(generateEmbed(ac).build()).queue();

            } catch (Exception e) {
                Log.newError(e, getClass());
            }
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        if (e.getChannel().getId().equals(Ustawienia.instance.rekrutacyjny.ankietyId) && !e.getUser().isBot()) {
            AnkietaConfig ankiety = mapper.loadRaw(String.format("SELECT * FROM ankieta WHERE data::jsonb @> '{\"messageId\": \"%s\"}'", e.getMessageId())).stream().findFirst().orElse(null);
            if (ankiety == null) return;

            if (!ankiety.isMultiOptions()) {
                int emotes = 0;
                Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete();
                for (MessageReaction react : msg.getReactions()) {
                    emotes += (int) react.retrieveUsers().complete().stream().filter(re -> re.getId().equals(e.getUserId())).count();
                }
                if (emotes >= 2) msg.removeReaction(e.getReactionEmote().getEmoji(), e.getUser()).complete();
            }
        }
    }

}
