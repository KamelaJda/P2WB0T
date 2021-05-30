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

package pl.kamil0024.stats.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.*;
import pl.kamil0024.music.MusicModule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TekstCommand extends Command {

    private final EventWaiter eventWaiter;
    private final MusicModule musicModule;

    public TekstCommand(EventWaiter eventWaiter, MusicModule musicModule) {
        name = "tekst";
        aliases.add("lyrics");
        category = CommandCategory.MUSIC;

        this.eventWaiter = eventWaiter;
        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String arg;
        AudioTrack track = musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack();

        String arg0 = context.getArgs().get(0);
        if (UserUtil.getPermLevel(context.getMember()).getNumer() >= PermLevel.STAZYSTA.getNumer() && arg0 == null && musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack() != null) {
            arg = track.getInfo().title;
        } else {
            arg = context.getArgsToString(0);
            if (arg0 == null) throw new UsageException();
        }

        try {
            JSONObject job = NetworkUtil.getJson("https://some-random-api.ml/lyrics?title=" + NetworkUtil.encodeURIComponent(arg));

            String tytul = Objects.requireNonNull(job).getString("title");
            String author = Objects.requireNonNull(job).getString("author");
            String lyrics = Objects.requireNonNull(job).getString("lyrics");
            JSONObject thumbnail = job.getJSONObject("thumbnail");
            JSONObject links = job.getJSONObject("links");

            ArrayList<EmbedBuilder> pages = new ArrayList<>();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.addField(context.getTranslate("tekst.autor"), author, true);
            eb.addField(context.getTranslate("tekst.tytul"), String.format("[%s](%s)", tytul, links.getString("genius")), true);
            eb.setTimestamp(Instant.now());
            eb.setImage(thumbnail.getString("genius"));

            StringBuilder sb = new StringBuilder();

            List<EmbedBuilder> teksty = new ArrayList<>();
            EmbedBuilder tekst = new EmbedBuilder();

            tekst.setTimestamp(Instant.now());
            tekst.setColor(UserUtil.getColor(context.getMember()));

            for (String s : lyrics.split("\n")) {
                sb.append(s).append("\n");
                if (sb.length() >= 900) {
                    tekst.addField(" ", sb.toString(), false);
                    sb = new StringBuilder();

                    if (tekst.length() > 5600) {
                        teksty.add(tekst);
                        tekst = new EmbedBuilder();
                        tekst.setTimestamp(Instant.now());
                        tekst.setColor(UserUtil.getColor(context.getMember()));
                    }
                }
            }

            pages.add(eb);
            if (!teksty.isEmpty()) {
                pages.addAll(teksty);
            } else pages.add(tekst);
            if (!sb.toString().isEmpty()) tekst.addField(" ", sb.toString(), false);

            new EmbedPaginator(pages, context.getUser(), eventWaiter).
                    create(context.getChannel(), context.getMessage());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.sendTranslate("tekst.error").queue();
        return false;
    }

}
