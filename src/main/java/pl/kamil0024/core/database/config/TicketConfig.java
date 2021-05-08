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

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Table("ticket")
@GIndex({"id"})
@Data
@AllArgsConstructor
public class TicketConfig {
    public TicketConfig() {
    }

    public TicketConfig(String id) {
        this.id = id;
    }

    @PrimaryKey
    private String id = "";

    private String admId; // id administratora
    private String admNick;
    private String userId = null; // id użytkownika
    private String userNick = null; // nick użytkownika
    private int ocena = -1; // ocena 1-5
    private String temat;
    private boolean problemRozwiazany = false;
    private String uwaga;
    private long createdTime; // kiedy stworzono ticketa
    private long completeTime; // kiedy wypełniono ankiete
    private long timestamp; // w ile rozwiązano ticketa
    private List<String> readBy = new ArrayList<>();
    private String kategoria; // kategoria zlogszenia mc/discord/forum

    private boolean spam = false;
    private String spamAdm = null;

    public static boolean isEdited(TicketConfig tc) {
        return tc.getOcena() != 1;
    }

    @JsonIgnore
    public static boolean exist(TicketConfig tc) {
        return tc.getAdmId() != null && tc.getUserId() != null;
    }

    @JsonIgnore
    public static String getUrl(TicketConfig tc) {
        return "https://discord.p2w.pl/ticket/" + tc.id;
    }

}
