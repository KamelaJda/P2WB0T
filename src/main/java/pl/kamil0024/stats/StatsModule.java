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

package pl.kamil0024.stats;

import lombok.Getter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.stats.commands.ChatmodStatsCommand;
import pl.kamil0024.stats.commands.TekstCommand;
import pl.kamil0024.stats.commands.TopCommand;
import pl.kamil0024.stats.entities.StatsCache;
import pl.kamil0024.stats.entities.Statystyka;
import pl.kamil0024.stats.listener.StatsListener;

import java.util.ArrayList;

public class StatsModule implements Modul {

    private ArrayList<Command> cmd;

    private final CommandManager commandManager;
    private final ShardManager api;
    private final EventWaiter eventWaiter;
    private final StatsDao statsDao;
    private final MusicModule musicModule;
    private final NieobecnosciDao nieobecnosciDao;

    private boolean start = false;

    @Getter public StatsCache statsCache;

    StatsListener statsListener;

    public StatsModule(CommandManager commandManager, ShardManager api, EventWaiter eventWaiter, StatsDao statsDao, MusicModule musicModule, NieobecnosciDao nieobecnosciDao) {
        this.commandManager = commandManager;
        this.api = api;
        this.eventWaiter = eventWaiter;
        this.statsDao = statsDao;
        this.statsCache = new StatsCache(statsDao);
        this.musicModule = musicModule;
        this.nieobecnosciDao = nieobecnosciDao;

        for (StatsConfig statsConfig : statsDao.getAll()) {
            Statystyka s = StatsConfig.getStatsFromDay(statsConfig.getStats(), new BDate().getDateTime().getDayOfYear());
            if (s == null) continue;
            statsCache.save(statsConfig.getId(), s);
        }

    }

    @Override
    public boolean startUp() {
        this.statsListener = new StatsListener(this);
        api.addEventListener(statsListener);
        cmd = new ArrayList<>();

        cmd.add(new ChatmodStatsCommand(statsDao));
        cmd.add(new TopCommand(statsDao, eventWaiter, nieobecnosciDao));
        cmd.add(new TekstCommand(eventWaiter, musicModule));

        cmd.forEach(commandManager::registerCommand);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        commandManager.unregisterCommands(cmd);
        api.removeEventListener(statsListener);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }

}
