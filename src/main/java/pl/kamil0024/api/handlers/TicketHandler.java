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
import org.json.JSONObject;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.TicketDao;
import pl.kamil0024.core.database.config.TicketConfig;

import java.util.Date;

public class TicketHandler implements HttpHandler {

    private final TicketDao ticketDao;
    private final int type;

    public TicketHandler(TicketDao ticketDao, int type) {
        this.ticketDao = ticketDao;
        this.type = type;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) {
            return;
        }

        if (type == 0) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String id = json.getString("id");
                int ocena = json.getInt("rating");
                String temat = json.getString("tematPomocy");
                boolean problemRozwiazany = json.getBoolean("pomocUdana");
                String uwaga = null;
                try {
                    uwaga = json.getString("uwagi");
                } catch (Exception ignored) {
                }
                TicketConfig tc = ticketDao.get(id);
                if (!TicketConfig.exist(tc)) {
                    Response.sendErrorResponse(ex, "Błąd!", "Nie ma ticketa o takim ID!");
                    return;
                }
                tc.setOcena(ocena);
                tc.setTemat(temat);
                tc.setProblemRozwiazany(problemRozwiazany);
                tc.setCompleteTime(new Date().getTime());
                if (uwaga != null) tc.setUwaga(uwaga);
                Response.sendResponse(ex, "Pomyślnie zapisano");
                ticketDao.save(tc);
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 5) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String id = json.getString("id");
                String admNick = json.getString("admNick");
                boolean setSpam = json.getBoolean("setSpam");
                TicketConfig tc = ticketDao.get(id);
                if (!TicketConfig.exist(tc)) {
                    Response.sendErrorResponse(ex, "Błąd!", "Nie ma ticketa o takim ID!");
                    return;
                }
                tc.setSpam(setSpam);
                tc.setSpamAdm(admNick);
                Response.sendResponse(ex, "Pomyślnie zapisano");
                ticketDao.save(tc);
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 6) {
            try {
                String id = ex.getRequestHeaders().get("userId").getFirst();
                TicketConfig tc = ticketDao.getSpam(id);
                if (tc == null) {
                    Response.sendResponse(ex, "Ten gracz jest czysty");
                    return;
                }
                Response.sendObjectResponse(ex, tc);
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 8) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String id = json.getString("id");
                String admId = json.getString("admId");
                TicketConfig tc = ticketDao.get(id);
                if (!TicketConfig.exist(tc)) {
                    Response.sendErrorResponse(ex, "Błąd!", "Nie ma ticketa o takim ID!");
                    return;
                }
                tc.getReadBy().remove(admId);
                tc.getReadBy().add(admId);
                Response.sendResponse(ex, "Pomyślnie zapisano");
                ticketDao.save(tc);
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 9) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String admId = json.getString("admId");
                boolean read = json.getBoolean("read");
                int offset = json.getInt("offset");
                Response.sendObjectResponse(ex, ticketDao.getAllTickets(offset, admId, read));
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        try {
            String id = "0";
            try {
                id = ex.getQueryParameters().get("id").getFirst();
            } catch (NullPointerException ignored) {
            }

            int offset = 0;
            try {
                offset = Integer.parseInt(ex.getQueryParameters().get("offset").getFirst());
            } catch (NumberFormatException ignored) {
            }
            switch (type) {
                case 1:
                    Response.sendObjectResponse(ex, ticketDao.get(id));
                    break;
                case 2:
                    Response.sendObjectResponse(ex, ticketDao.getByNick(id, offset));
                    break;
                case 3:
                    Response.sendObjectResponse(ex, ticketDao.getById(id, offset));
                    break;
                case 4:
                    Response.sendObjectResponse(ex, ticketDao.getAllTickets(offset));
                    break;
                case 7:
                    Response.sendObjectResponse(ex, ticketDao.getAllTicketsSpam(offset));
                    break;
                default:
                    throw new UnsupportedOperationException("Zły int type");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
        }
    }

}
