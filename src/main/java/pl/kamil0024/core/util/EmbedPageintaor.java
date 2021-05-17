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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class EmbedPageintaor {

    public static final String FIRST_EMOJI = "\u23EE";
    public static final String LEFT_EMOJI = "\u25C0";
    public static final String RIGHT_EMOJI = "\u25B6";
    public static final String LAST_EMOJI = "\u23ED";
    public static final String STOP_EMOJI = "\u23F9";

    private final EventWaiter eventWaiter;
    private final List<EmbedBuilder> pages;
    private int thisPage = 1;
    private boolean isPun;
    private boolean customFooter = false;

    private Message botMsg;
    private long botMsgId;
    private final long userId;
    private final int secound;

    public static final String[] values;

    static {
        values = new String[] {FIRST_EMOJI, LEFT_EMOJI, RIGHT_EMOJI, LAST_EMOJI, STOP_EMOJI};
    }

    public EmbedPageintaor(List<EmbedBuilder> pages, User user, EventWaiter eventWaiter, int secound) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.userId = user.getIdLong();
        this.secound = secound;
    }

    public EmbedPageintaor(List<EmbedBuilder> pages, User user, EventWaiter eventWaiter) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.userId = user.getIdLong();
        this.secound = 60;
    }

    public EmbedPageintaor create(MessageChannel channel) {
        //noinspection DuplicatedCode
        channel.sendMessage(render(1)).override(true).queue(msg -> {
            botMsg = msg;
            botMsgId = msg.getIdLong();
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
        return this;
    }

    public EmbedPageintaor create(MessageChannel channel, Message mes) {
        //noinspection DuplicatedCode
        channel.sendMessage(render(1)).reference(mes).override(true).queue(msg -> {
            botMsg = msg;
            botMsgId = msg.getIdLong();
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
        return this;
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                this::onMessageReactionAdd, secound, TimeUnit.SECONDS, this::clearReactions);
    }

    private void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getUser() == null ||
                event.getUserIdLong() != userId ||
                event.getMessageIdLong() != botMsgId) return;

        if (!event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case FIRST_EMOJI:
                    thisPage = 1;
                    break;
                case LEFT_EMOJI:
                    if (thisPage > 1) thisPage--;
                    break;
                case RIGHT_EMOJI:
                    if (thisPage < pages.size()) thisPage++;
                    break;
                case LAST_EMOJI:
                    thisPage = pages.size();
                    break;
                case STOP_EMOJI:
                    if (isPun) botMsg.delete().queue();
                    return;
                default:
                    return;
            }
        }
        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) { }
        botMsg.editMessage(render(thisPage)).override(true).complete();
        waitForReaction();
    }

    private void addReactions(Message message) {
        for (String s : values) {
            message.addReaction(s).queue();
        }
    }

    private void clearReactions() {
        if (!isPun) {
            try {
                botMsg.clearReactions().complete();
            } catch (Exception ignored) {/*lul*/}
        }
    }

    private boolean checkReaction(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() == botMsgId && !event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case FIRST_EMOJI:
                case LEFT_EMOJI:
                case RIGHT_EMOJI:
                case LAST_EMOJI:
                case STOP_EMOJI:
                    return event.getUserIdLong() == userId;
                default:
                    return false;
            }
        }
        return false;
    }

    private MessageEmbed render(int page) {
        EmbedBuilder pageEmbed = pages.get(page - 1);
        if (!customFooter) pageEmbed.setFooter(String.format("%s/%s", page, pages.size()), null);
        return pageEmbed.build();
    }

    public EmbedPageintaor setPun(boolean bol) {
        isPun = bol;
        return this;
    }

    public EmbedPageintaor setCustomFooter(boolean bol) {
        this.customFooter = bol;
        return this;
    }

}
