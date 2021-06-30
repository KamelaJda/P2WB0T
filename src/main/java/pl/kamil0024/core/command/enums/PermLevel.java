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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PermLevel {

    MEMBER(0, "permlvl.member", null),
    STAZYSTA(1, "permlvl.staz", Ustawienia.instance.rangi.stazysta),
    CHATMOD(2, "permlvl.chatmod", Ustawienia.instance.roles.chatMod),
    HELPER(3, "permlvl.helper", Ustawienia.instance.rangi.pomocnik),
    MODERATOR(4, "permlvl.mod", Ustawienia.instance.rangi.moderator),
    ADMINISTRATOR(5, "permlvl.adm", Ustawienia.instance.rangi.administrator),
    DEVELOPER(10, "permlvl.dev", null);

    private final int numer;
    private final String tranlsateKey;
    private final String roleId;

    public static PermLevel getPermLevel(int numer) {
        if (numer == 0) return MEMBER;
        if (numer == 1) return HELPER;
        if (numer == 2) return MODERATOR;
        if (numer == 3) return ADMINISTRATOR;
        if (numer == 10) return DEVELOPER;
        throw new IllegalArgumentException("Nieprawidłowy poziom!");
    }

    @Nullable
    public static PermLevel getPermLevel(String roleId) {
        return Arrays.stream(PermLevel.values()).filter(f -> f.getRoleId().equals(roleId)).findAny().orElse(null);
    }

}
