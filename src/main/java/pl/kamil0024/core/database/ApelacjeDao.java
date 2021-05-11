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

package pl.kamil0024.core.database;

import gg.amy.pgorm.PgMapper;
import lombok.Getter;
import org.joda.time.DateTime;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.config.ApelacjeConfig;
import pl.kamil0024.core.database.config.Dao;
import pl.kamil0024.core.logger.Log;

import java.util.*;
import java.util.stream.Collectors;

public class ApelacjeDao implements Dao<ApelacjeConfig> {

    @Getter
    private final PgMapper<ApelacjeConfig> mapper;

    public ApelacjeDao(DatabaseManager databaseManager) {
        if (databaseManager == null) throw new IllegalStateException("databaseManager == null");
        mapper = databaseManager.getPgStore().mapSync(ApelacjeConfig.class);
    }

    @Override
    public ApelacjeConfig get(String id) {
        return mapper.load(id).orElse(null);
    }

    public List<ApelacjeConfig> getAll(int offset) {
        return mapper.getAllApelacje(offset);
    }

    public List<ApelacjeConfig> getAllByNick(String nick, int offset) {
        return mapper.getAllApelacjeByNick(nick, offset);
    }

    public static HashMap<String, List<ApelacjeConfig>> getFromMonth(List<ApelacjeConfig> apelacje, int month, int year) {
        HashMap<String, List<ApelacjeConfig>> map = new HashMap<>();

        List<ApelacjeConfig> filtr = apelacje.stream().filter(a -> {
            DateTime dt = new DateTime(a.getCreatedApelacja());
            return dt.getMonthOfYear() == month && dt.getYear() == year;
        }).collect(Collectors.toList());

        for (ApelacjeConfig a : filtr) {
            List<ApelacjeConfig> fmap = map.getOrDefault(a.getApelacjeNick(), new ArrayList<>());
            fmap.add(a);
            map.put(a.getApelacjeNick(), fmap);
        }
        return map;
    }

    public static HashMap<String, List<ApelacjeConfig>> getFrom(List<ApelacjeConfig> all, DateTime dt, int days) {
        Calendar cal = Calendar.getInstance();
        HashMap<String, List<ApelacjeConfig>> map = new HashMap<>();

        long start = dt.minusDays(days).getMillis();
        long end = dt.getMillis();

        List<ApelacjeConfig> filtr = all.stream()
                .filter(ape -> d(ape.getCreatedApelacja(), cal) <= end && d(ape.getCreatedApelacja(), cal) >= start)
                .collect(Collectors.toList());

        for (ApelacjeConfig a : filtr) {
            List<Integer> dzien = Ustawienia.instance.apelacje.dni.getOrDefault(a.getApelacjeNick(), new ArrayList<>());
            if (dzien != null && !dzien.isEmpty()) {
                List<ApelacjeConfig> fmap = map.getOrDefault(a.getApelacjeNick(), new ArrayList<>());
                if (!dzien.contains(new DateTime(a.getCreatedApelacja()).getDayOfWeek())) {
                    fmap.add(a);
                    map.put(a.getApelacjeNick(), fmap);
                }
            } else {
                Log.newError("Nick " + a.getApelacjeNick() + " ma apelacje o id " + a.getId() + ", ale nie ma go w configu!", ApelacjeDao.class);
            }
        }
        return map;
    }

    private static long d(long ms, Calendar i) {
        i.setTime(new Date(ms));
        i.set(Calendar.HOUR_OF_DAY, 0);
        i.set(Calendar.MINUTE, 0);
        i.set(Calendar.SECOND, 0);
        i.set(Calendar.MILLISECOND, 0);
        return i.toInstant().toEpochMilli();
    }

}
