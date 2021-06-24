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
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.*;
import pl.kamil0024.music.MusicModule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;

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
            Piosenka piosenka = requestGenius(arg);

            List<FutureTask<EmbedBuilder>> pages = new ArrayList<>();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.addField(context.getTranslate("tekst.autor"), piosenka.getAuthor(), true);
            eb.addField(context.getTranslate("tekst.tytul"), String.format("[%s](%s)", piosenka.getTitle(), piosenka.getWebsite()), true);
            eb.setTimestamp(Instant.now());
            eb.setImage(piosenka.getImageLink());

            StringBuilder sb = new StringBuilder();

            List<EmbedBuilder> teksty = new ArrayList<>();
            EmbedBuilder tekst = new EmbedBuilder();

            tekst.setTimestamp(Instant.now());
            tekst.setColor(UserUtil.getColor(context.getMember()));

            for (String s : piosenka.getSlowa().split("\n")) {
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

            pages.add(new FutureTask<>(() -> eb));

            if (!teksty.isEmpty()) {
                for (EmbedBuilder builder : teksty) {
                    pages.add(new FutureTask<>(() -> builder));
                }
            } else {
                EmbedBuilder finalTekst = tekst;
                pages.add(new FutureTask<>(() -> finalTekst));
            }
            if (!sb.toString().isEmpty()) tekst.addField(" ", sb.toString(), false);

            Message message = context.send("Ładuje");
            new DynamicEmbedPaginator(pages, context.getUser(), eventWaiter, 60).create(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        context.sendTranslate("tekst.error");
        return false;
    }

    private Piosenka requestGenius(String q) throws IOException {
        JSONObject xd = NetworkUtil.getJson("https://api.genius.com/search?q=" + NetworkUtil.encodeURIComponent(q), "Bearer " + Ustawienia.instance.api.geniusToken);
        if (xd == null) throw new IOException("resp == null");
        JSONArray arr = xd.getJSONObject("response").getJSONArray("hits");
        if (arr.isEmpty()) throw new IOException("arr == empty");
        JSONObject song = arr.getJSONObject(0).getJSONObject("result");
        String imageLink = song.getString("song_art_image_url");
        String title = song.getString("full_title");
        String lyricsPath = song.getString("path");
        String author = song.getJSONObject("primary_artist").getString("name");
        byte[] html = NetworkUtil.download("https://genius.com" + lyricsPath);
        Document doc = Jsoup.parse(new String(html, StandardCharsets.UTF_8));
        Element slowaElement = doc.select(".lyrics p").get(0);
        StringBuilder slowa = new StringBuilder();
        for (Node n : slowaElement.childNodes()) {
            if (n instanceof TextNode) {
                slowa.append(((TextNode) n).getWholeText());
            } else if (n instanceof Element) {
                if (((Element) n).tagName().equals("a")) slowa.append(((Element) n).wholeText());
                if (((Element) n).tagName().equals("i"))
                    slowa.append("_").append(((Element) n).wholeText()).append("_");
                if (((Element) n).tagName().equals("b")) slowa.append(((Element) n).wholeText());
            }
        }
        return new Piosenka(imageLink, title, "https://genius.com" + lyricsPath, slowa.toString(), author);
    }

    @AllArgsConstructor
    @Data
    private static class Piosenka {
        private final String imageLink;
        private final String title;
        private final String website;
        private final String slowa;
        private final String author;
    }

}