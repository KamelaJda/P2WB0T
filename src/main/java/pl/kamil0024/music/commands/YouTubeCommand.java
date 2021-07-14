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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.music.MusicModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class YouTubeCommand extends Command {

    private final MusicModule musicModule;
    private final EventWaiter eventWaiter;

    public YouTubeCommand(MusicModule musicModule, EventWaiter eventWaiter) {
        name = "youtube";
        aliases.add("yt");
        permLevel = PermLevel.STAZYSTA;
        category = CommandCategory.MUSIC;
        enabledInRekru = true;

        this.musicModule = musicModule;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String tytul = context.getArgsToString(0);
        if (context.getArgs().get(0) == null) throw new UsageException();
        List<AudioTrack> audioTrackList = new ArrayList<>();

        try {
            audioTrackList = musicModule.search(tytul);
        } catch (Exception ignored) {
        }

        if (audioTrackList.isEmpty()) {
            context.sendTranslate("youtube.bad").queue();
            return false;
        }
        HashMap<Integer, AudioTrack> mapa = new HashMap<>();
        Message msg = context.send(build(mapa, audioTrackList)).complete();

        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
                (event) -> event.getAuthor().getId().equals(context.getUser().getId()) && event.getChannel().getId().equals(context.getChannel().getId()),
                (event) -> {
                    List<Integer> lista = new ArrayList<>();
                    String eMsg = event.getMessage().getContentRaw().replaceAll(" ", "");
                    for (String s : eMsg.split(",")) {
                        Integer i = context.getParsed().getNumber(s);
                        assert i != null && mapa.get(i) != null;
                        lista.add(i);
                    }
                    if (lista.isEmpty()) {
                        msg.delete().complete();
                        return;
                    }
                    msg.delete().complete();
                    lista.forEach(i -> musicModule.loadAndPlay(context.getChannel(), QueueCommand.getYtLink(mapa.get(i)), PlayCommand.getVc(context.getMember()), false));
                    context.sendTranslate("youtube.succes", lista.size()).queue();
                }, 30, TimeUnit.SECONDS, () -> msg.delete().queue());

        return true;
    }

    public static String build(HashMap<Integer, AudioTrack> mapa, List<AudioTrack> audioTrackList) {
        BetterStringBuilder bsb = new BetterStringBuilder();
        bsb.appendLine("```");
        bsb.appendLine(Tlumaczenia.get("youtube.firstline"));
        int tracks = 0;
        for (AudioTrack audioTrack : audioTrackList) {
            tracks++;
            AudioTrackInfo info = audioTrack.getInfo();
            bsb.appendLine(tracks + ". " + info.title + " :: " + info.author);
            mapa.put(tracks, audioTrack);
            if (tracks == 10) break;
        }
        bsb.appendLine("```");
        return bsb.build();
    }

}
