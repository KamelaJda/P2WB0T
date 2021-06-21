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

package pl.kamil0024.api.handlers;

import com.wrapper.spotify.model_objects.specification.Artist;
import com.wrapper.spotify.model_objects.specification.Track;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.kamil0024.api.Response;
import pl.kamil0024.commands.utils.SpotifyWaiter;
import pl.kamil0024.music.utils.SpotifyUtil;
import pl.kamil0024.music.utils.UserCredentials;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SpotifyStatsHandler implements HttpHandler {

    private final SpotifyUtil spotifyUtil;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) return;

        try {
            String id = ex.getQueryParameters().get("member").getFirst();
            SpotifyWaiter.Choose time = SpotifyWaiter.Choose.valueOf(ex.getQueryParameters().get("time").getFirst());
            SpotifyWaiter.Choose type = SpotifyWaiter.Choose.valueOf(ex.getQueryParameters().get("type").getFirst());

            UserCredentials user = spotifyUtil.getUser(id);
            if (user == null) {
                Response.sendErrorResponse(ex, "Błąd", "Nie ma usera w bazie danych!");
                return;
            }


            if (type == SpotifyWaiter.Choose.ARTISTS) {
                List<ArtistItem> list = new ArrayList<>();
                Artist[] items = user.getApi().getUsersTopArtists().time_range(time.s).limit(10).build().execute().getItems();
                for (Artist item : items) {
                    list.add(new ArtistItem(item.getName(), item.getId(), item.getImages().length >= 1 ? item.getImages()[0].getUrl() : null));
                }
                Response.sendObjectResponse(ex, list);
            } else if (type == SpotifyWaiter.Choose.TRACK) {
                List<TrackItem> list = new ArrayList<>();
                Track[] items = user.getApi().getUsersTopTracks().time_range(time.s).limit(10).build().execute().getItems();
                for (Track item : items) {
                    list.add(new TrackItem(item.getName(), item.getId(),
                            item.getAlbum().getImages().length >= 1 ? item.getAlbum().getImages()[1].getUrl() : null,
                            item.getArtists()[0].getName()));
                }
                Response.sendObjectResponse(ex, list);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd", "Nie udało się wysłać requesta: " + e.getLocalizedMessage());
        }
    }

    @AllArgsConstructor
    @Data
    private static class ArtistItem {
        private final String name;
        private final String id;
        private final String imageUrl;
    }

    @AllArgsConstructor
    @Data
    private static class TrackItem {
        private final String name;
        private final String id;
        private final String imageUrl;
        private final String artist;
    }

}
