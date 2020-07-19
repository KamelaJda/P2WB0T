package pl.kamil0024.stats.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import org.json.JSONObject;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.*;
import pl.kamil0024.music.MusicModule;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TekstCommand extends Command {

    private EventWaiter eventWaiter;
    private MusicModule musicModule;

    public TekstCommand(EventWaiter eventWaiter, MusicModule musicModule) {
        name = "tekst";
        aliases.add("lyrics");
        category = CommandCategory.MUSIC;
        permLevel = PermLevel.HELPER;

        this.eventWaiter = eventWaiter;
        this.musicModule = musicModule;
    }

    @Override
    public boolean execute(CommandContext context) {
        String arg;
        AudioTrack track = musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack();

        String arg0 = context.getArgs().get(0);
        if (arg0 == null && musicModule.getGuildAudioPlayer(context.getGuild()).getPlayer().getPlayingTrack() != null) {
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
            eb.addField("Autor", author, true);
            eb.addField("Tytuł", String.format("[%s](%s)", tytul, links.getString("genius")), true);
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

            new EmbedPageintaor(pages, context.getUser(), eventWaiter, context.getJDA()).create(context.getChannel());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        context.send("Wstąpił zewnętrzny błąd z API (lub nie znaleziono piosenki)!").queue();
        return false;
    }

}
