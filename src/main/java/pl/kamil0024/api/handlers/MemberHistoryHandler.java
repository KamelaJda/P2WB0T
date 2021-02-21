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
import lombok.Data;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.api.Response;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.database.config.UserinfoConfig;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.Kara;
import pl.kamil0024.core.util.kary.KaryEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MemberHistoryHandler implements HttpHandler {

    private final ShardManager api;
    private final CaseDao caseDao;

    @Override
    public void handleRequest(HttpServerExchange ex) {
        if (!Response.checkToken(ex)) return;

        try {
            String id = ex.getQueryParameters().get("member").getFirst();
            int offset = Integer.parseInt(ex.getQueryParameters().get("offset").getFirst());
            if (offset < 0) throw new UnsupportedOperationException("Query nie może być mniejsze od 0");

            List<CaseConfig> cc = caseDao.getAllDesc(id, offset);
            Response.sendObjectResponse(ex, cc.stream().map(a -> ApiCaseConfig.convert(a.getKara(), api)).collect(Collectors.toList()));

        } catch (Exception e) {
            e.printStackTrace();
            Response.sendErrorResponse(ex, "Błąd", "Nie udało się wysłać requesta: " + e.getLocalizedMessage());
        }

    }

    @Data
    @AllArgsConstructor
    public static class ApiCaseConfig {
        private int karaId;
        private UserinfoConfig karany;
        private String mcNick;
        private UserinfoConfig adm;
        private String powod;
        private Long timestamp;
        private KaryEnum typKary;
        private Boolean aktywna;
        private String messageUrl;
        private Long end;
        private String duration;
        private Boolean punAktywna;
        private List<Dowod> dowody;

        public static ApiCaseConfig convert(Kara kara, ShardManager api) {
            return new ApiCaseConfig(kara.getKaraId(),
                            getWhateverConfig(kara.getKaranyId(), api),
                            kara.getMcNick(),
                            getWhateverConfig(kara.getAdmId(), api),
                            kara.getPowod(),
                            kara.getTimestamp(),
                            kara.getTypKary(),
                            kara.getAktywna(),
                            kara.getMessageUrl(),
                            kara.getEnd(),
                            kara.getDuration(),
                            kara.getPunAktywna(),
                            kara.getDowody());
        }

    }

    @SuppressWarnings("ConstantConditions")
    public static UserinfoConfig getWhateverConfig(String id, ShardManager api) {
        Member mem = api.getGuildById(Ustawienia.instance.bot.guildId).getMemberById(id);
        if (mem != null) return UserinfoConfig.convert(mem);
        return UserinfoConfig.convert(api.getUserById(id));
    }

}
