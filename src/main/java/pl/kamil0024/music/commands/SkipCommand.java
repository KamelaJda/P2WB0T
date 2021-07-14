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
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.musicmanager.MusicManager;
import pl.kamil0024.musicmanager.entity.GuildMusicManager;

public class SkipCommand extends Command {

    private final MusicModule musicModule;

    public SkipCommand(MusicModule musicModule) {
        name = "skip";
        permLevel = PermLevel.STAZYSTA;
        category = CommandCategory.MUSIC;
        enabledInRekru = true;

        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        GuildMusicManager musicManager = getMusicManager(context, musicModule);
        if (musicManager == null) return false;

        if (musicManager.getScheduler().getLoop()) {
            context.sendTranslate("skip.looped").queue();
            return false;
        }

        if (musicManager.getPlayer().getPlayingTrack() == null) {
            context.sendTranslate("resume.noplay").queue();
            return false;
        }
        context.sendTranslate("skip.next").queue();
        musicManager.getScheduler().nextTrack();
        return true;
    }

    @Nullable
    public static GuildMusicManager getMusicManager(CommandContext context, MusicModule musicModule) {
        if (!PlayCommand.isVoice(context.getGuild().getSelfMember())) {
            context.sendTranslate("leave.nochannel").queue();
            return null;
        }

        if (!PlayCommand.isSameChannel(context.getGuild().getSelfMember(), context.getMember())) {
            context.sendTranslate("leave.samechannel").queue();
            return null;
        }
        return musicModule.getGuildAudioPlayer(context.getGuild());
    }

}
