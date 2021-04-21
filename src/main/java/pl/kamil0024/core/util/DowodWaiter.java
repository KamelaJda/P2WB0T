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
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.moderation.commands.DowodCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class DowodWaiter {

    private final String userId;
    private final CaseConfig cc;
    private final CaseDao cd;
    private final TextChannel channel;
    private final EventWaiter eventWaiter;

    private Message botMsg;

    public void start() {
        botMsg = channel.sendMessage(String.format("<@%s>, zapisz dowód... (jeżeli takowego nie ma, napisz `anuluj`)\n**NIE WPISUJ KOLEJNEJ KOMENDY JEŻELI NIE MA DOWODU, WPISZ `ANULUJ`**", userId)).complete();
        waitForMessage();
    }

    private void waitForMessage() {
        eventWaiter.waitForEvent(MessageReceivedEvent.class, this::checkMessage,
                this::event, 40, TimeUnit.SECONDS, this::clear);
    }

    private boolean checkMessage(MessageReceivedEvent e) {
        if (!e.getAuthor().getId().equals(userId)) return false;
        if (e.getMessage().getContentRaw().equalsIgnoreCase("anuluj")) {
            clear();
            return false;
        }
        return e.isFromGuild() && e.getTextChannel().getId().equals(channel.getId());
    }

    private void clear() {
        try {
            botMsg.delete().completeAfter(5, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
    }

    private void event(MessageReceivedEvent e) {
        Message msg;
        try {
            msg = e.getTextChannel().retrieveMessageById(e.getMessageId()).complete();
        } catch (Exception ex) {
            waitForMessage();
            return;
        }

        List<Dowod> d = DowodCommand.getKaraConfig(msg.getContentRaw(), msg, false);
        if (d == null || d.isEmpty()) {
            e.getTextChannel().sendMessage("Dowód jest pusty?").queue();
            return;
        }
        if (cc.getKara().getDowody() == null) cc.getKara().setDowody(new ArrayList<>());
        for (Dowod dowod : d) {
            cc.getKara().getDowody().add(dowod);
        }
        cd.save(cc);
        clear();
    }

}