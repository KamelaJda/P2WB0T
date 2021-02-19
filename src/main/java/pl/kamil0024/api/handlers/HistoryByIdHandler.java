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
import pl.kamil0024.api.Response;
import pl.kamil0024.commands.system.UserinfoCommand;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.kary.Kara;

@AllArgsConstructor
public class HistoryByIdHandler implements HttpHandler {

    private final ShardManager api;
    private final CaseDao caseDao;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkToken(ex)) return;

        try {
            CaseConfig cc = caseDao.get(ex.getQueryParameters().get("id").getFirst());
            if (cc.getKara() == null) throw new UnsupportedOperationException("Nie ma kary o takim id!");
            Response.sendObjectResponse(ex, MemberHistoryHandler.ApiCaseConfig.convert(cc.getKara(), api));
        } catch (Exception e) {
            Response.sendErrorResponse(ex, "Błąd", "Nie udało się wysłać requesta: " + e.getLocalizedMessage());
        }

    }

}
