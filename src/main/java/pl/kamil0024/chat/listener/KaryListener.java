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

package pl.kamil0024.chat.listener;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import pl.kamil0024.chat.Action;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.moderation.commands.MuteCommand;
import pl.kamil0024.moderation.commands.PunishCommand;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class KaryListener extends ListenerAdapter {

    @Getter
    private final Cache<Action> embedy;

    private final KaryJSON karyJSON;
    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;

    public KaryListener(KaryJSON karyJSON, CaseDao caseDao, ModLog modLog, StatsModule statsModule, RedisManager redisManager) {
        this.karyJSON = karyJSON;
        this.caseDao = caseDao;
        this.modLog = modLog;
        this.statsModule = statsModule;
        this.embedy = redisManager.new CacheRetriever<Action>() {}.getCache(-1);
    }

    @Override
    public void onButtonClick(ButtonClickEvent event) {
        if (!event.isFromGuild() ||
                event.getGuild() == null ||
                !event.getGuild().getId().equals(Ustawienia.instance.bot.guildId) ||
                !event.getChannel().getId().equals(Ustawienia.instance.channel.moddc) ||
                event.getUser().isBot() ||
                UserUtil.getPermLevel(event.getMember()).getNumer() == PermLevel.MEMBER.getNumer()) return;
        check(event);
    }

    private synchronized void check(ButtonClickEvent e) {
        Action action = embedy.getIfPresent(e.getMessageId());
        if (action == null || e.getButton() == null || e.getButton().getId() == null || e.getGuild() == null) return;
        e.deferEdit().queue();

        switch (e.getButton().getId()) {
            case "1":
                Dowod d = new Dowod();
                d.setId(1);
                d.setUser(e.getUser().getId());
                d.setContent("Wystawione automatycznie. Treść wiadomości poniżej.\n\n" + MarkdownSanitizer.escape(action.getMsg().getContent()));
                d.setImage(null);

                Member mem = null;
                try {
                    mem = e.getGuild().retrieveMemberById(action.getMsg().getAuthor()).complete();
                } catch (Exception ignored) { }

                if (mem == null) {
                    e.getChannel().sendMessage(e.getUser().getAsMention() + ", użytkownik wyszedł z serwera??")
                            .queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }
                if (MuteCommand.hasMute(mem)) {
                    e.getChannel().sendMessage(e.getUser().getAsMention() + ", użytkownik jest wyciszony!")
                            .queue(m -> m.delete().queueAfter(10, TimeUnit.SECONDS));
                    return;
                }

                KaryJSON.Kara kara = karyJSON.getByName(action.getKara().getPowod());
                if (kara == null) {
                    e.getChannel().sendMessage(e.getUser().getAsMention() + ", kara `" + action.getKara().getPowod() + "` jest źle wpisana!").queue();
                    return;
                }

                PunishCommand.putPun(kara, Collections.singletonList(mem), e.getMember(), e.getTextChannel(), caseDao, modLog, statsModule, d, null);
                embedy.invalidate(e.getMessageId());
                deleteMessage(e.getMessage());
                try {
                    deleteMessage(e.getGuild().getTextChannelById(action.getMsg().getChannel()).retrieveMessageById(action.getMsg().getId()).complete());
                } catch (Exception ignored) { }
                break;
            case "2":
                deleteMessage(e.getMessage());
                embedy.invalidate(e.getMessageId());
                break;
            case "3":
                try {
                    deleteMessage(e.getGuild().getTextChannelById(action.getMsg().getChannel()).retrieveMessageById(action.getMsg().getId()).complete());
                } catch (Exception ignored) { }
                deleteMessage(e.getMessage());
        }

    }

    private void deleteMessage(Message... m) {
        for (Message message : m) {
            try {
                message.delete().complete();
            } catch (Exception ignored) { }
        }
    }

}
