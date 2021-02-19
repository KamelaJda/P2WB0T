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

package pl.kamil0024.api;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.api.handlers.*;
import pl.kamil0024.api.internale.MiddlewareBuilder;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.*;
import pl.kamil0024.core.database.config.DiscordInviteConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

import static io.undertow.Handlers.path;

@Getter
public class APIModule implements Modul {

    private final VoiceStateDao voiceStateDao;
    private final ShardManager api;
    private boolean start = false;
    private Undertow server;

    private final CaseDao caseDao;
    private final RedisManager redisManager;
    private final NieobecnosciDao nieobecnosciDao;
    private final StatsDao statsDao;
    private final TicketDao ticketDao;
    private final ApelacjeDao apelacjeDao;
    private final AnkietaDao ankietaDao;
    private final EmbedRedisManager embedRedisManager;
    private final AcBanDao acBanDao;
    private final RecordingDao recordingDao;
    private final DeletedMessagesDao deletedMessagesDao;

    private final Cache<DiscordInviteConfig> dcCache;

    private final Guild guild;

    public APIModule(ShardManager api, CaseDao caseDao, RedisManager redisManager, NieobecnosciDao nieobecnosciDao, StatsDao statsDao, VoiceStateDao voiceStateDao, TicketDao ticketDao, ApelacjeDao apelacjeDao, AnkietaDao ankietaDao, EmbedRedisManager embedRedisManager, AcBanDao acBanDao, RecordingDao recordingDao, DeletedMessagesDao deletedMessagesDao) {
        this.api = api;
        this.redisManager = redisManager;
        this.guild = api.getGuildById(Ustawienia.instance.bot.guildId);
        if (guild == null) throw new UnsupportedOperationException("Gildia docelowa jest nullem!");

        this.caseDao = caseDao;
        this.nieobecnosciDao = nieobecnosciDao;
        this.statsDao = statsDao;
        this.voiceStateDao = voiceStateDao;
        this.ticketDao = ticketDao;
        this.apelacjeDao = apelacjeDao;
        this.ankietaDao = ankietaDao;
        this.embedRedisManager = embedRedisManager;
        this.acBanDao = acBanDao;
        this.recordingDao = recordingDao;
        this.deletedMessagesDao = deletedMessagesDao;

        this.dcCache = redisManager.new CacheRetriever<DiscordInviteConfig>() {}.getCache(3600);
    }

    @Override
    public boolean startUp() {
        RoutingHandler routes = new RoutingHandler();
        routes.get("api/util/checktoken", new CheckToken());

        routes.post("api/ticket/create", new TicketHandler(ticketDao, 0));
        routes.post("api/ticket/read", new TicketHandler(ticketDao, 8));
        routes.post("api/ticket/spam", new TicketHandler(ticketDao, 5));
        routes.get("api/ticket/getspam", new TicketHandler(ticketDao, 6));
        routes.post("api/ticket/getreads", new TicketHandler(ticketDao, 9));
        routes.get("api/ticket/stats", new TicketStatsHandler(ticketDao));
        routes.get("api/ticket/getbyid/{id}/{offset}", new TicketHandler(ticketDao, 1));
        routes.get("api/ticket/getbynick/{id}/{offset}", new TicketHandler(ticketDao, 2));
        routes.get("api/ticket/getbyuserid/{id}/{offset}", new TicketHandler(ticketDao, 3));
        routes.get("api/ticket/getall/{id}/{offset}", new TicketHandler(ticketDao, 4));
        routes.get("api/ticket/getallspam/{id}/{offset}", new TicketHandler(ticketDao, 7));

        routes.post("api/react/apelacje/put", new ApelacjeHandler(apelacjeDao));
        routes.post("api/react/apelacje/edit", new ApelacjeHandler(apelacjeDao, 2));
        routes.post("api/react/apelacje/getall", new ApelacjeHandler(apelacjeDao, 3));
        routes.get("api/react/apelacje/get/{id}", new ApelacjeHandler(apelacjeDao, 1));
        routes.get("api/react/apelacje/getstats", new ApelacjeHandler(apelacjeDao, 4));
        routes.post("api/react/apelacje/getmonthstats", new ApelacjeHandler(apelacjeDao, 5));
        routes.post("api/react/apelacje/ac/put", new AcBanHandler(acBanDao, 1));
        routes.post("api/react/apelacje/ac/edit", new AcBanHandler(acBanDao, 2));
        routes.get("api/react/apelacje/ac/get/{index}", new AcBanHandler(acBanDao, 3));
        routes.post("api/react/ankiety/post", new AnkietaHandler(ankietaDao));
        routes.post("api/react/embed/post", new EmbedHandler(embedRedisManager));

        routes.get("api/chatmod/stats", new ChatmodStats(statsDao, api));
        routes.get("api/chatmod/logs", new MessageLogsHandler(deletedMessagesDao, api));

        routes.get("api/util/staff", new StaffHandler(api));
        routes.get("api/util/channels", new GuildChannelsHandler(api));

        routes.get("api/member/{member}/info", new MemberInfoHandler(api, guild));
        routes.get("api/member/{member}/history", new MemberHistoryHandler(api, caseDao));

        routes.get("api/history/list", new HistoryListHandler(api, caseDao));
        routes.get("api/history/get", new HistoryByIdHandler(api, caseDao));

        routes.get("api/recording/get", new RecordingHandler(recordingDao));
        routes.get("api/discord/{token}/{nick}/{ranga}/{kod}", new DiscordInvite(this));
        routes.get("api/youtrack/reports", new YouTrackReport(api));
        routes.post("api/react/addmember/{id}", new AddMember(api));

        this.server = Undertow.builder()
                .addHttpListener(Ustawienia.instance.api.port, "0.0.0.0")
                .setHandler(path()
                        .addPrefixPath("/", wrapWithMiddleware(routes)))
                .build();
        this.server.start();

        return true;
    }

    @Override
    public boolean shutDown() {
        this.server.stop();
        return true;
    }

    @Override
    public String getName() {
        return "api";
    }

    @Override
    public boolean isStart() {
        return this.start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }

    private static HttpHandler wrapWithMiddleware(HttpHandler handler) {
        return MiddlewareBuilder.begin(BlockingHandler::new).complete(handler);
    }

    //#region Discord Cache
    @Nullable
    public DiscordInviteConfig getDiscordConfig(String nick) {
        return dcCache.getIfPresent(nick);
    }
    
    public void putDiscordConfig(String nick, String kod, String ranga) {
        dcCache.put(kod, new DiscordInviteConfig(nick, kod, ranga));
    }
    //#endregion Discord Cache

}