/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.kamil0024.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import pl.kamil0024.api.APIModule;
import pl.kamil0024.chat.ChatModule;
import pl.kamil0024.commands.CommandsModule;
import pl.kamil0024.commands.ModLog;
import pl.kamil0024.commands.dews.RebootCommand;
import pl.kamil0024.core.arguments.ArgumentManager;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.database.*;
import pl.kamil0024.core.database.config.VoiceStateConfig;
import pl.kamil0024.core.listener.ExceptionListener;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.musicapi.MusicAPI;
import pl.kamil0024.core.musicapi.MusicResponse;
import pl.kamil0024.core.musicapi.MusicRestAction;
import pl.kamil0024.core.musicapi.impl.MusicAPIImpl;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.Statyczne;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.embedgenerator.EmbedGeneratorModule;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;
import pl.kamil0024.liczydlo.LiczydloModule;
import pl.kamil0024.logs.LogsModule;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.music.commands.privates.PrivateQueueCommand;
import pl.kamil0024.nieobecnosci.NieobecnosciManager;
import pl.kamil0024.nieobecnosci.NieobecnosciModule;
import pl.kamil0024.rekrutacyjny.RekruModule;
import pl.kamil0024.stats.StatsModule;
import pl.kamil0024.ticket.TicketModule;
import pl.kamil0024.weryfikacja.WeryfikacjaModule;
import pl.kamil0024.youtrack.YTModule;
import pl.kamil0024.youtrack.YouTrack;
import pl.kamil0024.youtrack.YouTrackBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.*;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;

import static pl.kamil0024.core.util.Statyczne.WERSJA;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal", "OptionalGetWithoutIsPresent"})
public class B0T {

    private static Logger logger = LoggerFactory.getLogger(B0T.class);

    @Getter private HashMap<String, Modul> moduls;

    @Inject private CommandManager commandManager;
    @Inject private ArgumentManager argumentManager;
    @Inject private DatabaseManager databaseManager;

    private static final File cfg = new File("config.json");

    private Ustawienia ustawienia;
    private Tlumaczenia tlumaczenia;
    private Thread shutdownThread;

    private StatsModule statsModule;
    private ShardManager api;
    private ModulManager modulManager;
    private MusicModule musicModule;
    private MusicAPI musicAPI;
    private VoiceStateDao voiceStateDao;

    @SneakyThrows
    public B0T(String token) {
        //#region fix self-assigne certs
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {  }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {  }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier allHostsValid = (hostname, session) -> true;
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        //#endregion fix self-assigne certs
        moduls = new HashMap<>();
        tlumaczenia = new Tlumaczenia();
        argumentManager = new ArgumentManager();
        commandManager = new CommandManager();

        argumentManager.registerAll();
        shutdownThread();

        logger.info("Loguje v{}", Statyczne.CORE_VERSION);
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        if (!cfg.exists()) {
            api.shutdown();
            Log.newError("Nie ma pliku konfiguracyjnego!", B0T.class);
            System.exit(1);
        }

        try {
            ustawienia = gson.fromJson(new FileReader(cfg), Ustawienia.class);
        } catch (Exception e) {
            logger.error("Nie mozna odczytac konfiguracji");
            System.exit(1);
        }

        Ustawienia.instance = ustawienia;
        EventWaiter eventWaiter;
        KaryJSON karyJSON;
        try {
            eventWaiter = new EventWaiter();
            karyJSON = new KaryJSON();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return; // super jestes idea
        }

        tlumaczenia.setLang(Ustawienia.instance.language);
        tlumaczenia.load();

        YouTrackBuilder ytbuilder = new YouTrackBuilder();
        ytbuilder.setApiUrl(Ustawienia.instance.yt.url);
        ytbuilder.setHubUrl(Ustawienia.instance.yt.hub);
        ytbuilder.setClientId(Ustawienia.instance.yt.ytId);
        ytbuilder.setClientSecret(Ustawienia.instance.yt.clientSecret);
        ytbuilder.setScope(Ustawienia.instance.yt.clientScope);

        YouTrack youTrack = null;
        try {
            youTrack = ytbuilder.build();
        } catch (Exception e) {
            Log.newError("Nie udało się połączyć z YouTrackiem!", B0T.class);
            e.printStackTrace();
        }

        this.api = null;
        try {
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_BANS, GatewayIntent.GUILD_VOICE_STATES,
                    GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_PRESENCES);
            builder.setShardsTotal(1);
            builder.setShards(0, 0);
            builder.setEnableShutdownHook(false);
            builder.setAutoReconnect(true);
            builder.setStatus(OnlineStatus.DO_NOT_DISTURB);
            builder.setActivity(Activity.playing(tlumaczenia.get("status.starting")));
            builder.addEventListeners(eventWaiter, new ExceptionListener());
            builder.setBulkDeleteSplittingEnabled(false);
            builder.setCallbackPool(Executors.newFixedThreadPool(4));
            builder.enableCache(CacheFlag.EMOTE, CacheFlag.ACTIVITY);
            builder.setMemberCachePolicy(MemberCachePolicy.ALL);
            MessageAction.setDefaultMentionRepliedUser(false);
            MessageAction.setDefaultMentions(EnumSet.of(Message.MentionType.EMOTE, Message.MentionType.CHANNEL));
            this.api = builder.build();
            api.getGatewayIntents();
        } catch (LoginException e) {
            logger.error("Nie udalo sie zalogowac!");
            e.printStackTrace();
            System.exit(1);
        }

        while(api.getShards().stream().allMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) {
            try {
                //noinspection BusyWait
                Thread.sleep(100);
            } catch (InterruptedException ignored) { }
        }

        Optional<JDA> shard = api.getShards().stream().filter(s -> {
            try {
                s.getSelfUser();
            } catch (IllegalStateException e) {
                return false;
            }
            return true;
        }).findAny();

        databaseManager = new DatabaseManager();
        databaseManager.load();

        try {
            Thread.sleep(8000);
        } catch (InterruptedException ignored) {}

        RedisManager     redisManager        = new RedisManager(shard.get().getSelfUser().getIdLong());
        EmbedRedisManager embedRedisManager  = new EmbedRedisManager(redisManager);
                         this.musicAPI       = new MusicAPIImpl(api);

        CaseDao          caseDao             = new CaseDao(databaseManager);
        UserDao          userDao             = new UserDao(databaseManager);
        NieobecnosciDao  nieobecnosciDao     = new NieobecnosciDao(databaseManager);
        RemindDao        remindDao           = new RemindDao(databaseManager);
        GiveawayDao      giveawayDao         = new GiveawayDao(databaseManager);
        StatsDao         statsDao            = new StatsDao(databaseManager);
                    this.voiceStateDao       = new VoiceStateDao(databaseManager);
        MultiDao         multiDao            = new MultiDao(databaseManager);
        TicketDao        ticketDao           = new TicketDao(databaseManager);
        ApelacjeDao      apelacjeDao         = new ApelacjeDao(databaseManager);
        AnkietaDao       ankietaDao          = new AnkietaDao(databaseManager, api);
        WeryfikacjaDao   weryfikacjaDao      = new WeryfikacjaDao(databaseManager);
        AcBanDao         acBanDao            = new AcBanDao(databaseManager);
        RecordingDao     recordingDao        = new RecordingDao(databaseManager);

        ArrayList<Object> listeners = new ArrayList<>();
        CommandExecute commandExecute = new CommandExecute(commandManager, tlumaczenia, argumentManager, userDao);
        listeners.add(commandExecute);
        listeners.forEach(api::addEventListener);

        this.modulManager = new ModulManager();
        ModLog modLog = new ModLog(api, caseDao);
        NieobecnosciManager nieobecnosciManager = new NieobecnosciManager(api, nieobecnosciDao);
        api.addEventListener(modLog);

        this.musicModule = new MusicModule(commandManager, api, eventWaiter, voiceStateDao, musicAPI);
        this.statsModule = new StatsModule(commandManager, api, eventWaiter, statsDao, musicModule, nieobecnosciDao);

        APIModule apiModule = new APIModule(api, caseDao, redisManager, nieobecnosciDao, statsDao, musicAPI, voiceStateDao, ticketDao, apelacjeDao, ankietaDao, embedRedisManager, acBanDao, recordingDao);
        WeryfikacjaModule weryfikacjaModule = new WeryfikacjaModule(apiModule, multiDao, modLog, caseDao, weryfikacjaDao);
        modulManager.getModules().add(new LogsModule(api, statsModule, redisManager));
        modulManager.getModules().add(new ChatModule(api, karyJSON, caseDao, modLog, statsModule));
//        modulManager.getModules().add(new StatusModule(api));
        modulManager.getModules().add(new NieobecnosciModule(api, nieobecnosciDao, nieobecnosciManager));
        modulManager.getModules().add(new LiczydloModule(api));
        modulManager.getModules().add(new CommandsModule(commandManager, tlumaczenia, api, eventWaiter, karyJSON, caseDao, modulManager, commandExecute, userDao, modLog, nieobecnosciDao, remindDao, giveawayDao, statsModule, musicModule, multiDao, musicAPI, nieobecnosciManager, youTrack, ticketDao, apelacjeDao, ankietaDao, embedRedisManager, weryfikacjaDao, weryfikacjaModule, recordingDao));
        modulManager.getModules().add(new RekruModule(api, commandManager));
        modulManager.getModules().add(musicModule);
        modulManager.getModules().add(statsModule);
        modulManager.getModules().add(apiModule);
        modulManager.getModules().add(new EmbedGeneratorModule(commandManager, embedRedisManager));
        modulManager.getModules().add(weryfikacjaModule);
        modulManager.getModules().add(new TicketModule(api, ticketDao, redisManager, eventWaiter));
        if (youTrack != null) modulManager.getModules().add(new YTModule(commandManager, api, eventWaiter, youTrack));

        for (Modul modul : modulManager.getModules()) {
            try {
                int commands = commandManager.getCommands().size();
                logger.debug(tlumaczenia.get("module.loading", modul.getName()));
                boolean bol = false;
                try {
                    bol = modul.startUp();
                    modul.setStart(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                commands = commandManager.getCommands().size() - commands;
                if (!bol) {
                    logger.error(tlumaczenia.get("module.loading.fail"));
                } else {
                    logger.debug(tlumaczenia.get("module.loading.success", modul.getName(), commands));
                }

                moduls.put(modul.getName(), modul);
            } catch (Exception ignored) {
                logger.error(tlumaczenia.get("module.loading.fail"));
            }
        }

        api.setStatus(OnlineStatus.ONLINE);
        api.setActivity(Activity.playing(tlumaczenia.get("status.hi", WERSJA)));
        logger.info("Zalogowano jako {}", shard.get().getSelfUser());

        if (api.getGuildById(Ustawienia.instance.bot.guildId) == null) {
            api.shutdown();
            Log.newError("Nie ma bota na serwerze docelowym", B0T.class);
            System.exit(1);
        }
    }

    public void shutdownThread() {
        this.shutdownThread = new Thread(() -> {
            logger.info("Zamykam...");
            RebootCommand.reboot = true;
            api.setStatus(OnlineStatus.DO_NOT_DISTURB);
            api.setActivity(Activity.playing("Wyłącznie bota w toku..."));

            loadMusic();
            musicAPI.getPorts().forEach(port -> {
                try {
                    musicAPI.getAction(port).shutdown();
                } catch (IOException ignored) { }
            });

            musicModule.load();
            modulManager.disableAll();
            statsModule.getStatsCache().databaseSave();
        });
        shutdownThread.setName("P2WB0T ShutDown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }

    public void loadMusic() {
        try {
            Guild g = api.getGuildById(Ustawienia.instance.bot.guildId);
            if (g == null) return;

            for (Integer port : musicAPI.getPorts()) {
                MusicRestAction action = musicAPI.getAction(port);
                VoiceStateConfig vsc = new VoiceStateConfig(musicAPI.getClientByPort(port));

                VoiceChannel vc = action.getVoiceChannel();
                if (vc == null) continue;
                vsc.setVoiceChannel(vc.getId());

                MusicResponse queue = action.getQueue();
                if (queue.isError()) continue;

                ArrayList<String> kurwa = new ArrayList<>();
                for (Object o : queue.json.getJSONArray("data")) {
                    PrivateQueueCommand.Track trak = new Gson()
                            .fromJson(o.toString(), PrivateQueueCommand.Track.class);
                    kurwa.add(trak.getIdentifier());
                }
                vsc.setQueue(kurwa);
                MusicResponse mr = action.getPlayingTrack();
                PrivateQueueCommand.Track trak = new Gson()
                        .fromJson(mr.json.getJSONObject("data").toString(), PrivateQueueCommand.Track.class);
                vsc.setAktualnaPiosenka(trak.getIdentifier());
                voiceStateDao.save(vsc);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
