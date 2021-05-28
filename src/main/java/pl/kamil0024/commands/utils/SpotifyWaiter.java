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

package pl.kamil0024.commands.utils;

import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Track;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.DynamicEmbedPageinator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.utils.UserCredentials;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class SpotifyWaiter {

    private static final String ONE = "\u0031\u20E3";
    private static final String TWO = "\u0032\u20E3";
    private static final String THREE = "\u0033\u20E3";

    private final User user;
    private final TextChannel channel;
    private final EventWaiter eventWaiter;
    private final UserCredentials userCredentials;

    private Message botMsg = null;

    private Choose a1 = null;
    private Choose b2 = null;

    public SpotifyWaiter(User user, TextChannel channel, EventWaiter eventWaiter, UserCredentials userCredentials) {
        this.user = user;
        this.channel = channel;
        this.eventWaiter = eventWaiter;
        this.userCredentials = userCredentials;
    }

    public void create() {
        botMsg = channel.sendMessage(String.format("%s, wybierz co chcesz sprawdzić! " +
                "\n:one: Swoich ulubionych artystów" +
                "\n:two: Najczęściej słuchane tracki", user.getAsMention())).complete();
        botMsg.addReaction(ONE).complete();
        botMsg.addReaction(TWO).complete();
        waitForReaction();
    }

    private void waitForReaction() {
        eventWaiter.waitForEvent(MessageReactionAddEvent.class, this::checkReaction,
                this::onMessageReactionAdd, 60, TimeUnit.SECONDS, this::clearReactions);
    }

    private void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!event.getMessageId().equals(botMsg.getId()) || !event.getUser().getId().equals(user.getId())) return;
        if (!event.getReactionEmote().isEmote()) {
            switch (event.getReactionEmote().getName()) {
                case ONE:
                    if (a1 == null) a1 = Choose.ARTISTS;
                    else if (b2 == null) b2 = Choose.SHORT;
                    break;
                case TWO:
                    if (a1 == null) a1 = Choose.TRACK;
                    else if (b2 == null) b2 = Choose.LONG;
                    break;
                case THREE:
                    b2 = Choose.ALL;
            }
        }
        try {
            event.getReaction().removeReaction(event.getUser()).queue();
        } catch (PermissionException ignored) { }

        if (a1 != null && b2 != null) {
            List<FutureTask<EmbedBuilder>> futurePages = new ArrayList<>();

            BetterStringBuilder sb = new BetterStringBuilder();
            int nr = 1;
            try {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(Color.green);
                eb.setTimestamp(Instant.now());
                switch (a1) {
                    case ARTISTS:
                        for (Artist artist : userCredentials.getApi().getUsersTopArtists().time_range(b2.s).limit(10).build().execute().getItems()) {
                            eb.setTitle(String.format("%s. %s", nr, artist.getName()), "https://open.spotify.com/artist/" + artist.getId());
                            eb.setDescription("Twoi topowi artyści z okresu: " + b2.s2);
                            if (artist.getImages().length >= 1) eb.setImage(artist.getImages()[0].getUrl());
                            if (sb.toString().isEmpty()) sb.appendLine("Twoi topowi artyści z okresu: " + b2.s2 + "\n");
                            sb.appendLine(String.format("%s. [%s](%s)", nr, artist.getName(), "https://open.spotify.com/artist/" + artist.getId()));
                            nr++;
                            futurePages.add(new FutureTask<>(() -> eb));
                        }
                        break;
                    case TRACK:
                        for (Track item : userCredentials.getApi().getUsersTopTracks().time_range(b2.s).limit(10).build().execute().getItems()) {
                            eb.setTitle(String.format("%s. %s", nr, item.getName()), "https://open.spotify.com/track/" + item.getId());
                            eb.setDescription("Twoje topowe piosenki z okresu: " + b2.s2);
                            if (item.getAlbum().getImages().length >= 1) eb.setImage(item.getAlbum().getImages()[0].getUrl());
                            if (sb.toString().isEmpty()) sb.appendLine("Twoje topowe piosenki z okresu: " + b2.s2 + "\n");
                            sb.appendLine(String.format("%s. [%s - %s](%s)", nr, item.getArtists()[0].getName(), item.getName(), "https://open.spotify.com/track/" + item.getId()));
                            nr++;
                            futurePages.add(new FutureTask<>(() -> eb));
                        }
                }
            } catch (Exception e) {
                botMsg.editMessage("Wystąpił błąd przy uzyskiwaniu piosenek!").queue();
                Log.newError(e, getClass());
                return;
            }
            clearReactions();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.blue);
            eb.setTimestamp(Instant.now());
            eb.setDescription(sb.build());
            futurePages.add(new FutureTask<>(() -> eb));

            new DynamicEmbedPageinator(futurePages, user, eventWaiter, 120).create(botMsg);
        } else {
            botMsg.addReaction(THREE).complete();
            botMsg.editMessage(String.format("%s, wybierz z jakiego okresu chcesz uzyskać dane" +
                    "\n:one: 4 tygodni" +
                    "\n:two: 6 miesięcy" +
                    "\n:three: kilku lat", user.getAsMention())).complete();
            waitForReaction();
        }
    }

    private boolean checkReaction(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() == botMsg.getIdLong() && !event.getReactionEmote().isEmote() && !event.getUser().isBot()) {
            switch (event.getReactionEmote().getName()) {
                case ONE:
                case TWO:
                case THREE:
                    return event.getUser().getId().equals(user.getId());
                default:
                    return false;
            }
        } else return false;
    }

    private void clearReactions() {
        try {
            botMsg.clearReactions().complete();
        } catch (Exception ignored) {/*lul*/}
    }

    @AllArgsConstructor
    public enum Choose {
        TRACK(null, null), ARTISTS(null, null),

        SHORT("short_term", "**4 tygodni**"),
        LONG("medium_term", "**6 miesięcy**"),
        ALL("long_term", "**kilku lat**");

        public String s;
        public String s2;
    }

}
