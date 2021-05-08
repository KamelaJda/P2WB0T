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

package pl.kamil0024.core.command.enums;

import lombok.Getter;

@SuppressWarnings("unused")
public enum PermLevel {

    MEMBER(0, "permlvl.member"),
    STAZYSTA(1, "permlvl.staz"),
    CHATMOD(2, "permlvl.chatmod"),
    HELPER(3, "permlvl.helper"),
    MODERATOR(4, "permlvl.mod"),
    ADMINISTRATOR(5, "permlvl.adm"),
    DEVELOPER(10, "permlvl.dev");

    @Getter
    private final int numer;
    @Getter
    private final String tranlsateKey;

    PermLevel(int numer, String tranlsateKey) {
        this.numer = numer;
        this.tranlsateKey = tranlsateKey;
    }

    public static PermLevel getPermLevel(int numer) {
        if (numer == 0) return MEMBER;
        if (numer == 1) return HELPER;
        if (numer == 2) return MODERATOR;
        if (numer == 3) return ADMINISTRATOR;
        if (numer == 10) return DEVELOPER;
        throw new IllegalArgumentException("Nieprawidłowy poziom!");
    }

}
