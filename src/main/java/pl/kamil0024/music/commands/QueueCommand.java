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
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.EmbedPageintaor;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.musicmanager.entity.TrackScheduler;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class QueueCommand extends Command {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MusicModule musicModule;
    private final EventWaiter eventWaiter;

    public QueueCommand(MusicModule musicModule, EventWaiter eventWaiter) {
        name = "queue";
        permLevel = PermLevel.STAZYSTA;
        category = CommandCategory.MUSIC;
        enabledInRekru = true;

        this.musicModule = musicModule;
        this.eventWaiter = eventWaiter;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        TrackScheduler trackScheduler = musicModule.getGuildAudioPlayer(context.getGuild()).getScheduler();
        if (trackScheduler == null) {
            context.send("trackScheduler jest nullem").queue();
            return false;
        }

        ArrayList<EmbedBuilder> pages = new ArrayList<>();
        if (trackScheduler.getAktualnaPiosenka() != null) {
            pages.add(getEmbed(trackScheduler.getAktualnaPiosenka(), true));
        }

        for (AudioTrack audioTrack : trackScheduler.getQueue()) {
            pages.add(getEmbed(audioTrack, false));
        }

        if (pages.isEmpty()) {
            context.sendTranslate("queue.empty").queue();
            return false;
        }

        new EmbedPageintaor(pages, context.getUser(), eventWaiter, context.getJDA()).create(context.getChannel(), context.getMessage());
        return true;
    }

    public static EmbedBuilder getEmbed(AudioTrack audioTrack, boolean aktualnieGrana) {
        EmbedBuilder eb = new EmbedBuilder();
        AudioTrackInfo info = audioTrack.getInfo();

        eb.setColor(Color.cyan);
        if (audioTrack.getSourceManager().getSourceName().equalsIgnoreCase("youtube")) {
            eb.setImage(getImageUrl(audioTrack));
        }
        eb.addField("Tytuł", String.format("[%s](%s)", info.title, audioTrack.getInfo().uri), false);
        eb.addField("Autor", info.author, false);

        if (!aktualnieGrana) {
            eb.addField("Długość", info.isStream ? "To jest stream ;p" : longToTimespan(info.length), true);
        } else {
            eb.addField("Długość",  longToTimespan(info.length), true);
            eb.addField("Pozostało", longToTimespan(info.length - audioTrack.getPosition()), false);
        }

        return eb;
    }

    public static String getImageUrl(AudioTrack audtioTrack) {
        return String.format("https://i.ytimg.com/vi_webp/%s/sddefault.webp", audtioTrack.getIdentifier());
    }

    public static String getImageUrl(String audtioTrack) {
        return String.format("https://i.ytimg.com/vi_webp/%s/sddefault.webp", audtioTrack);
    }

    public static String getYtLink(AudioTrack audioTrack) {
        return String.format("https://youtu.be/%s", audioTrack.getIdentifier());
    }

    public static String getYtLink(String audioTrack) {
        return String.format("https://www.youtube.com/watch?v=%s", audioTrack);
    }

    public static String longToTimespan(Number milins) {
        return longToTimespan(milins, FORMATTER);
    }

    public static String longToTimespan(Number milins, DateTimeFormatter formatter) {
        return formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(milins.longValue()), ZoneId.of("GMT")));
    }

}
