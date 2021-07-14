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

package pl.kamil0024.music.commands;

import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.musicmanager.entity.GuildMusicManager;

public class LeaveCommand extends Command {

    private final MusicModule musicModule;

    public LeaveCommand(MusicModule musicModule) {
        name = "leave";
        aliases.add("opusc");
        category = CommandCategory.MUSIC;
        permLevel = PermLevel.STAZYSTA;
        this.musicModule = musicModule;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildMusicManager musicManager = SkipCommand.getMusicManager(context, musicModule);
        if (musicManager == null) return false;
        musicManager.getScheduler().destroy();
        context.sendTranslate("leave.succes", "\uD83D\uDC4B").queue();
        return true;
    }

}
