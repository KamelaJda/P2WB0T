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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class EmbedPaginator {

    public static final Button FIRST_BUTTON = Button.secondary("FIRST", Emoji.fromUnicode("\u23EE"));
    public static final Button LEFT_BUTTON = Button.primary("LEFT", Emoji.fromUnicode("\u25C0"));
    public static final Button RIGHT_BUTTON = Button.primary("RIGHT", Emoji.fromUnicode("\u25B6"));
    public static final Button LAST_BUTTON = Button.secondary("LAST", Emoji.fromUnicode("\u23ED"));
    public static final Button STOP_BUTTON = Button.danger("STOP", Emoji.fromUnicode("\u23F9"));

    private final EventWaiter eventWaiter;
    private final List<EmbedBuilder> pages;
    private final long userId;
    private final int secound;

    public static final ActionRow actionRow;

    static {
        actionRow = ActionRow.of(FIRST_BUTTON, LEFT_BUTTON, RIGHT_BUTTON, LEFT_BUTTON, STOP_BUTTON);
    }

    private int thisPage = 1;
    private Message botMsg;

    public EmbedPaginator(List<EmbedBuilder> pages, User user, EventWaiter eventWaiter, int secound) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.userId = user.getIdLong();
        this.secound = secound;
    }

    public EmbedPaginator(List<EmbedBuilder> pages, User user, EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.userId = user.getIdLong();
        this.secound = 60;
    }

    public EmbedPaginator create(MessageChannel channel) {
        channel.sendMessage(render(1)).setActionRows(getActionRow(1)).queue(msg -> {
            botMsg = msg;
            if (pages.size() != 1) waitForReaction();
        });
        return this;
    }

    public EmbedPaginator create(MessageChannel channel, Message mes) {
        channel.sendMessage(render(1)).reference(mes).setActionRows(getActionRow(1)).queue(msg -> {
            botMsg = msg;
            if (pages.size() != 1) waitForReaction();
        });
        return this;
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(ButtonClickEvent.class, this::check,
                this::handle, secound, TimeUnit.SECONDS, this::clear);
    }

    private void handle(ButtonClickEvent event) {
        event.deferEdit().queue();
        switch (event.getComponentId()) {
            case "FIRST":
                thisPage = 1;
                break;
            case "LEFT":
                if (thisPage > 1) thisPage--;
                break;
            case "RIGHT":
                if (thisPage < pages.size()) thisPage++;
                break;
            case "LAST":
                thisPage = pages.size();
                break;
            case "STOP":
                clear();
                return;
        }
        botMsg.editMessage(render(thisPage)).setActionRows(getActionRow(thisPage)).override(true).complete();
        waitForReaction();
    }

    private boolean check(ButtonClickEvent event) {
        if (event.getMessageId().equals(botMsg.getId()) && event.getUser().getIdLong() == userId) {
            switch (event.getComponentId()) {
                case "FIRST":
                case "LEFT":
                case "RIGHT":
                case "LAST":
                case "STOP":
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private void clear() {
        try {
            MessageBuilder mb = new net.dv8tion.jda.api.MessageBuilder();
            mb.setEmbed(botMsg.getEmbeds().get(0));
            mb.setContent(botMsg.getContentRaw());
            mb.setActionRows(Collections.emptyList());
            botMsg.editMessage(mb.build()).complete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MessageEmbed render(int page) {
        EmbedBuilder pageEmbed = pages.get(page - 1);
        pageEmbed.setFooter(String.format("%s/%s", page, pages.size()), null);
        return pageEmbed.build();
    }

    public ActionRow getActionRow(int page) {
        return getActionRow(page, pages);
    }

    public static ActionRow getActionRow(int page, List<?> pages) {
        List<Button> buttons = new ArrayList<>();
        buttons.add(FIRST_BUTTON.withDisabled(page == 1));
        buttons.add(LEFT_BUTTON.withLabel(page == 1 ? "-" : page - 1 + "").withDisabled(page == 1));
        buttons.add(RIGHT_BUTTON.withLabel(pages.size() == page ? "-" : page + 1 + "").withDisabled(pages.size() == page));
        buttons.add(LAST_BUTTON.withDisabled(pages.size() == page));
        buttons.add(STOP_BUTTON);
        return ActionRow.of(buttons);
    }

}
