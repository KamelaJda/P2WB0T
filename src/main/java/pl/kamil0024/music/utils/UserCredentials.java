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

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import lombok.Getter;
import pl.kamil0024.core.database.SpotifyDao;
import pl.kamil0024.core.database.config.SpotifyConfig;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class UserCredentials {

    private final ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);

    private final String discordId;

    private final SpotifyApi api;
    private final SpotifyDao dao;

    public UserCredentials(String discordId, String accessToken, String refreshToken, SpotifyDao dao) {
        this.discordId = discordId;
        this.api = SpotifyApi.builder()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken).build();
        this.dao = dao;
        refreshAccessToken();
    }

    public void refreshAccessToken() {
        try {
            ClientCredentials cr = api.clientCredentials().build().execute();
            api.setAccessToken(cr.getAccessToken());
            ses.schedule(this::refreshAccessToken, cr.getExpiresIn() - 120, TimeUnit.SECONDS);

            SpotifyConfig config = dao.get(discordId);
            if (config != null) { // trudno, nie będziemy zapisywać
                config.setAccessToken(cr.getAccessToken());
                dao.save(config);
            }

        } catch (Exception e) {
            ses.schedule(this::refreshAccessToken, 60, TimeUnit.SECONDS);
        }
    }

}
