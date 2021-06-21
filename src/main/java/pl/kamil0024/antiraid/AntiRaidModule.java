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

package pl.kamil0024.antiraid;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.antiraid.listeners.AntiRaidListener;
import pl.kamil0024.antiraid.managers.AntiRaidManager;
import pl.kamil0024.core.database.AntiRaidDao;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.moderation.listeners.ModLog;

public class AntiRaidModule implements Modul {

    private final ShardManager api;
    private final AntiRaidDao dao;
    private final RedisManager redisManager;
    private final CaseDao caseDao;
    private final ModLog modLog;

    @Getter
    private final String name = "antiraid";

    @Getter
    @Setter
    private boolean start = false;

    private AntiRaidListener antiRaidListener;

    public AntiRaidModule(ShardManager api, AntiRaidDao dao, RedisManager redisManager, CaseDao caseDao, ModLog modLog) {
        this.api = api;
        this.dao = dao;
        this.redisManager = redisManager;
        this.caseDao = caseDao;
        this.modLog = modLog;
    }

    @Override
    public boolean startUp() {
        AntiRaidManager manager = new AntiRaidManager(dao, redisManager);
        antiRaidListener = new AntiRaidListener(manager, dao, caseDao, modLog);
        api.addEventListener(antiRaidListener);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(antiRaidListener);
        return true;
    }

}
