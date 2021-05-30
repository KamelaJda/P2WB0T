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

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.List;
import java.util.concurrent.*;

@SuppressWarnings("DuplicatedCode")
public class DynamicEmbedPaginator {

    private static final ExecutorService mainExecutor = Executors.newFixedThreadPool(8);

    private final EventWaiter eventWaiter;
    private final List<FutureTask<EmbedBuilder>> pages;
    private final long userId;
    private final int secound;
    private final boolean ended = false;

    private int thisPage = 1;
    private boolean isPun;

    private Message botMsg;
    private long botMsgId;

    private boolean loading = true;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(mainExecutor::shutdown));
    }

    public DynamicEmbedPaginator(List<FutureTask<EmbedBuilder>> pages, User user, EventWaiter eventWaiter, int secound) {
        this.eventWaiter = eventWaiter;
        this.pages = pages;
        this.userId = user.getIdLong();
        this.secound = secound;
        mainExecutor.submit(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(2, new NamedThreadFactory("PageLoader-" +
                    userId + "-" + botMsgId + "-" + pages.size() + "-pages"));
            pages.forEach(executor::execute);
            while (!pages.stream().allMatch(FutureTask::isDone)) {
                try {
                    if (ended) {
                        pages.forEach(f -> f.cancel(true));
                        break;
                    }
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            executor.shutdownNow();
            setLoading(false);
        });
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        if (botMsg != null) botMsg.editMessage(render(thisPage)).override(true).queue();
    }

    public DynamicEmbedPaginator create(MessageChannel channel, Message mess) {
        channel.sendMessage(render(1)).reference(mess).override(true).queue(msg -> {
            botMsg = msg;
            botMsgId = msg.getIdLong();
            if (pages.size() != 1) {
                addReactions(msg);
                waitForReaction();
            }
        });
        return this;
    }

    public DynamicEmbedPaginator create(Message message) {
        message.editMessage(render(1)).override(true).queue(msg -> {
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
                event.getUser().getIdLong() != userId ||
                event.getMessageIdLong() != botMsgId) return;

        if (!event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case EmbedPaginator.FIRST_EMOJI:
                    thisPage = 1;
                    break;
                case EmbedPaginator.LEFT_EMOJI:
                    if (thisPage > 1) thisPage--;
                    break;
                case EmbedPaginator.RIGHT_EMOJI:
                    if (thisPage < pages.size()) thisPage++;
                    break;
                case EmbedPaginator.LAST_EMOJI:
                    thisPage = pages.size();
                    break;
                case EmbedPaginator.STOP_EMOJI:
                    botMsg.delete().queue();
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
        for (String s : EmbedPaginator.values) {
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
        if (event.getMessageIdLong() == botMsgId && !event.getReactionEmote().isEmote() && !event.getUser().isBot()) {
            switch (event.getReactionEmote().getName()) {
                case EmbedPaginator.FIRST_EMOJI:
                case EmbedPaginator.LEFT_EMOJI:
                case EmbedPaginator.RIGHT_EMOJI:
                case EmbedPaginator.LAST_EMOJI:
                case EmbedPaginator.STOP_EMOJI:
                    return event.getUser().getIdLong() == userId;
                default:
                    return false;
            }
        }
        return false;
    }

    private MessageEmbed render(int page) {
        FutureTask<EmbedBuilder> pageEmbed = pages.get(page - 1);
        EmbedBuilder eb;
        if (!pageEmbed.isDone()) mainExecutor.submit(pageEmbed);
        try {
            if (page == 1) {
                if (pageEmbed.get() == null) throw new IllegalStateException("pEmbed == null");
                eb = new EmbedBuilder(pageEmbed.get().build());
            } else {
                EmbedBuilder pEmbed = pageEmbed.get(5, TimeUnit.SECONDS);
                if (pEmbed == null) throw new IllegalStateException("pEmbed == null");
                eb = new EmbedBuilder(pEmbed.build());
            }
        } catch (TimeoutException e) {
            botMsg.getChannel().sendMessage("Ta strona jest jeszcze wczytywana. Poczekaj chwilę!")
                    .queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS));
            throw new LoadingException();
        } catch (ExecutionException e) {
            throw new LoadingException();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        eb.setFooter(String.format("%s/%s", page, pages.size()), null);
        if (loading && pages.size() > 1) eb.setFooter(String.format("%s/%s", page, pages.size())
                + " ⌛", null);
        return eb.build();
    }

    public DynamicEmbedPaginator setPun(boolean bol) {
        isPun = bol;
        return this;
    }

    private static class LoadingException extends RuntimeException {
        @Getter
        private final boolean firstPage;

        LoadingException() {
            this(false);
        }

        LoadingException(boolean firstPage) {
            this.firstPage = firstPage;
        }
    }

}
