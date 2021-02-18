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

import com.google.inject.Inject;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.api.handlers.*;
import pl.kamil0024.api.internale.MiddlewareBuilder;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.*;
import pl.kamil0024.core.database.config.DiscordInviteConfig;
import pl.kamil0024.core.database.config.UserinfoConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static io.undertow.Handlers.path;

@Getter
public class APIModule implements Modul {

    private final VoiceStateDao voiceStateDao;
    private final ShardManager api;
    private boolean start = false;
    Undertow server;

    @Inject private final CaseDao caseDao;
    @Inject private final RedisManager redisManager;
    @Inject private final NieobecnosciDao nieobecnosciDao;
    @Inject private final StatsDao statsDao;
    @Inject private final TicketDao ticketDao;
    @Inject private final ApelacjeDao apelacjeDao;
    @Inject private final AnkietaDao ankietaDao;
    @Inject private final EmbedRedisManager embedRedisManager;
    @Inject private final AcBanDao acBanDao;
    @Inject private final RecordingDao recordingDao;
    @Inject private final DeletedMessagesDao deletedMessagesDao;

    private final Cache<UserinfoConfig> ucCache;
    private final Cache<DiscordInviteConfig> dcCache;
    private final Cache<ChatModUser> cdCache;

    private final Guild guild;

    private final ScheduledExecutorService executorSche;

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

        this.ucCache = redisManager.new CacheRetriever<UserinfoConfig>(){}.getCache(3600);
        this.dcCache = redisManager.new CacheRetriever<DiscordInviteConfig>() {}.getCache(3600);
        this.cdCache = redisManager.new CacheRetriever<ChatModUser>() {}.getCache(3600);

        executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::refreshChatmod, 0, 30, TimeUnit.MINUTES);
    }

    @Override
    public boolean startUp() {

        RoutingHandler routes = new RoutingHandler();
        routes.get("/api/checkToken/{token}", new CheckToken());
        routes.get("/api/karainfo/{token}/{id}", new Karainfo(caseDao, this));
        routes.get("/api/listakar/{token}/{nick}", new Listakar(caseDao, this));
        routes.get("api/nieobecnosci/{token}/{data}", new Nieobecnosci(nieobecnosciDao, this));
        routes.get("api/stats/{token}/{dni}/{nick}", new StatsHandler(statsDao, this));
        routes.get("api/stats/{token}/{dni}/id/{nick}", new StatsHandler(statsDao, this, true));
        routes.get("api/discord/{token}/{nick}/{ranga}/{kod}", new DiscordInvite(this));

        routes.get("api/youtrack/reports", new YouTrackReport(api));

        routes.get("api/react/history/{token}/{id}/{offset}", new HistoryDescById(caseDao));
        routes.get("api/react/permlevel/{token}", new UserPermLevel(api));
        routes.get("api/react/chatmod/{token}/list", new ChatMod(api, this));
        routes.get("api/react/userinfo/{token}/{id}", new UserInfo(api));
        routes.get("api/react/memberinfo/{id}", new MemberInfoHandler(api));
        routes.post("api/react/addmember/{id}", new AddMember(api));

        routes.post("api/ticket/create", new TicketHandler(ticketDao, 0));
        routes.get("api/ticket/getbyid/{id}/{offset}", new TicketHandler(ticketDao, 1));
        routes.get("api/ticket/getbynick/{id}/{offset}", new TicketHandler(ticketDao, 2));
        routes.get("api/ticket/getbyuserid/{id}/{offset}", new TicketHandler(ticketDao, 3));
        routes.get("api/ticket/getall/{id}/{offset}", new TicketHandler(ticketDao, 4));
        routes.get("api/ticket/getallspam/{id}/{offset}", new TicketHandler(ticketDao, 7));
        routes.post("api/ticket/spam", new TicketHandler(ticketDao, 5));
        routes.get("api/ticket/getspam", new TicketHandler(ticketDao, 6));
        routes.post("api/ticket/read", new TicketHandler(ticketDao, 8));
        routes.post("api/ticket/getreads", new TicketHandler(ticketDao, 9));

//        routes.get("api/react/stats/chatmod", new RedisChatModStats(redisStatsManager));

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

        routes.get("api/staff", new StaffHandler(api));
        routes.get("api/recording", new RecordingHandler(recordingDao));
        routes.get("api/ticket/stats", new TicketStatsHandler(ticketDao));

        routes.get("api/chatmod/stats", new ChatmodStats(statsDao, this));

        routes.get("api/channels", new GuildChannelsHandler(api));
        routes.get("api/react/message/logs", new MessageLogsHandler(deletedMessagesDao, api));

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

    //#region User Cache
    public UserinfoConfig getUserConfig(String id) {
        return ucCache.get(id, this::get);
    }

    private UserinfoConfig get(String id) {
        UserinfoConfig uc = new UserinfoConfig(id);
        User u = api.retrieveUserById(id).complete();
        Member mem = guild.retrieveMemberById(id).complete();
        if (mem != null) {
            if (mem.getNickname() != null) uc.setMcNick(mem.getNickname());
        }
        uc.setFullname(UserUtil.getName(u));
        ucCache.put(id, uc);
        return uc;
    }
    //#endregion User Cache

    @Nullable
    public DiscordInviteConfig getDiscordConfig(String nick) {
        return dcCache.getIfPresent(nick);
    }

    //#region Discord Cache
    public void putDiscordConfig(String nick, String kod, String ranga) {
        DiscordInviteConfig dc = new DiscordInviteConfig(nick);
        dc.setKod(kod);
        dc.setRanga(ranga);
        dcCache.put(kod, dc);
    }

    //#endregion Discord Cache

    //#region ChatMod Cache

    public void refreshChatmod() {
        Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
        if (g == null) {
            Log.newError("Serwer docelowy jest nullem", APIModule.class);
            return;
        }
        Role role = g.getRoleById(Ustawienia.instance.roles.chatMod);
        if (role == null) {
            Log.newError("Rola chatModa jest nullem!", APIModule.class);
            return;
        }
        g.loadMembers().onSuccess((mem) -> mem.stream().filter(m -> m.getRoles().contains(role)).forEach(this::putChatModUser));
    }

    public Map<String, ChatModUser> getChatModUsers() {
        return cdCache.asMap();
    }

    private void putChatModUser(Member mem) {
        String nick = UserUtil.getMcNick(mem, true);
        String prefix;
        if (nick.contains("#")) {
            prefix = "Brak zmienionego nicku";
        } else {
            try {
                prefix = Objects.requireNonNull(mem.getNickname()).split(" ")[0];
            } catch (Exception e) {
                prefix = "Błąd przy pobieraniu nicku!";
            }
        }
        ChatModUser cmd = new ChatModUser(nick, prefix, mem.getUser().getAvatarUrl(), mem.getUser().getName(), mem.getUser().getDiscriminator(), mem.getId());
        cdCache.put(mem.getId(), cmd);
    }

    @Data
    @AllArgsConstructor
    public static class ChatModUser {
        private final String nick;
        private final String prefix;
        private final String avatar;
        private final String username;
        private final String tag;
        private final String id;
    }
    //#endregion ChatMod Cache

}