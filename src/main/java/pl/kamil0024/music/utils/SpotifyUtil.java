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

package pl.kamil0024.music.utils;

import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.Album;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.data.AbstractDataRequest;
import lombok.Getter;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.database.SpotifyDao;
import pl.kamil0024.core.database.config.SpotifyConfig;
import pl.kamil0024.core.logger.Log;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class SpotifyUtil {

    private static final Pattern TRACK_REGEX = Pattern.compile("^(https://open.spotify.com/track/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern PLAYLIST_REGEX = Pattern.compile("^(https://open.spotify.com/playlist/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern ALBUM_REGEX = Pattern.compile("^(https://open.spotify.com/album/)([a-zA-Z0-9]+)(.*)$");
    private static final Pattern ARTISTS_REGEX = Pattern.compile("^(https://open.spotify.com/artist/)([a-zA-Z0-9]+)(.*)$");

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    private final SpotifyApi api;
    private final SpotifyDao dao;
    private final Map<String, UserCredentials> userCredentials = new HashMap<>();

    public SpotifyUtil(SpotifyApi api, SpotifyDao dao) {
        this.api = api;
        this.dao = dao;
        refreshAccessToken();
    }

    public boolean isTrack(String url) {
        return TRACK_REGEX.matcher(url).matches();
    }

    public boolean isPlaylist(String url) {
        return PLAYLIST_REGEX.matcher(url).matches();
    }

    public boolean isAlbum(String url) {
        return ALBUM_REGEX.matcher(url).matches();
    }

    public boolean isArtists(String url) {
        return ARTISTS_REGEX.matcher(url).matches();
    }

    public Track getTrackFromUrl(String url) throws ParseException, IOException {
        Matcher reg = TRACK_REGEX.matcher(url);
        if (reg.find()) return e(getApi().getTrack(reg.group(2)));
        else return null;
    }

    public Paging<PlaylistTrack> getPlaylistFromUrl(String url) throws ParseException, IOException {
        Matcher reg = PLAYLIST_REGEX.matcher(url);
        if (reg.find()) return e(getApi().getPlaylistsItems(reg.group(2)));
        else return null;
    }

    public Album getAlbumFromUrl(String url) throws IOException, ParseException {
        Matcher m = ALBUM_REGEX.matcher(url);
        if (m.find()) return e(getApi().getAlbum(m.group(2)));
        else return null;
    }

    public Track[] getArtistsTracks(String url) throws IOException, ParseException {
        Matcher m = ARTISTS_REGEX.matcher(url);
        if (m.find()) return e(getApi().getArtistsTopTracks(m.group(2), CountryCode.PL));
        else return null;
    }

    private <T, V extends AbstractDataRequest.Builder<T, ?>> T e(AbstractDataRequest.Builder<T, V> e) throws IOException, ParseException {
        try {
            return e.build().execute();
        } catch (SpotifyWebApiException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Nullable
    public UserCredentials getUser(String user) {
        if (getUserCredentials().containsKey(user)) return getUserCredentials().get(user);
        SpotifyConfig conf = dao.get(user);
        if (conf == null) return null;
        UserCredentials cr = new UserCredentials(user, conf.getAccessToken(), conf.getRefreshToken(), dao);
        getUserCredentials().put(user, cr);
        return cr;
    }

    public void addUser(String user, String code) {
        try {
            AuthorizationCodeCredentials c = getApi().authorizationCode(
                    getApi().getClientId(),
                    getApi().getClientSecret(),
                    code,
                    new URI("https://discord.p2w.pl/api/spotify/callback")
            ).build().execute();
            UserCredentials userCredentials = new UserCredentials(user, c.getAccessToken(), c.getRefreshToken(), dao);
            getUserCredentials().put(user, userCredentials);

            SpotifyConfig config = new SpotifyConfig(user);
            config.setAccessToken(c.getAccessToken());
            config.setRefreshToken(c.getRefreshToken());
            dao.save(config);
        } catch (Exception e) {
            Log.newError(e, getClass());
        }
    }

    public void refreshAccessToken() {
        try {
            ClientCredentials cr = api.clientCredentials().build().execute();
            api.setAccessToken(cr.getAccessToken());
            ses.schedule(this::refreshAccessToken, cr.getExpiresIn() - 120, TimeUnit.SECONDS);
        } catch (Exception e) {
            ses.schedule(this::refreshAccessToken, 60, TimeUnit.SECONDS);
        }
    }

}