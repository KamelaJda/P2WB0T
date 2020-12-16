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

package pl.kamil0024.embedgenerator;

import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.embedgenerator.commands.EmbedCommand;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

import java.util.ArrayList;

public class EmbedGeneratorModule implements Modul {

    private final CommandManager commandManager;
    private final EmbedRedisManager embedRedisManager;

    private ArrayList<Command> cmd;
    private boolean start = false;

    public EmbedGeneratorModule(CommandManager commandManager, EmbedRedisManager embedRedisManager) {
        this.commandManager = commandManager;
        this.embedRedisManager = embedRedisManager;
    }

    @Override
    public boolean startUp() {
        cmd = new ArrayList<>();
        cmd.add(new EmbedCommand(embedRedisManager));
        cmd.forEach(commandManager::registerCommand);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        commandManager.unregisterCommands(cmd);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "embed";
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
