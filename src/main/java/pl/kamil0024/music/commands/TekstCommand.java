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
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.json.JSONObject;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
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
        commandData = new CommandData(name, Tlumaczenia.get(name + ".opis"))
                .addOption(OptionType.STRING, "piosenka", "Tytuł piosenki", false);
        hideSlash = true;
    }

    @Override
    public boolean execute(SlashContext context) {
        String arg;
        AudioTrack track = musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack();

        OptionMapping arg0 = context.getEvent().getOption("piosenka");
        if (UserUtil.getPermLevel(context.getMember()).getNumer() >= PermLevel.STAZYSTA.getNumer() && arg0 == null && musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack() != null) {
            arg = track.getInfo().title;
        } else {
            if (arg0 == null) throw new UsageException();
            arg = arg0.getAsString();
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

            context.send("Komenda wykonana!");
            new EmbedPaginator(pages, context.getUser(), eventWaiter).create(context.getChannel());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.sendTranslate("tekst.error");
        return false;
    }

}
