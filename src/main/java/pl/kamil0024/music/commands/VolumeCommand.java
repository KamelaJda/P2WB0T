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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.music.MusicModule;

public class VolumeCommand extends Command {

    private final MusicModule musicModule;

    public VolumeCommand(MusicModule musicModule) {
        name = "volume";
        permLevel = PermLevel.STAZYSTA;
        category = CommandCategory.MUSIC;
        enabledInRekru = true;

        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (!PlayCommand.isVoice(context.getGuild().getSelfMember())) {
            context.sendTranslate("leave.nochannel").queue();
            return false;
        }

        if (!PlayCommand.isSameChannel(context.getGuild().getSelfMember(), context.getMember())) {
            context.sendTranslate("leave.samechannel").queue();
            return false;
        }
        AudioPlayer audio = musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer();
        Integer arg = context.getParsed().getNumber(context.getArgs().get(0));
        if (arg == null) {
            context.sendTranslate("volume.volume", audio.getVolume()).queue();
            return true;
        }

        if (arg < 1 || arg > 100) {
            context.sendTranslate("volume.badnumber").queue();
            return false;
        }

        audio.setVolume(arg);
        context.sendTranslate("volume.succes", arg).queue();
        return true;
    }

}
