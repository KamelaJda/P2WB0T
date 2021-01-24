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

package pl.kamil0024.commands.system;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.managers.AudioManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.audio.handlers.VoiceChannelHandler;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.music.commands.PlayCommand;

import java.util.Random;

public class RecordingCommand extends Command {

    @Getter @Setter
    public static VoiceChannelHandler handler;

    public RecordingCommand() {
        name = "recording";
        aliases.add("record");
        aliases.add("records");
        permLevel = PermLevel.ADMINISTRATOR;
    }

    @Override
    public boolean execute(CommandContext context) {
        String arg = context.getArgs().get(0);
        if (arg == null) throw new UsageException();

        if (arg.equalsIgnoreCase("start")) {
            if (!PlayCommand.isVoice(context.getMember())) {
                context.sendTranslate("recording.novc").queue();
                return false;
            }
            if (!PlayCommand.hasPermission(context.getGuild().getSelfMember(), PlayCommand.getVc(context.getMember()))) {
                context.sendTranslate("play.noperms").queue();
                return false;
            }

            if (getHandler() != null) {
                context.sendTranslate("recording.alreadyrec").queue();
                return false;
            }

            AudioManager manager = context.getGuild().getAudioManager();
            VoiceChannelHandler h = new VoiceChannelHandler(String.valueOf(new Random().nextInt(2137)));

            manager.setReceivingHandler(h);
            setHandler(h);
            try {
                manager.openAudioConnection(PlayCommand.getVc(context.getMember()));
                context.sendTranslate("recording.startrec", "⏭️");
            } catch (Exception e) {
                context.sendTranslate("recording.noconnect").queue();
                return false;
            }
            return true;
        }

        if (arg.equalsIgnoreCase("stop")) {
            if (getHandler() == null) {
                context.sendTranslate("recording.norec").queue();
                return false;
            } else {
                Message msg = context.sendTranslate("recording.saving",
                        context.getJDA().getEmoteById(Ustawienia.instance.emote.load).getAsMention()).complete();
                context.getGuild().getAudioManager().setReceivingHandler(null);
                getHandler().save();
                msg.editMessage(context.getTranslate("recording.success", getHandler().getId())).queue();
                setHandler(null);
                return true;
            }
        }

        if (arg.equalsIgnoreCase("lista")) {
            return true;
        }

        throw new UsageException();
    }

}
