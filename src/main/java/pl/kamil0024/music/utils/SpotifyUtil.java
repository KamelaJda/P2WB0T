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
import com.wrapper.spotify.model_objects.specification.Track;
import lombok.AllArgsConstructor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class SpotifyUtil {

    private static final Pattern TRACK_REGEX = Pattern.compile("^(https://open.spotify.com/track/)([a-zA-Z0-9]+)(.*)$");

    private final SpotifyApi spotifyApi;

    public Track getTrack(String link) {
        Matcher xd = TRACK_REGEX.matcher(link);
        if (xd.matches()) {
            try {
                return spotifyApi.getTrack(xd.group(2)).build().execute();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
        return null;

    }

}
