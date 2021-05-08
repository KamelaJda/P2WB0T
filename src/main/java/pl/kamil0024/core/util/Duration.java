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

package pl.kamil0024.core.util;

import lombok.Getter;
import lombok.Setter;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.bdate.util.BLanguage;
import pl.kamil0024.bdate.util.Nullable;
import pl.kamil0024.bdate.util.Time;

public class Duration {

    @Getter
    @Setter
    private Long now;
    @Getter
    @Setter
    private Long date;

    public Duration() {
        now = new BDate().getTimestamp();
    }

    @Nullable
    public Long parseLong(String s) {
        if (s == null) return null;
        String split = null;
        Time time = null;

        if (s.endsWith("d")) {
            split = "d";
            time = Time.DAY;
        } else if (s.endsWith("s")) {
            split = "s";
            time = Time.SECOND;
        } else if (s.endsWith("m")) {
            split = "m";
            time = Time.MINUTE;
        } else if (s.endsWith("h")) {
            split = "h";
            time = Time.HOUR;
        }
        if (split == null) return null;

        String[] czas = s.split(split);
        long longg;

        try {
            long l = Long.parseLong(czas[0]);
            if (l <= 0) return null;
            longg = l;
        } catch (NumberFormatException e) {
            return null;
        }

        return new BDate(now, getLang()).add(longg, time).getTimestamp();
    }

    private static BLanguage getLang() {
        BLanguage lang = new BLanguage();
        lang.setSecond("sek.");
        lang.setMinute("min.");
        lang.setHour("godz.");
        lang.setDay("d.");
        return lang;
    }

}