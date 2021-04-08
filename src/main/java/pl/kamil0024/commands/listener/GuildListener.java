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

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.moderation.commands.MuteCommand;
import pl.kamil0024.core.Ustawienia;

public class GuildListener extends ListenerAdapter {

    public GuildListener() { }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        if (MuteCommand.hasMute(event.getMember())) {
            Message msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
            if (msg == null) return; // tak

            MessageReaction.ReactionEmote emote = event.getReactionEmote();
            if (emote.isEmote()) msg.removeReaction(emote.getEmote(), event.getUser()).queue();
            else msg.removeReaction(emote.getEmoji(), event.getUser()).queue();
        }
    }

}
