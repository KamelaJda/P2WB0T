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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Table("ankieta")
@GIndex({"id"})
@Data
@AllArgsConstructor
public class AnkietaConfig {
    public AnkietaConfig() {
    }

    public AnkietaConfig(String id) {
        this.id = id;
    }

    @PrimaryKey
    private String id;
    private String autorId;
    private String description;
    private String messageId;

    private List<Opcja> opcje = new ArrayList<>();

    // id - ilość głosów
    private Map<Integer, Integer> glosy = new HashMap<>();

    private long createdAt;
    private long sendAt;
    private long endAt;

    private boolean multiOptions = false;
    private boolean aktywna = true;

    @Data
    @AllArgsConstructor
    public static class Opcja {
        private int id;
        private String text;
        private String emoji;
    }

}
