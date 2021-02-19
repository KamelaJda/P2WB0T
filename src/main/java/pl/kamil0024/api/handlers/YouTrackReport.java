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

package pl.kamil0024.api.handlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.stats.commands.TopCommand;

import java.awt.*;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class YouTrackReport implements HttpHandler {

    private final ShardManager api;

    public YouTrackReport(ShardManager api) {
        this.api = api;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkToken(ex)) return;
        Response.sendResponse(ex, "Pomyślnie wysłano reporta!");

        String header = ex.getRequestHeaders().get("data").getFirst();
        String iloscIssuesow = ex.getRequestHeaders().get("issues").getFirst();
        String zakonczonychIssuesow = ex.getRequestHeaders().get("resolved").getFirst();
        String projekt = ex.getRequestHeaders().get("project").getFirst();
        String testerzy = ex.getRequestHeaders().get("testerzy").getFirst();

        Type typeOfHashMap = new TypeToken<Map<String, Integer>>() { }.getType();
        HashMap<String, Integer> map = new Gson().fromJson(header, typeOfHashMap);
        HashMap<String, Integer> testerzyMap = new Gson().fromJson(testerzy, typeOfHashMap);
        TextChannel txt = api.getTextChannelById("738122215878295572");

        MessageBuilder mb = new MessageBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.**MM**");
        EmbedBuilder eb = new EmbedBuilder();

        mb.setContent("Statystyki z " + sdf.format(new Date()));
        eb.setColor(Color.red);
        eb.setTimestamp(Instant.now());
        eb.setTitle(projekt);

        //#region Total stats
        String totalStatsBuilder = "Wszystkich issuesów: " + iloscIssuesow + "\n" +
                "Zakończonych issuesów: " + zakonczonychIssuesow;
        eb.addField("Ogólne statystyki", totalStatsBuilder, false);
        //#endregion Total stats

        //#region Top stworzonych issuesów
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        StringBuilder topCreated = new StringBuilder(getPodium(map));
        SimpleDateFormat firstFormat = new SimpleDateFormat("yyyy-MM-");

        String format = String.format("issues?q=created:%s%s..%s%s",
                firstFormat.format(new Date()) + "01", "%20", "%20",
                firstFormat.format(new Date()) + maxDay);
        String string = String.format("[TUTAJ](%s)", Ustawienia.instance.yt.url + "/" + format);

        topCreated.append("\n").append("Lista issuesów dostępna jest ").append(string);
        eb.addField("Topka stworzonych issuesów", topCreated.toString(), false);
        //#endregion Top stworzonych issuesów

        //#region Top testowanych issuesów
        eb.addField("Topka testowanych issuesów", getPodium(testerzyMap), false);
        //#endregion Top testowanych issuesów

        mb.setEmbed(eb.build());
        Objects.requireNonNull(txt).sendMessage(mb.build()).queue();
    }

    private String getPodium(HashMap<String, Integer> map) {
        int miejsce = 1;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : TopCommand.sortByValue(map).entrySet()) {
            sb.append(miejsce).append(". ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            miejsce++;
        }
        return sb.toString();
    }

}
