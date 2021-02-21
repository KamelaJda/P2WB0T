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
import pl.kamil0024.core.database.ApelacjeDao;
import pl.kamil0024.core.database.config.ApelacjeConfig;
import org.joda.time.DateTime;

import java.util.Date;

public class ApelacjeHandler implements HttpHandler {

    private final ApelacjeDao apelacjeDao;
    private final int type;

    public ApelacjeHandler(ApelacjeDao apelacjeDao) {
        this.apelacjeDao = apelacjeDao;
        this.type = 0;
    }

    public ApelacjeHandler(ApelacjeDao apelacjeDao, int type) {
        this.apelacjeDao = apelacjeDao;
        this.type = type;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) { return; }

        if (type == 3) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String filtr = json.getString("filtr");
                String value = json.getString("value");
                int offset = json.getInt("offset");

                if (!filtr.equals("all") && !filtr.equals("nick") && !filtr.equals("id")) {
                    Response.sendErrorResponse(ex, "Błąd!", "filtr typu " + filtr + " jest nieprawidłowy");
                    return;
                }

                if (filtr.equalsIgnoreCase("all")) Response.sendObjectResponse(ex, apelacjeDao.getAll(offset));
                if (filtr.equalsIgnoreCase("nick")) Response.sendObjectResponse(ex, apelacjeDao.getAllByNick(value, offset));
                if (filtr.equalsIgnoreCase("id")) Response.sendObjectResponse(ex, apelacjeDao.get(value));

            } catch (Exception e) {
                e.printStackTrace();
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 0 || type == 2) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                String id = json.getString("id");
                String bannedNick = json.getString("bannedNick");
                String admNick = json.getString("admNick");
                String apelacjeNick = json.getString("apelacjeNick");
                String reason = json.getString("reason");
                String slusznaBlokada = json.getString("slusznaBlokada");
                long apelacjaCreated = json.getLong("apelacjaCreated");
                String dodatkowaUwaga = null;
                try {
                    dodatkowaUwaga = json.getString("dodatkowaUwaga");
                } catch (Exception ignored) { }

                String unbanned = json.getString("unbanned");
                long createdTime = new Date().getTime();
                ApelacjeConfig aa = apelacjeDao.get(id);
                if (type == 0 && aa != null) {
                    Response.sendErrorResponse(ex, "Błąd!", "Takie ID apelacji już istnieje.");
                    return;
                }

                if (type == 2 && aa == null) {
                    Response.sendErrorResponse(ex, "Błąd!", "Takie ID apelacji nie istnieje.");
                    return;
                }

                ApelacjeConfig ac = new ApelacjeConfig(id);
                ac.setAdmNick(admNick);
                ac.setApelacjeNick(apelacjeNick);
                ac.setBannedNick(bannedNick);
                ac.setCreatedTime(createdTime);
                ac.setDodatkowaUwaga(dodatkowaUwaga);
                ac.setUnbanned(unbanned);
                ac.setReason(reason);
                ac.setSlusznaBlokada(slusznaBlokada);
                ac.setCreatedApelacja(apelacjaCreated);

                Response.sendResponse(ex, "Pomyślnie zapisano");
                apelacjeDao.save(ac);

            } catch (Exception e) {
                e.printStackTrace();
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
            return;
        }

        if (type == 1) {
            try {
                String id = ex.getQueryParameters().get("id").getFirst();
                ApelacjeConfig apelacja = apelacjeDao.get(id);
                if (apelacja == null) {
                    Response.sendErrorResponse(ex, "Błąd!", "Nie ma apelacji o takim ID");
                    return;
                }
                Response.sendObjectResponse(ex, apelacja);
            } catch (Exception e) {
                e.printStackTrace();
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
        }

        if (type == 4) {
            Response.sendObjectResponse(ex, ApelacjeDao.getFrom(apelacjeDao.getAll(), new DateTime(), 30));
        }

        if (type == 5) {
            try {
                JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
                int year = json.getInt("year");
                int month = json.getInt("month");
                Response.sendObjectResponse(ex, ApelacjeDao.getFromMonth(apelacjeDao.getAll(), month, year));
            } catch (Exception e) {
                Response.sendErrorResponse(ex, "Błąd!", "Nie udało się wysłać requesta: " + e.getMessage());
            }
        }
    }

}
