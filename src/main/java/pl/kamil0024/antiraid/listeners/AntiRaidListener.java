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

package pl.kamil0024.antiraid.listeners;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.antiraid.managers.AntiRaidManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.AntiRaidDao;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.AntiRaidConfig;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.moderation.commands.TempbanCommand;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.Collections;
import java.util.Objects;

@AllArgsConstructor
public class AntiRaidListener extends ListenerAdapter {

    private final AntiRaidManager antiRaidManager;
    private final AntiRaidDao dao;
    private final CaseDao caseDao;
    private final ModLog modLog;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.isFromGuild() || event.getAuthor().isBot()
                || UserUtil.getPermLevel(event.getMember()).getNumer() > PermLevel.MEMBER.getNumer()
                || event.getMessage().getContentRaw().isEmpty()
                || event.getChannel().getId().equals("426864003562864641")
                || event.getChannel().getId().equals("652927860943880224"))
            return;
        try {
            antiRaidManager.saveMessage(event.getAuthor().getId(), event.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getMember().getUser().isBot() || !event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)
                || !event.getChannel().getId().equals(Ustawienia.instance.channel.moddc)) return;
        if (UserUtil.getPermLevel(event.getMember()).getNumer() == PermLevel.MEMBER.getNumer()) return;

        AntiRaidConfig arc = dao.get(event.getMessageId());
        if (arc == null) return;

        Member member = null;
        Message msg;

        try {
            msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
        } catch (Exception ignored) {
            event.getChannel().sendMessage("Nie udało się jakimś cudem pobrać wiadomości. Spróbuj ponownie!").complete();
            return;
        }

        try {
            member = event.getGuild().retrieveMemberById(arc.getUserId()).complete();
        } catch (Exception ignored) { }

        if (member == null) {
            event.getChannel().sendMessage("Użytkownik wyszedł z serwera?").complete();
            dao.delete(arc.getId());
            msg.delete().complete();
            return;
        }

        if (msg == null || event.getReactionEmote().getId().equals(Ustawienia.instance.emote.red)) {
            try {
                event.getGuild().removeRoleFromMember(arc.getUserId(), Objects.requireNonNull(event.getGuild().getRoleById(Ustawienia.instance.muteRole))).complete();
            } catch (Exception e) {
                event.getChannel().sendMessage("Nie udało się usunąć roli!").complete();
            }
            if (msg != null) msg.delete().complete();
            return;
        }

        Dowod d = new Dowod();
        d.setId(0);
        d.setUser(event.getUserId());
        d.setContent("Zbanowano przez antiraida - dowód wystawiony automatycznie. Wiadomości użytkownika znajdują się poniżej\n\n" + arc.getMessages());
        String tak = TempbanCommand.tempban(member.getUser(), event.getUser(),
                String.format("Raid (%s)", arc.getReason()), "1d", caseDao, modLog, false,
                event.getGuild(), UserUtil.getMcNick(member), 1, Collections.singletonList(d));

        if (tak != null) event.getChannel().sendMessage(tak).complete();
        msg.delete().complete();
    }

}
