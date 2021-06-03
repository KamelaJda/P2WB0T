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
import lombok.Data;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Data
public class ButtonWaiter {

    private static final Logger logger = LoggerFactory.getLogger(ButtonWaiter.class);

    private final User author;
    private final List<ButtonWaiterAction> buttonWaiterActionList;
    private final EventWaiter eventWaiter;

    private Message botMsg;
    private boolean disableComponentOnClick = false;
    private MessageAction messageAction;

    public ButtonWaiter(EventWaiter eventWaiter, User author, MessageAction messageAction, List<ButtonWaiterAction> buttonWaiterActionList) {
        this.buttonWaiterActionList = buttonWaiterActionList;
        this.author = author;
        this.messageAction = messageAction;
        this.eventWaiter = eventWaiter;
    }

    public void create() {
        List<Button> collect = getButtonWaiterActionList().stream().map(ButtonWaiterAction::getButton).collect(Collectors.toList());
        setBotMsg(getMessageAction().setActionRows(ActionRow.of(collect)).complete());
        waitForEvent();
    }

    public void waitForEvent() {
        eventWaiter.waitForEvent(ButtonClickEvent.class,
                this::check,
                this::handleEvent,
                60, TimeUnit.SECONDS,
                this::clear);
    }

    private boolean check(ButtonClickEvent e) {
        return e.getMessageId().equals(getBotMsg().getId()) && e.getUser().getId().equals(author.getId());
    }

    private void handleEvent(ButtonClickEvent e) {
        if (e.getButton() == null || e.getButton().getId() == null) return;
        InteractionHook hook = e.getHook();

        e.deferEdit().queue(); // potwierdza, że przycisk został kliknięty. Inaczej wywali "Żądanie nie zostało przetworzone"

        for (ButtonWaiterAction action : getButtonWaiterActionList()) {
            if (action.getButton().getId() == null) {
                continue;
            }

            if (action.getButton().getId().equals(e.getButton().getId())) {
                action.getAction().accept(e);
                if (isDisableComponentOnClick()) { // Wyłącza przyciski
                    List<Button> collect = getButtonWaiterActionList().stream().map(m -> m.getButton().asDisabled()).collect(Collectors.toList());
                    hook.editOriginalComponents(ActionRow.of(collect)).complete();
                }
            }
        }
    }

    private void clear() {
        botMsg.delete().complete(); // nie ma opcji usunięcia/wyłączenia przycisków
    }

    @Data
    @AllArgsConstructor
    public static class ButtonWaiterAction {
        private final Button button;
        private final Consumer<ButtonClickEvent> action;
    }

}