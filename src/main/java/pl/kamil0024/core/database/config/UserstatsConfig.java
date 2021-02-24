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

package pl.kamil0024.core.database.config;

import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Table("userstats")
@GIndex({"date", "members"})
@Data
@AllArgsConstructor
public class UserstatsConfig {
    public UserstatsConfig() { }

    public UserstatsConfig(String date) {
        this.date = date;
    }

    public UserstatsConfig(long date) {
        this.date = String.valueOf(date);
    }

    @PrimaryKey
    private String date;

    private Map<String, Config> members = new HashMap<>();

    @Data
    @AllArgsConstructor
    public static class Config {
        private long messageCount;
        private Map<String, Long> channels;
    }

}
