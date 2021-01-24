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
import pl.kamil0024.api.redisstats.RedisStatsManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.*;
import pl.kamil0024.core.database.config.DiscordInviteConfig;
import pl.kamil0024.core.database.config.UserinfoConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.musicapi.MusicAPI;
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
@SuppressWarnings("DanglingJavadoc")
public class APIModule implements Modul {

    private final VoiceStateDao voiceStateDao;
    private final MusicAPI musicAPI;
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

    private final Cache<UserinfoConfig> ucCache;
    private final Cache<DiscordInviteConfig> dcCache;
    private final Cache<ChatModUser> cdCache;

    private final Guild guild;

    private final ScheduledExecutorService executorSche;

    public APIModule(ShardManager api, CaseDao caseDao, RedisManager redisManager, NieobecnosciDao nieobecnosciDao, StatsDao statsDao, MusicAPI musicAPI, VoiceStateDao voiceStateDao, TicketDao ticketDao, ApelacjeDao apelacjeDao, AnkietaDao ankietaDao, EmbedRedisManager embedRedisManager, AcBanDao acBanDao, RecordingDao recordingDao) {
        this.api = api;
        this.redisManager = redisManager;
        this.guild = api.getGuildById(Ustawienia.instance.bot.guildId);
        this.musicAPI = musicAPI;
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

        this.ucCache = redisManager.new CacheRetriever<UserinfoConfig>(){}.getCache(3600);
        this.dcCache = redisManager.new CacheRetriever<DiscordInviteConfig>() {}.getCache(3600);
        this.cdCache = redisManager.new CacheRetriever<ChatModUser>() {}.getCache(3600);

        executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(this::refreshChatmod, 0, 30, TimeUnit.MINUTES);
    }

    @Override
    public boolean startUp() {
//        RedisStatsManager redisStatsManager = new RedisStatsManager(redisManager, caseDao, api);

        RoutingHandler routes = new RoutingHandler();

        /**
         * @api {get} api/checkToken/:token Sprawdza token
         * @apiName checkToken
         * @apiDescription Używane do sprawdzenia prawidłowości tokenów
         * @apiGroup Token
         * @apiVersion 1.0.0
         * @apiParam {Token} token Token
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         * @apiSuccess {String} msg Wiadomość potwierdzająca
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "msg": "Token jest dobry"
         *     }
         * @apiErrorExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": false,
         *         "error": {
         *             "body": "Zły token",
         *             "description": "Token jest pusty?"
         *         }
         *     }
         *
         */
        routes.get("/api/checkToken/{token}", new CheckToken());

        /**
         * @api {get} api/karainfo/:token/:id Informacje o karze
         * @apiName karainfo
         * @apiDescription Wyświetla informacje o karze poprzez ID
         * @apiGroup Kary
         * @apiVersion 1.0.0
         * @apiParam {Number} id ID Kary
         * @apiParam {String} token Token
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         * @apiSuccess {Kara} data Odpowiedź w postaci kary
         * @apiSuccess {String} id ID kary
         * @apiSuccess {Object} kara Kara
         * @apiSuccess {Number} kara.karaId ID kary
         * @apiSuccess {String} kara.karanyId Karany użytkownik
         * @apiSuccess {String} kara.mcNick Nick, który gracz miał ustawiony, gdy dostawał karę
         * @apiSuccess {String} kara.admId Nick administratora
         * @apiSuccess {Number} kara.timestamp Czas nadania kary
         * @apiSuccess {String} kara.typKary Typ kary (KICK, BAN, MUTE, TEMPBAN, TEMPMUTE, UNMUTE, UNBAN)
         * @apiSuccess {Boolean} kara.aktywna Czy kara jest aktywna
         * @apiSuccess {String} kara.messageUrl Link do wiadomości, która została napisana na kanale #logi
         * @apiSuccess {Boolean} kara.punAktywna Czy aktywna jako punish (raczej bez użycia to jest)
         * @apiSuccess {Number} kara.end Czas zakończenie kary (tylko przy karze TEMPBAN, TEMPMUTE)
         * @apiSuccess {Number} kara.duration Na jaki czas nadano
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": {
         *             "id": "600",
         *             "kara": {
         *                 "karaId": 600,
         *                 "karanyId": "[VIP] gracz123 (lub gracz#1234 jeżeli nie ma go na serwerze)",
         *                 "mcNick": "gracz123",
         *                 "admId": "[POM] KAMIL0024 (lub KAMIL0024#1234 jeżeli nie ma go na serwerze)",
         *                 "powod": "Omijanie bana",
         *                 "timestamp": 1595536961248,
         *                 "typKary": "BAN",
         *                 "aktywna": true,
         *                 "messageUrl": "https://discordapp.com/channels/1234/1234/1234",
         *                 "punAktywna": false
         *             }
         *         }
         *     }
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": {
         *             "id": "678",
         *             "kara": {
         *                 "karaId": 678,
         *                 "karanyId": "[VIP] gracz123 (lub gracz#1234 jeżeli nie ma go na serwerze)"",
         *                 "mcNick": "gracz123",
         *                 "admId": "[POM] KAMIL0024 (lub KAMIL0024#1234 jeżeli nie ma go na serwerze)",
         *                 "powod": "Powód",
         *                 "timestamp": 1595685472444,
         *                 "typKary": "TEMPBAN",
         *                 "aktywna": true,"
         *                 "messageUrl": "422016694408577025/533703342195605523/736583005367435294",
         *                 "end": 1595696272444,
         *                 "duration": "3h",
         *                 "punAktywna": true
         *             }
         *         }
         *     }
         *
         *
         * @apiErrorExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": false,
         *         "error": {
         *             "body": "Złe ID",
         *             "description": "ID kary jest puste lub nie jest liczbą"
         *          }
         *      }
         *
         */
        routes.get("/api/karainfo/{token}/{id}", new Karainfo(caseDao, this));

        /**
         * @api {get} api/listakar/:token/:nick Informacje o karach
         * @apiName listakar
         * @apiDescription Wyświetla informacje o karze poprzez nick użytkownika
         * @apiGroup Kary
         * @apiVersion 1.0.0
         * @apiParam {String} nick Nick gracza
         * @apiParam {Token} token Token
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         * @apiSuccess {Kara} data Odpowiedź w postaci kary
         * @apiSuccess {String} id ID kary
         * @apiSuccess {Kara} data Data w postaci listy kary
         * @apiSuccess {Number} data.karaId ID kary
         * @apiSuccess {String} data.karanyId Karany użytkownik
         * @apiSuccess {String} data.mcNick Nick, który gracz miał ustawiony, gdy dostawał karę
         * @apiSuccess {String} data.admId Nick administratora
         * @apiSuccess {Number} data.timestamp Czas nadania kary
         * @apiSuccess {String} data.typKary Typ kary (KICK, BAN, MUTE, TEMPBAN, TEMPMUTE, UNMUTE, UNBAN)
         * @apiSuccess {Boolean} data.aktywna Czy kara jest aktywna
         * @apiSuccess {String} data.messageUrl Link do wiadomości, która została napisana na kanale #logi
         * @apiSuccess {Boolean} data.punAktywna Czy aktywna jako punish (raczej bez użycia to jest)
         * @apiSuccess {Number} data.end Czas zakończenie kary (tylko przy karze TEMPBAN, TEMPMUTE)
         * @apiSuccess {Number} data.duration Na jaki czas nadano
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *          "success": true,
         *          "data": [
         *             {
         *                  "id": "680",
         *                  "kara": {
         *                       "karaId": 680,
         *                       "karanyId": "niezesrajsie_",
         *                       "mcNick": "niezesrajsie_",
         *                       "admId": "[POM] matc2002",
         *                       "powod": "Nadmierny spam",
         *                       "timestamp": 1595685781024,
         *                       "typKary": "TEMPMUTE",
         *                       "aktywna": false,
         *                       "messageUrl":"https://discordapp.com/channels/422016694408577025/533703342195605523/736584298823417909",
         *                       "end": 1595689381024,
         *                       "duration": "1h",
         *                       "punAktywna": true
         *                 }
         *             },
         *             {
         *                  "id": "680",
         *                  "kara": {
         *                       "karaId": 680,
         *                       "karanyId": "niezesrajsie_",
         *                       "mcNick": "niezesrajsie_",
         *                       "admId": "[POM] matc2002",
         *                       "powod": "Nadmierny spam",
         *                       "timestamp": 1595685781024,
         *                       "typKary": "TEMPMUTE",
         *                       "aktywna": false,
         *                       "messageUrl":"https://discordapp.com/channels/422016694408577025/533703342195605523/736584298823417909",
         *                       "end": 1595689381024,
         *                       "duration": "1h",
         *                       "punAktywna": true
         *                 }
         *             }
         *         ]
         *     }
         *
         *
         * @apiErrorExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": false,
         *         "error": {
         *             "body": "Zły nick",
         *             "description": "Ten nick nie ma żadnej kary"
         *          }
         *      }
         *
         */
        routes.get("/api/listakar/{token}/{nick}", new Listakar(caseDao, this));

        /**
         * @api {get} api/nieobecnosci/{token}/{nick} Lista nieobecności - Nick
         * @apiName nieobecnosci.nick
         * @apiDescription Wyświetla liste branych nieobecności na podstawie nicku
         * @apiGroup Nieobecności
         * @apiVersion 1.0.0
         * @apiParam {String} nick Nick gracza
         * @apiParam {Token} token Token
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         *
         * @apiSuccess {Nieobecnosc} data Odpowiedź w postaci listy nieobecnosci
         * @apiSuccess {String} data.userId Nick administatora
         * @apiSuccess {Number} data.id ID nieobecności adminisatora
         * @apiSuccess {String} data.msgId Link do wiadomości na #nieobecnosci
         * @apiSuccess {Number} data.start Data rozpoczęcia
         * @apiSuccess {String} data.powod Czas nadania kary
         * @apiSuccess {Number} data.end Data zakończenia
         * @apiSuccess {Boolean} data.aktywna Czy nieobecność jest aktywna
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": [
         *             {
         *                 "userId": "[POM] adm1",
         *                 "id":1,
         *                 "msgId":"https://discordapp.com/channels/422016694408577025/687775040065896495/734660241878155276",
         *                 "start": 1595196000000,
         *                 "powod": "Powód",
         *                 "end": 1595800800000,
         *                 "aktywna" :true
         *             },
         *             {
         *                 "userId": "[POM] adm2",
         *                 "id":1,
         *                 "msgId":"https://discordapp.com/channels/422016694408577025/687775040065896495/734660241878155276",
         *                 "start": 1595196000000,
         *                 "powod": "Powód2",
         *                 "end": 1595800800000,
         *                 "aktywna" :true
         *             }
         *         ]
         *     }
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         */

        /**
         * @api {get} api/nieobecnosci/{token}/all Lista wszystkich nieobecności
         * @apiName nieobecnosci.all
         * @apiDescription Wyświetla liste wszystkich nieobecności
         * @apiGroup Nieobecności
         * @apiVersion 1.0.0
         * @apiParam {Token} token Token
         *
         * @apiSuccess {String} success Czy zapytanie się udało
         *
         * @apiSuccess {Object} data Odpowiedź
         * @apiSuccess {String} data.userId Nick administatora
         * @apiSuccess {Number} data.id ID nieobecności adminisatora
         * @apiSuccess {String} data.msgId Link do wiadomości na #nieobecnosci
         * @apiSuccess {Number} data.start Data rozpoczęcia
         * @apiSuccess {String} data.powod Czas nadania kary
         * @apiSuccess {Number} data.end Data zakończenia
         * @apiSuccess {Boolean} data.aktywna Czy nieobecność jest aktywna
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": {
         *             "[MOD] adm1": [
         *                 {
         *                     "userId": "[MOD] adm1",
         *                     "id": 1,
         *                     "msgId": "https://discordapp.com/channels/422016694408577025/687775040065896495/735598733173063750",
         *                     "start": 1595455200000,
         *                     "powod": "Powód.",
         *                     "end": 1598133600000,
         *                     "aktywna": true
         *                 },
         *                 {...}
         *             ],
         *             "[POM] adm2": [
         *                 {
         *                     "userId": "[POM] adm2",
         *                     "id": 1,
         *                     "msgId": "https://discordapp.com/channels/422016694408577025/687775040065896495/734660241878155276",
         *                     "start": 1595196000000,
         *                     "powod": "Powód",
         *                     "end": 1595800800000,
         *                     "aktywna": true
         *                 },
         *                 {...}
         *             ]
         *         }
         *     }
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         */

        /**
         * @api {get} api/nieobecnosci/{token}/aktywne Lista aktywnych nieobecności
         * @apiName nieobecnosci.aktywne
         * @apiDescription Wyświetla liste aktywnych nieobecności
         * @apiGroup Nieobecności
         * @apiVersion 1.0.0
         * @apiParam {Token} token Token
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         *
         * @apiSuccess {Nieobecnosc} data Lista nieobecności
         * @apiSuccess {String} data.userId Nick administratora
         * @apiSuccess {Number} data.id ID nieobecności administatora
         * @apiSuccess {String} data.msgId Link do wiadomości na #nieobecności
         * @apiSuccess {Number} data.start Data rozpoczęcia
         * @apiSuccess {String} data.powod Czas nadania kary
         * @apiSuccess {Number} data.end Data zakończenia
         * @apiSuccess {Boolean} data.aktywna Czy nieobecność jest aktywna
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": [
         *             {
         *                 "userId": "[POM] adm1",
         *                 "id":1,
         *                 "msgId": "https://discordapp.com/channels/422016694408577025/687775040065896495/734660241878155276",
         *                 "start": 1595196000000,
         *                 "powod": "Powód.",
         *                 "end": 1595800800000,
         *                 "aktywna": true
         *             },
         *             {
         *                 "userId": "[MOD] adm2",
         *                 "id":1,
         *                 "msgId": "https://discordapp.com/channels/422016694408577025/687775040065896495/735598733173063750",
         *                 "start": 1595455200000,
         *                 "powod": "Powód.",
         *                 "end": 1598133600000,
         *                 "aktywna" :true
         *             }
         *         ]
         *     }
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         */
        routes.get("api/nieobecnosci/{token}/{data}", new Nieobecnosci(nieobecnosciDao, this));

        /**
         * @api {get} api/stats/:token/:dni/:nick Statystyki z Discorda
         * @apiName stats
         * @apiDescription Wyświetla statystyki danego użytkownika na podstawie nicku. Gracz musi mieć rangę ChatMod. Wpisz w :dni wartość 0, jeżeli chcesz wyświetlać statystyki z dzisiaj.
         * @apiGroup ChatMod
         * @apiVersion 1.0.0
         * @apiParam {Token} token Token
         * @apiParam {Number} dni Liczba dni z których mają być brane statystyki. Np. 7 to statystyki sprzed tygodnia, a 30 to statystyki sprzed miesiąca.
         * @apiParam {String} nick Nick osoby
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         *
         * @apiSuccess {Object} data Odpowiedź
         * @apiSuccess {String} data.zmutowanych Liczba osób zmutowanych
         * @apiSuccess {Number} data.zbanowanych Liczba osób zmutowanych
         * @apiSuccess {String} data.wyrzuconych Liczba osób wyrzuconych
         * @apiSuccess {Number} data.usunietychWiadomosci Liczba usuniętych wiadomości
         * @apiSuccess {String} data.napisanychWiadomosci Liczba napisanych wiadomości
         * @apiSuccess {Number} data.day Wartość tutaj bezużyteczna
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "data": {
         *             "zmutowanych": 104,
         *             "zbanowanych": 17,
         *             "wyrzuconych": 0,
         *             "usunietychWiadomosci": 146,
         *             "napisanychWiadomosci": 742,
         *             "day": 0
         *         }
         *     }
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         */
        routes.get("api/stats/{token}/{dni}/{nick}", new StatsHandler(statsDao, this));
        routes.get("api/stats/{token}/{dni}/id/{nick}", new StatsHandler(statsDao, this, true));

        /**
         * @api {get} api/discord/:token/:nick/:ranga/:kod Weryfikacja
         * @apiName weryfikacja-discord
         * @apiDescription Weryfikacja serwer -> discord
         * @apiGroup Discord
         * @apiVersion 1.0.0
         * @apiParam {Token} token Token
         * @apiParam {String} nick Nick gracza
         * @apiParam {String} ranga Ranga gracza (gracz, vip, vipplus, mvp, mvpplus, mvpplusplus, sponsor, miniyt, yt)
         * @apiParam {String} kod Kod jaki gracz musi wpisać na #weryfikacja
         *
         * @apiSuccess {Boolean} success Czy zapytanie się udało
         * @apiSuccess {String} msg Odpowiedź do zapytania
         *
         * @apiSuccessExample {json}
         *     HTTP/1.1 200 OK
         *     {
         *         "success": true,
         *         "msg": "Zapytanie przebiegło pomyślnie"
         *     }
         *
         * @apiError {Boolean} success Czy zapytanie się udało
         * @apiError {Object} error Odpowiedź
         * @apiError {Boolean} error.body Krótka odpowiedź błędu
         * @apiError {Boolean} error.description Długa odpowiedź błędu
         */
        routes.get("api/discord/{token}/{nick}/{ranga}/{kod}", new DiscordInvite(this));

        //#region Music Bot api
        routes.get("api/musicbot/shutdown/{port}", new MusicBotHandler(musicAPI, false, voiceStateDao, api));
        routes.get("api/musicbot/connect/{port}/{clientid}", new MusicBotHandler(musicAPI, true, voiceStateDao, api));
        //#endregion Music Bot api

        routes.get("api/youtrack/reports", new YouTrackReport(api));

        routes.get("api/react/history/{token}/{id}/{offset}", new HistoryDescById(caseDao));
        routes.get("api/react/permlevel/{token}", new UserPermLevel(api));
        routes.get("api/react/chatmod/{token}/list", new ChatMod(api, this));
        routes.get("api/react/userinfo/{token}/{id}", new UserInfo(api));

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
