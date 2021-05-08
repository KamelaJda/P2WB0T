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

package pl.kamil0024.api.redisstats.config;

import lombok.Data;

import java.util.List;

@Data
public class ChatModStatsConfig {
    public ChatModStatsConfig() {
    }

    private String id;
    private String nick;
    private int liczbaKar = 0;

    public static boolean consId(String id, List<ChatModStatsConfig> c) {
        for (ChatModStatsConfig e : c) {
            if (e.getId().equals(id)) {
                return false;
            }
        }
        return true;
    }

}
