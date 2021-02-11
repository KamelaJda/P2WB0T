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

package pl.kamil0024.userstats;

import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.userstats.listener.StatsListener;
import pl.kamil0024.userstats.manager.StatsManager;

import java.util.ArrayList;

public class UserStatsModule implements Modul {

    private ArrayList<Command> cmd;
    private final CommandManager commandManager;
    private final StatsManager statsManager;
    private final ShardManager api;

    private boolean start = false;

    private StatsListener statsListener;

    public UserStatsModule(ShardManager api, CommandManager commandManager, StatsManager statsManager) {
        this.commandManager = commandManager;
        this.statsManager = statsManager;
        this.api = api;
    }

    @Override
    public boolean startUp() {
        statsListener = new StatsListener(statsManager);
        api.addEventListener(statsListener);

        cmd = new ArrayList<>();
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
        return "userstats";
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
