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

package pl.kamil0024.api.redisstats.modules;

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.joda.time.DateTime;
import pl.kamil0024.api.redisstats.config.ChatModStatsConfig;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.database.config.CaseConfig;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Kara;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class CaseRedisManager {

    @Setter
    private long lastUpdate = 0;

    private final CaseDao caseDao;
    private final Map<Long, Integer> mapKaryWRoku;
    private final Map<Long, Integer> mapWTygodniu;
    private final Map<Long, Integer> mapOstatnieKary24h;
    private final Map<Long, Integer> mapKaryWMiesiacu;

    private final Map<Long, List<ChatModStatsConfig>> mapChatmodWMiesiacu;
    private final Map<Long, List<ChatModStatsConfig>> mapChatmodWRoku;

    private final ScheduledExecutorService executorSche;
    private final ShardManager api;

    public CaseRedisManager(CaseDao caseDao, ShardManager api) {
        this.caseDao = caseDao;
        this.api = api;

        this.mapKaryWRoku = new HashMap<>();
        this.mapWTygodniu = new HashMap<>();
        this.mapOstatnieKary24h = new HashMap<>();
        this.mapKaryWMiesiacu = new HashMap<>();
        this.mapChatmodWMiesiacu = new HashMap<>();
        this.mapChatmodWRoku = new HashMap<>();

        executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(() -> {
            try {
                load();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    public synchronized void load() {
        getMapKaryWRoku().clear();
        getMapOstatnieKary24h().clear();
        getMapWTygodniu().clear();

        setLastUpdate(new Date().getTime());

        Map<String, String> nicknames = new HashMap<>();

        Map<Long, Integer> kryWRoku = new HashMap<>();
        Map<Long, Integer> karyWTygodniu = new HashMap<>();
        Map<Long, Integer> karyWMiesiacu = new HashMap<>();

        Map<Long, List<ChatModStatsConfig>> chatmodWMiesiacu = new HashMap<>();
        Map<Long, List<ChatModStatsConfig>> chatModWRoku = new HashMap<>();

        Map<Long, Integer> ostatnieKary24h = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        DateTime now = new DateTime();
        for (CaseConfig caseConfig : caseDao.getAll()) {
            Kara kara = caseConfig.getKara();
            DateTime dt = new DateTime(kara.getTimestamp());

            long h = dt.getMonthOfYear();
            kryWRoku.put(h, (kryWRoku.getOrDefault(h, 0)) + 1);

            String chatmodId = kara.getAdmId();
            List<ChatModStatsConfig> lista = chatModWRoku.getOrDefault(h, new ArrayList<>());

            if (lista.isEmpty() || ChatModStatsConfig.consId(chatmodId, lista)) {
                ChatModStatsConfig cst = new ChatModStatsConfig();
                cst.setLiczbaKar(1);
                cst.setId(chatmodId);
                String nick = nicknames.get(chatmodId);
                if (nick == null) {
                    User u = api.retrieveUserById(chatmodId).complete();
                    try {
                        String n = UserUtil.getMcNick(Objects.requireNonNull(api.getGuildById(Ustawienia.instance.bot.guildId)).retrieveMemberById(chatmodId).complete(), true);
                        nicknames.put(chatmodId, n);
                        cst.setNick(n);
                    } catch (Exception e) {
                        String n = UserUtil.getName(u);
                        nicknames.put(chatmodId, n);
                        cst.setNick(n);
                    }
                } else cst.setNick(nick);
                lista.add(cst);
            } else {
                for (ChatModStatsConfig config : lista) {
                    if (config.getId().equals(chatmodId)) {
                        config.setLiczbaKar(config.getLiczbaKar() + 1);
                    }
                }
            }

            chatModWRoku.put(h, lista);

            cal.setTime(new Date(dt.getMillis()));
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (dt.isAfter(now.minusDays(8))) {
                long ms = cal.toInstant().toEpochMilli();
                karyWTygodniu.put(ms, (karyWTygodniu.getOrDefault(ms, 0)) + 1);
            }

            if (dt.isAfter(now.minusDays(1)) && dt.getDayOfYear() == now.getDayOfYear()) {
                long hofday = dt.getHourOfDay();
                ostatnieKary24h.put(hofday, (ostatnieKary24h.getOrDefault(hofday, 0)) + 1);
            }

            if (dt.getMonthOfYear() == now.getMonthOfYear() && dt.getYear() == now.getYear()) {
                long ms = cal.toInstant().toEpochMilli();
                karyWMiesiacu.put(ms, (karyWMiesiacu.getOrDefault(ms, 0)) + 1);

                List<ChatModStatsConfig> listaMsc = chatmodWMiesiacu.getOrDefault(ms, new ArrayList<>());
                if (listaMsc.isEmpty() || ChatModStatsConfig.consId(chatmodId, listaMsc)) {
                    ChatModStatsConfig cst = new ChatModStatsConfig();
                    cst.setId(chatmodId);
                    cst.setLiczbaKar(1);
                    lista.add(cst);
                } else {
                    for (ChatModStatsConfig config : listaMsc) {
                        if (config.getId().equals(chatmodId)) {
                            config.setLiczbaKar(config.getLiczbaKar() + 1);
                        }
                    }
                }
                chatmodWMiesiacu.put(ms, lista);
            }
        }

        for (Map.Entry<Long, List<ChatModStatsConfig>> entry : chatModWRoku.entrySet()) {
            for (ChatModStatsConfig chatModStatsConfig : entry.getValue()) {
                if (chatModStatsConfig.getNick() == null || chatModStatsConfig.getNick().isEmpty()) {
                    chatModWRoku.remove(entry.getKey());
                }
            }
        }

        mapKaryWRoku.putAll(kryWRoku);
        mapWTygodniu.putAll(karyWTygodniu);
        mapOstatnieKary24h.putAll(ostatnieKary24h);
        mapKaryWMiesiacu.putAll(karyWMiesiacu);
        mapChatmodWMiesiacu.putAll(chatmodWMiesiacu);
        mapChatmodWRoku.putAll(chatModWRoku);

    }
}
