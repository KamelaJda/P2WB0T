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

package pl.kamil0024.musicbot.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.musicbot.api.APIModule;
import pl.kamil0024.musicbot.core.logger.Log;
import pl.kamil0024.musicbot.core.module.Modul;
import pl.kamil0024.musicbot.core.module.ModulManager;
import pl.kamil0024.musicbot.core.util.EventWaiter;
import pl.kamil0024.musicbot.core.util.Statyczne;
import pl.kamil0024.musicbot.core.util.Tlumaczenia;
import pl.kamil0024.musicbot.music.managers.MusicManager;
import pl.kamil0024.musicbot.socket.SocketClient;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class B0T {

    @Getter private HashMap<String, Modul> moduls;

    private static final File cfg = new File("config.json");

    private Ustawienia ustawienia;
    private Tlumaczenia tlumaczenia;
    private Thread shutdownThread;

    private ShardManager api;
    private ModulManager modulManager;

    public B0T(String token) {
        moduls = new HashMap<>();
        tlumaczenia = new Tlumaczenia();
        shutdownThread();

        Log.info("Loguje v%s", Statyczne.CORE_VERSION);
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        if (!cfg.exists()) {
            Log.error("Nie ma pliku konfiguracyjnego!");
            System.exit(1);
        }

        try {
            ustawienia = gson.fromJson(new FileReader(cfg), Ustawienia.class);
        } catch (Exception e) {
            Log.error("Nie mozna odczytac konfiguracji\n%s", e);
            System.exit(1);
        }

        Ustawienia.instance = ustawienia;
        EventWaiter eventWaiter = new EventWaiter();

        tlumaczenia.setLang("pl");
        tlumaczenia.load();

        this.api = null;
        try {
            DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token,
                    GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_VOICE_STATES);
            builder.setShardsTotal(1);
            builder.setShards(0, 0);
            builder.setEnableShutdownHook(false);
            builder.setAutoReconnect(true);
            builder.setStatus(OnlineStatus.OFFLINE);
            builder.addEventListeners(eventWaiter);
            builder.setBulkDeleteSplittingEnabled(false);
            builder.setCallbackPool(Executors.newFixedThreadPool(4));
            this.api = builder.build();
        } catch (LoginException e) {
            Log.error("Nie udalo sie zalogowac!");
            e.printStackTrace();
            System.exit(1);
        }

        while(api.getShards().stream().allMatch(s -> s.getStatus() != JDA.Status.CONNECTED)) {
            try {
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

        try {
            Thread.sleep(8000);
        } catch (InterruptedException ignored) {}

        this.modulManager = new ModulManager();



        MusicManager musicManager = new MusicManager(api);
        musicManager.socketClient.start();

        APIModule apiModule = new APIModule(api, musicManager, eventWaiter);

        modulManager.getModules().add(apiModule);

        for (Modul modul : modulManager.getModules()) {
            try {
                Log.debug(tlumaczenia.get("module.loading", modul.getName()));
                boolean bol = false;
                try {
                    bol = modul.startUp();
                    modul.setStart(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!bol) Log.error(tlumaczenia.get("module.loading.fail"));
                else Log.debug(tlumaczenia.get("module.loading.success", modul.getName()));

                moduls.put(modul.getName(), modul);
            } catch (Exception ignored) {
                Log.error(tlumaczenia.get("module.loading.fail"));
            }
        }


        api.setStatus(OnlineStatus.OFFLINE);
        Log.info("Zalogowano jako %s", shard.get().getSelfUser().getName());

        if (api.getGuildById(Ustawienia.instance.bot.guildId) == null) {
            api.shutdown();
            Log.newError("Nie ma bota na serwerze docelowym");
            System.exit(1);
        }

    }

    public void shutdownThread() {
        this.shutdownThread = new Thread(() -> {
            Log.info("Zamykam...");
            modulManager.disableAll();
            api.shutdown();
        });
        shutdownThread.setName("P2WB0T ShutDown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
    }


}
