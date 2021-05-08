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
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.json.JSONObject;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.database.AcBanDao;
import pl.kamil0024.core.database.config.AcBanConfig;

import java.util.Random;

public class AcBanHandler implements HttpHandler {

    private final static Gson GSON = new Gson();

    private final Random rand = new Random();
    private final AcBanDao acBanDao;
    private final int id;

    public AcBanHandler(AcBanDao acBanDao, int id) {
        this.acBanDao = acBanDao;
        this.id = id;
    }

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkIp(ex)) {
            return;
        }

        if (id == 3) {
            try {
                String readed = ex.getQueryParameters().get("readed").getFirst();
                int index = Integer.parseInt(ex.getQueryParameters().get("index").getFirst());
                Response.sendObjectResponse(ex, acBanDao.getAll(index, Boolean.parseBoolean(readed)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        JSONObject json = new JSONObject(Response.getBody(ex.getInputStream()));
        AcBanConfig acBanConfig = GSON.fromJson(json.toString(), AcBanConfig.class);

        if (id == 1) { // put
            if (acBanDao.existNick(acBanConfig.getNick())) {
                Response.sendErrorResponse(ex, "Błąd!", "Ten nick już istnieje!");
                return;
            }
            acBanConfig.setId(rand.nextInt(10000000) + "");
            Response.sendResponse(ex, "Pomyślnie dodano");
            acBanDao.save(acBanConfig);
            return;
        }

        if (id == 2) { // edit
            Response.sendResponse(ex, "Pomyślnie zmienono");
            acBanDao.save(acBanConfig);
        }

    }

}
