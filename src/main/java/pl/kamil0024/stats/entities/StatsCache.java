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

package pl.kamil0024.stats.entities;

import lombok.Data;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
public class StatsCache {

    private List<UserStats> statystykaMap;

    private StatsDao statsDao;

    public StatsCache(StatsDao statsDao) {
        this.statystykaMap = new ArrayList<>();
        this.statsDao = statsDao;

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::tak, 0, 60, TimeUnit.MINUTES);
    }

    public void save(String id, Statystyka stats) {
        UserStats userStats = new UserStats(id);
        userStats.getStatsMap().put(new BDate().getDateTime().getDayOfYear(), stats);
        getStatystykaMap().add(userStats);
    }

    public void tak() {
        databaseSave();
    }

    public synchronized void databaseSave() {
        for (UserStats entry : getStatystykaMap()) {
            Statystyka stat = entry.getFromNow();
            StatsConfig config = statsDao.get(entry.getId());
            config.getStats().removeIf(configStat -> configStat.getDay() == stat.getDay());
            config.getStats().add(stat);
            statsDao.save(config);
        }
    }

    public synchronized void addZmutowanych(String id, int liczba) {
        saveCache(id, liczba, UserStats.StatsType.MUTE);
    }

    public synchronized void addZbanowanych(String id, int liczba) {
        saveCache(id, liczba, UserStats.StatsType.BAN);
    }

    public synchronized void addUsunietychWiadomosci(String id, int liczba) {
        saveCache(id, liczba, UserStats.StatsType.DELETEDMESSAGE);
    }

    public synchronized void addNapisanychWiadomosci(String id, int liczba) {
        saveCache(id, liczba, UserStats.StatsType.SENDMESSAGE);
    }

    public synchronized void addWyrzuconych(String id, int liczba) {
        saveCache(id, liczba, UserStats.StatsType.KICK);
    }

    private void saveCache(String id, int liczba, UserStats.StatsType statsType) {
        for (UserStats userStats : getStatystykaMap()) {
            if (!userStats.getId().equals(id)) continue;
            userStats.add(statsType, liczba);
            return;
        }
        int day = new BDate().getDateTime().getDayOfYear();
        UserStats stat = new UserStats(id);
        Statystyka statystyka = new Statystyka();
        statystyka.setDay(day);
        stat.getStatsMap().put(day, statystyka);
        getStatystykaMap().add(stat);
        saveCache(id, liczba, statsType);
    }
}
