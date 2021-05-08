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
import lombok.Data;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.TicketDao;
import pl.kamil0024.core.database.config.TicketConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TicketStatsHandler implements HttpHandler {

    private final TicketDao ticketDao;

    public TicketStatsHandler(TicketDao ticketDao) {
        this.ticketDao = ticketDao;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) {
            return;
        }

        long start, end;

        try {
            start = Long.parseLong(ex.getQueryParameters().get("start").getFirst());
            end = Long.parseLong(ex.getQueryParameters().get("end").getFirst());
        } catch (Exception e) {
            Response.sendErrorResponse(ex, "Brak parametrów!", "Brakuje parametrów end i start");
            return;
        }

        TicketStats ticketStats = new TicketStats();
        List<TicketConfig> tc = ticketDao.getAll();
        tc.removeIf(t -> !(t.getCreatedTime() <= end && t.getCreatedTime() >= start));
        ticketStats.setLiczbaZgloszen(tc.size());

        Map<String, UserTicketStats> stats = new HashMap<>();

        for (TicketConfig entry : tc) {
            if (entry.getAdmNick() == null) continue;

            UserTicketStats st = stats.getOrDefault(entry.getAdmNick(), new UserTicketStats());

            st.setLiczbaZgloszen(st.getLiczbaZgloszen() + 1);

            if (entry.getOcena() != -1) {
                if (entry.getOcena() > 3) st.setPozytywneZgloszenia(st.getPozytywneZgloszenia() + 1);
                else st.setNegatywneZgloszenia(st.getNegatywneZgloszenia() + 1);
            }

            if (entry.getKategoria() != null) {
                int i = (st.getKategorie().getOrDefault(entry.getKategoria(), 0)) + 1;
                st.getKategorie().put(entry.getKategoria(), i);
            }

            stats.put(entry.getAdmNick(), st);
        }
        ticketStats.setStats(stats);
        Response.sendObjectResponse(ex, ticketStats);
    }

    @Data
    private static class UserTicketStats {
        public UserTicketStats() {
        }

        private int liczbaZgloszen = 0;
        private int negatywneZgloszenia = 0;
        private int pozytywneZgloszenia = 0;
        private Map<String, Integer> kategorie = new HashMap<>();
    }

    @Data
    private static class TicketStats {
        public TicketStats() {
        }

        private int liczbaZgloszen = 0;
        private Map<String, UserTicketStats> stats = new HashMap<>();

    }

}
