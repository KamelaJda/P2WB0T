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

package pl.kamil0024.chat;


import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.chat.listener.ChatListener;
import pl.kamil0024.chat.listener.KaryListener;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;

public class ChatModule implements Modul {

    private final ShardManager api;
    private final KaryJSON karyJSON;
    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;
    private final RedisManager redisManager;

    @Getter
    @Setter
    private boolean start = false;
    private ChatListener chatListener;
    private KaryListener karyListener;

    public ChatModule(ShardManager api, KaryJSON karyJSON, CaseDao caseDao, ModLog modLog, StatsModule statsModule, RedisManager redisManager) {
        this.api = api;
        this.karyJSON = karyJSON;
        this.modLog = modLog;
        this.caseDao = caseDao;
        this.statsModule = statsModule;
        this.redisManager = redisManager;
    }

    @Override
    public boolean startUp() {
        this.karyListener = new KaryListener(karyJSON, caseDao, modLog, statsModule, redisManager);
        this.chatListener = new ChatListener(karyJSON, caseDao, modLog, statsModule, this.karyListener);
        api.addEventListener(chatListener, karyListener);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        api.removeEventListener(chatListener, karyListener);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "chat";
    }

}
