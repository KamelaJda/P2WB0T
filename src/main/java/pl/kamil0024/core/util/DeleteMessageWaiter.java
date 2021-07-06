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

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class DeleteMessageWaiter implements Waiter<SelectionMenuEvent> {

    private final String userId;
    private final TextChannel channel;
    private final EventWaiter eventWaiter;
    private final String bannedUser;
    private final boolean fake; // TODO: Usuąć

    private Message botMsg;

    public void start() {
        SelectionMenu.Builder sm = SelectionMenu.create("DELETE-MESSAGE-" + userId)
                .setPlaceholder("Wybierz ilość dni...")
                .setRequiredRange(1, 1);

        for (int i = 0; i < 8; i++) {
            if (i == 0) sm.addOption("Nie usuwaj wiadomości", String.valueOf(i));
            else sm.addOption("Usuń wiadomości z " + i + " dni", String.valueOf(i));
        }

        MessageBuilder mb = new MessageBuilder();
        mb.setContent(String.format("<@%s>, możesz wybrać usunięcie wiadomości użytkownika z okresu do 7 dni! Wybierz odpowiednią wartość z menu", userId));
        mb.setActionRows(ActionRow.of(sm.build()));

        botMsg = channel.sendMessage(mb.build()).complete();
        waitForMessage();
    }

    @Override
    public void waitForMessage() {
        eventWaiter.waitForEvent(SelectionMenuEvent.class, this::checkMessage,
                this::event, 40, TimeUnit.SECONDS, this::clear);
    }

    @Override
    public boolean checkMessage(SelectionMenuEvent e) {
        return e.isFromGuild() && e.getComponentId().equals("DELETE-MESSAGE-" + e.getUser().getId());
    }

    private void clear() {
        try {
            botMsg.delete().complete();
        } catch (Exception ignored) { }
    }

    @Override
    public void event(SelectionMenuEvent e) {
        try {
            Guild guild = channel.getGuild();
            int i = Math.max(Integer.parseInt(e.getSelectedOptions().get(0).getValue()), 0);
            if (!fake && i <= 7) {
                guild.unban(bannedUser).complete();
                guild.ban(bannedUser, i).complete();
            }
        } catch (Exception ex) {
            e.getChannel().sendMessage("Nie udało się zbanować! Error:" + ex.getLocalizedMessage()).complete();
        }
        clear();
    }

}
