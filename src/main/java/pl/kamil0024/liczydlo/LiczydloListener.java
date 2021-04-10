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

package pl.kamil0024.liczydlo;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import pl.kamil0024.core.Ustawienia;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LiczydloListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (!event.getChannel().getId().equals(Ustawienia.instance.channel.liczek)) return;

        if (event.getAuthor().isBot() || event.getMessage().isWebhookMessage()) {
            event.getMessage().delete().queue();
            return;
        }

        String msg = event.getMessage().getContentRaw();
        if (msg.isEmpty()) {
            event.getMessage().delete().queue();
            return;
        }

        int liczba;

        try {
            liczba = Integer.parseInt(msg);
        } catch (NumberFormatException e) {
            event.getMessage().delete().queue();
            return;
        }

        List<Message> messages = getHistoryList(event.getChannel());
        int kiedysMessage = Integer.parseInt(messages.get(1).getContentRaw());

        if (messages.size() < 2 || liczba != kiedysMessage+1 || messages.get(1).getAuthor().getId().equals(event.getAuthor().getId())) {
            event.getMessage().delete().queue();
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        if (!event.getChannel().getId().equals(Ustawienia.instance.channel.liczek)) return;
        event.getMessage().delete().complete();
    }

    private List<Message> getHistoryList(TextChannel txt) {
        List<Message> msgs = new ArrayList<>();
        for (Message msg : txt.getIterableHistory().stream().limit(3).collect(Collectors.toList())) {
            if (!msg.isEdited()) {
                msgs.add(msg);
                if (msgs.size() == 2) break;
            } else msg.delete().queue();
        }
        return msgs;
    }

}
