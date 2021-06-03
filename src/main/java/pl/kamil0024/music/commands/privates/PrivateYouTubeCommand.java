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

package pl.kamil0024.music.commands.privates;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.socket.SocketClient;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.music.commands.PlayCommand;
import pl.kamil0024.music.commands.QueueCommand;

import java.util.*;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("DuplicatedCode")
public class PrivateYouTubeCommand extends Command {

    private final SocketManager socketManager;
    private final EventWaiter eventWaiter;
    private final MusicModule musicModule;

    public PrivateYouTubeCommand(SocketManager socketManager, EventWaiter eventWaiter, MusicModule musicModule) {
        name = "pyt";
        aliases.add("privateeyt");
        aliases.add("privateeyoutube");
        category = CommandCategory.PRIVATE_CHANNEL;
        this.socketManager = socketManager;
        this.eventWaiter = eventWaiter;
        this.musicModule = musicModule;
        commandData = getData()
                .addOption(OptionType.STRING, "title", "Tytuł piosenki", true);
    }

    @Override
    public boolean execute(SlashContext context) {
        if (!PrivatePlayCommand.check(context)) return false;

        String tytul = Objects.requireNonNull(context.getEvent().getOption("title")).getAsString();

        if (tytul.startsWith("https://")) {
            context.sendTranslate("youtube.anothercmd", context.getPrefix());
            return false;
        }

        List<AudioTrack> audioTrackList = new ArrayList<>();

        try {
            audioTrackList = musicModule.search(tytul);
        } catch (Exception ignored) { }

        if (audioTrackList.isEmpty()) {
            context.sendTranslate("youtube.bad");
            return false;
        }

        HashMap<Integer, AudioTrack> mapa = new HashMap<>();

        BetterStringBuilder bsb = new BetterStringBuilder();
        bsb.appendLine("```");
        bsb.appendLine(context.getTranslate("youtube.firstline"));
        int tracks = 0;
        for (AudioTrack audioTrack : audioTrackList) {
            tracks++;
            AudioTrackInfo info = audioTrack.getInfo();
            bsb.appendLine(tracks + ". " + info.title + " :: " + info.author);
            mapa.put(tracks, audioTrack);
            if (tracks == 10) break;
        }
        bsb.appendLine("```");

        try {
            Message msg = context.send(bsb.toString(), false);
            eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
                    (event) -> event.getAuthor().getId().equals(context.getUser().getId()) && event.getChannel().getId().equals(context.getChannel().getId()),
                    (event) -> {
                        List<Integer> lista = new ArrayList<>();
                        String eMsg = event.getMessage().getContentRaw().replaceAll(" ", "");
                        for (String s : eMsg.split(",")) {
                            Integer i = context.getParsed().getNumber(s);
                            if (i != null && mapa.get(i) != null) {
                                lista.add(i);
                            }
                        }
                        try {
                            msg.delete().complete();
                            if (lista.isEmpty()) return;

                            List<String> urls = new ArrayList<>();
                            lista.forEach(i -> urls.add(QueueCommand.getYtLink(mapa.get(i))));

                            SocketClient client = socketManager.getClientFromChannel(context.getMember());

                            if (client != null) {
                                socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), client.getSocketId())
                                        .play(urls);
                            } else {
                                boolean find = false;
                                for (Map.Entry<Integer, SocketClient> entry : socketManager.getClients().entrySet()) {
                                    Member mem = context.getGuild().getMemberById(entry.getValue().getBotId());
                                    if (mem == null) continue;
                                    if (mem.getVoiceState() == null || mem.getVoiceState().getChannel() == null) {
                                        find = true;
                                        socketManager.getAction(context.getMember().getId(), context.getChannel().getId(), entry.getKey())
                                                .setSendMessage(false)
                                                .connect(PlayCommand.getVc(context.getMember()).getId())
                                                .setSendMessage(true)
                                                .play(urls);
                                        break;
                                    }
                                }
                                if (!find) context.sendTranslate("pplay.to.small.bot");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            context.send("Wystąpił błąd: " + e.getLocalizedMessage());
                        }
                        event.getMessage().delete().queue();
                    }, 15, TimeUnit.SECONDS, () -> msg.delete().queue());
        } catch (Exception e) {
            context.send("Wystąpił błąd z API! " + e.getLocalizedMessage());
            Log.newError(e, PrivateYouTubeCommand.class);
        }

        return true;
    }

}
