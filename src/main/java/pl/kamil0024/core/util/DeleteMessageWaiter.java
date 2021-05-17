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

package pl.kamil0024.core.util;

import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class DeleteMessageWaiter implements Waiter<MessageReceivedEvent> {

    private final String userId;
    private final TextChannel channel;
    private final EventWaiter eventWaiter;
    private final String bannedUser;

    private Message botMsg;
    private final DowodWaiter dowodWaiter;

    public void start() {
        botMsg = channel.sendMessage(String.format("<@%s>, możesz wybrać usunięcie wiadomości użytkownika z okresu do 7 dni! Wpisz odpowiednią liczbę lub napisz **0** jeżeli nie chcesz usuwać wiadomości", userId)).complete();
        waitForMessage();
    }

    @Override
    public void waitForMessage() {
        eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                this::event, 40, TimeUnit.SECONDS, this::clear);
    }

    @Override
    public boolean checkMessage(MessageReceivedEvent e) {
        if (!e.getAuthor().getId().equals(userId)) return false;
        if (e.getMessage().getContentRaw().equalsIgnoreCase("anuluj")) {
            clear();
            return false;
        }
        return e.isFromGuild() && e.getTextChannel().getId().equals(channel.getId());
    }

    private void clear() {
        try {
            botMsg.delete().complete();
        } catch (Exception ignored) { }
        dowodWaiter.start();
    }

    @Override
    public void event(MessageReceivedEvent e) {
        Message msg;
        try {
            msg = e.getTextChannel().retrieveMessageById(e.getMessageId()).complete();
        } catch (Exception ex) {
            waitForMessage();
            return;
        }

        try {
            int i = Math.max(Integer.parseInt(msg.getContentRaw()), 0);
            if (i <= 7) channel.getGuild().ban(bannedUser, i).complete();
        } catch (Exception ex) {
            e.getChannel().sendMessage("Nie udało się usunąć wiadomości! Error:" + ex.getLocalizedMessage())
                    .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
        }
        clear();
        try {
            msg.delete().complete();
        } catch (Exception ignored) { }
    }

}
