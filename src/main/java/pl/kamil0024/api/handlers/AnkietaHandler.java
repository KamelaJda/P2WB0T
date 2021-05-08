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
import com.google.gson.GsonBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.json.JSONObject;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.AnkietaDao;
import pl.kamil0024.core.database.config.AnkietaConfig;

public class AnkietaHandler implements HttpHandler {

    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private final AnkietaDao ankietaDao;

    public AnkietaHandler(AnkietaDao ankietaDao) {
        this.ankietaDao = ankietaDao;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) {
            return;
        }
        try {
            JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
            AnkietaConfig conf = gson.fromJson(json.toString(), AnkietaConfig.class);
            Response.sendResponse(ex, "Pomyślnie zapisano");
            ankietaDao.save(conf);
            ankietaDao.send(conf);
        } catch (Exception e) {
            Response.sendErrorResponse(ex, "Błąd", "Nie udało się wysłać requesta! " + e.getMessage());
        }

    }

}
