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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.managers.AudioManager;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.ModLog;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.audio.handlers.VoiceChannelHandler;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.RecordingDao;
import pl.kamil0024.core.database.config.RecordingConfig;
import pl.kamil0024.core.util.DynamicEmbedPageinator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.music.commands.PlayCommand;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.FutureTask;

public class RecordingCommand extends Command {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM HH:mm:ss");

    @Getter @Setter
    public static VoiceChannelHandler handler;

    public final RecordingDao recordingDao;
    public final EventWaiter eventWaiter;

    public RecordingCommand(RecordingDao recordingDao, EventWaiter eventWaiter) {
        name = "recording";
        aliases.add("record");
        aliases.add("records");
        permLevel = PermLevel.ADMINISTRATOR;

        this.recordingDao = recordingDao;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(CommandContext context) {
        String arg = context.getArgs().get(0);
        if (arg == null) throw new UsageException();
        AudioManager manager = context.getGuild().getAudioManager();

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

            VoiceChannelHandler h = new VoiceChannelHandler(String.valueOf(new Random().nextInt(Integer.MAX_VALUE)), context.getUser().getId());

            manager.setReceivingHandler(h);
            setHandler(h);
            try {
                manager.openAudioConnection(PlayCommand.getVc(context.getMember()));
                context.sendTranslate("recording.startrec").queue();
            } catch (Exception e) {
                e.printStackTrace();
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

                RecordingConfig rc = new RecordingConfig(getHandler().getId());
                rc.setEndTime(new Date().getTime());
                rc.setStartTime(getHandler().getDate());
                rc.setUser(getHandler().getUserId());
                recordingDao.save(rc);

                context.getGuild().getAudioManager().setReceivingHandler(null);
                getHandler().save();
                msg.editMessage(context.getTranslate("recording.success", getHandler().getId())).queue();
                setHandler(null);
                manager.closeAudioConnection();
                return true;
            }
        }

        if (arg.equalsIgnoreCase("lista")) {
            List<RecordingConfig> rc = recordingDao.getByUser(context.getUser().getId());
            if (rc.isEmpty()) {
                context.sendTranslate("recording.emptylist").queue();
                return false;
            }
            Collections.reverse(rc);
            List<FutureTask<EmbedBuilder>> futurePages = new ArrayList<>();

            for (RecordingConfig entry : rc) {
                futurePages.add(new FutureTask<>(() -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setColor(UserUtil.getColor(context.getMember()));
                    eb.addField("Link do nagrania", "https://discord.p2w.pl/recordings?id=" + entry.getId(), false);
                    eb.addField("Początek nagrania", SDF.format(entry.getStartTime()), false);
                    eb.addField("Koniec nagrania", SDF.format(entry.getEndTime()), false);
                    eb.addField("Czas trwania nagrania", new BDate(entry.getStartTime(), ModLog.getLang()).difference(entry.getEndTime()), false);
                    eb.setTimestamp(Instant.now());
                    return eb;
                }));
            }
            new DynamicEmbedPageinator(futurePages, context.getUser(), eventWaiter, context.getJDA(), 240).create(context.getChannel(), context.getMessage());
            return true;
        }

        throw new UsageException();
    }

}
