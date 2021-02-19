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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.database.config.UserinfoConfig;
import pl.kamil0024.stats.commands.StatsCommand;
import pl.kamil0024.stats.commands.TopCommand;
import pl.kamil0024.stats.entities.Statystyka;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pl.kamil0024.stats.commands.TopCommand.sortByValue;

@AllArgsConstructor
public class ChatmodStats implements HttpHandler {

    private final StatsDao statsDao;
    private final ShardManager api;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) { return; }

       try {
           List<StatsConfig> staty = statsDao.getAll();
           HashMap<String, TopCommand.Suma> mapa = new HashMap<>();
           HashMap<String, Integer> top = new HashMap<>();

           for (StatsConfig statsConfig : staty) {
               int suma = 0;
               Statystyka statyZParuDni = StatsCommand.getStatsOfDayMinus(statsConfig.getStats(), Integer.parseInt(ex.getQueryParameters().get("dni").getFirst()));
               suma += (statyZParuDni.getWyrzuconych() +
                       statyZParuDni.getZbanowanych() +
                       statyZParuDni.getZmutowanych());

               mapa.put(statsConfig.getId(), new TopCommand.Suma(suma, statyZParuDni));
           }

           for (Map.Entry<String, TopCommand.Suma> entry : mapa.entrySet()) {
               top.put(entry.getKey(), entry.getValue().getNadaneKary());
           }

           Map<String, TopCommand.Suma> finalStats = new HashMap<>();
           for (Map.Entry<String, Integer> entry : sortByValue(top).entrySet()) {
               try {
                   UserinfoConfig user = MemberHistoryHandler.getWhateverConfig(entry.getKey(), api);
                   if (user.getMcNick() == null) continue;
                   finalStats.put(user.getMcNick(), mapa.get(entry.getKey()));
               } catch (Exception ignored) { }
           }
           Response.sendObjectResponse(ex, finalStats);

       } catch (Exception e) {
           Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać zapyania: " + e.getLocalizedMessage());
           e.printStackTrace();
       }

    }

}
