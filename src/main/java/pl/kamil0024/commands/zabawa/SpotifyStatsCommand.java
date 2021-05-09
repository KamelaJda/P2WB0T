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

package pl.kamil0024.commands.zabawa;

import com.wrapper.spotify.model_objects.specification.Artist;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.DynamicEmbedPageinator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.music.utils.SpotifyUtil;
import pl.kamil0024.music.utils.UserCredentials;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

public class SpotifyStatsCommand extends Command {

    private final SpotifyUtil spotifyUtil;
    private final EventWaiter eventWaiter;

    public SpotifyStatsCommand(SpotifyUtil spotifyUtil, EventWaiter eventWaiter) {
        name = "spotifystats";
        aliases.add("spotify");
        category = CommandCategory.ZABAWA;
        permLevel = PermLevel.DEVELOPER;
        enabledInRekru = true;

        this.spotifyUtil = spotifyUtil;
        this.eventWaiter = eventWaiter;
    }


    @Override
    public boolean execute(CommandContext context) {
        Message msg = context.sendTranslate("generic.loading").complete();
        UserCredentials user = spotifyUtil.getUser(context.getUser().getId());

        if (user == null) {
            msg.delete().queue();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setTimestamp(Instant.now());
            eb.setFooter("P2WB0T");

            String link = String.format("https://accounts.spotify.com/authorize?response_type=code&client_id=%s&scope=user-top-read&redirect_uri=https://discord.p2w.pl/api/spotify/callback", spotifyUtil.getApi().getClientId());

            eb.setDescription("Aby zobaczyć swoje statystyki Spotify musisz połączyć swoje konto Discord z kontem Spotify! " +
                    "Możesz to zrobić wchodząc " + String.format("[tutaj](%s)", link));

            context.send(eb.build()).queue();
            return false;
        }

        int nr = 1;

        try {
            List<FutureTask<EmbedBuilder>> futurePages = new ArrayList<>();
            for (Artist artist : user.getApi().getUsersTopArtists().limit(10).build().execute().getItems()) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setColor(UserUtil.getColor(context.getMember()));
                eb.setTitle(String.format("%s. %s", nr, artist.getName()), "https://open.spotify.com/artist/" + artist.getId());
                eb.setTimestamp(Instant.now());
                if (artist.getImages().length >= 1) {
                    eb.setImage(artist.getImages()[0].getUrl());
                }
                futurePages.add(new FutureTask<>(() -> eb));
            }
            new DynamicEmbedPageinator(futurePages, context.getUser(), eventWaiter, context.getJDA(), 240).create(msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
