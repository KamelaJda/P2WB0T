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

import com.google.gson.annotations.SerializedName;

import java.util.*;

@SuppressWarnings("ALL")
public class Ustawienia {
    @SuppressWarnings("squid:S1444")
    public static Ustawienia instance;
    public static String def = "12345";

    public String prefix = "k!";
    public String language = "pl";
    public String muteRole = def;
    public List<String> devs = Arrays.asList("343467373417857025");
    public List<String> disabledCommand = Arrays.asList("647904617811804202");

    public Roles roles = new Roles();
    public Bot bot = new Bot();
    public Games games = new Games();
    public Emote emote = new Emote();
    public PostgresSettings postgres = new PostgresSettings();
    public WebhookConfig webhook = new WebhookConfig();
    public Channel channel = new Channel();
    public Inne inne = new Inne();
    public Api api = new Api();
    public Rangi rangi = new Rangi();
    public YouTrack yt = new YouTrack();
    public SpotifyCredentials spotify = new SpotifyCredentials();
    public Osu osu = new Osu();
    public Ticket ticket = new Ticket();
    public Apelacje apelacje = new Apelacje();
    public Rekrutacyjny rekrutacyjny = new Rekrutacyjny();
    public Zespoly zespoly = new Zespoly();
    public Sentry sentry = new Sentry();

    public static class Roles {
        public String helperRole = def;
        public String moderatorRole = def;
        public String adminRole = def;
        public String chatMod = def;
    }

    public static class Bot {
        public String botId = def;
        public String guildId = def;
    }

    public static class Games {
        public List<String> games = Arrays.asList("v{VERSION}", "{USERS:ALL} użytkowników", "{PREFIX}help");
    }

    public static class Emote {
        public String green = def;
        public String red = def;
        public String load = def;
    }

    public static class PostgresSettings {
        @SerializedName("jdbc-url")
        public String jdbcUrl = "jdbc:postgresql://localhost/test";
        public String user = "postgres";
        public String password = def;
    }

    public static class WebhookConfig {
        public String error = "https://discordapp.com/api/webhooks/";
        public String cmd = "https://discordapp.com/api/webhooks/";
        public String status = "https://discordapp.com/api/webhooks/";
        public String debug = "https://discordapp.com/api/webhooks/";
        public String cases = "https://discordapp.com/api/webhooks/";
    }

    public static class Channel {
        public String modlog = def;
        public String nieobecnosci = def;
        public String status = def;
        public String liczek = def;
        public String wiadomosci = def;
        public String moddc = def;
        public String loginieobecnosci = def;
        public String logidowodow = def;
    }

    public static class Inne {
        public String kategoriaArchiwum = def;
    }

    public static class Api {
        public Integer port;
        public List<String> tokens = Arrays.asList("343467373417857025");
        public List<String> whitelist = Arrays.asList("343467373417857025");
    }

    public static class Rangi {
        public String gracz = def;
        public String vip = def;
        public String vipplus = def;
        public String mvp = def;
        public String mvpplus = def;
        public String mvpplusplus = def;
        public String sponsor = def;
        public String miniyt = def;
        public String yt = def;
        public String ekipa = def;
        public String pomocnik = def;
        public String buildteam = def;
        public String stazysta = def;
        public String moderator = def;
        public String administrator = def;
        public String korona = def;
        public String moderatorforum;
    }

    public static class YouTrack {
        public String url = def;
        public String hub = def;
        public String ytId = def;
        public String clientSecret = def;
        public String clientScope = def;
    }

    public static class SpotifyCredentials {
        public String id;
        public String secret;
    }

    public static class Osu {
        public String apiKey = def;
        public String osuTD = "123";
        public String osu9K = "123";
        public String osuCOOPK = "123";
        public String osu1K = "123";
        public String osu3K = "123";
        public String osu2K = "123";
        public String osuV2 = "123";
        public String osuLM = "123";
        public String osuRD = "754088095812419607";
        public String osuFI = "754087897157861476";
        public String osuPF = "754087670774497321";
        public String osuAO = "754087407858745426";
        public String osuNC = "754087284705460225";
        public String osuHT = "754087136470499359";
        public String osuDT = "754086993469636668";
        public String osuHD = "754086756953096252";
        public String osuAP = "754086524882124820";
        public String osuCN = "754085931304091690";
        public String osu300 = "754070379135369266";
        public String osu100 = "754070669141999856";
        public String osu50 = "754071182390722591";
        public String osumiss = "754085080045191269";
        public String osukatu = "754072060547956887";
        public String osugeki = "754071625904816349";
        public String osuSS = "754072936809365544";
        public String osuSSH = "754072596659699802";
        public String osuS = "754074220140691696";
        public String osuSH = "754072596659699802";
        public String osuA = "754074551134060554";
        public String osuB = "754072325875564661";
        public String osuC = "754077064960802867";
        public String osuD = "754080563765444828";
        public String osuNF = "754083487908364502";
        public String osuEZ = "754076116918206475";
        public String osuHR = "754075313218125944";
        public String osuSD = "754082760058077335";
        public String osuRX = "754083953836687360";
        public String osuFL = "754082584765530192";
        public String osuSO = "754083148262015168";
        public String osuTP = "754082949535760496";
        public String osu4K = "754081129757278298";
        public String osu5K = "754081308728230110";
        public String osu6K = "754081407231459378";
        public String osu7K = "754081524294615081";
        public String osu8K = "754081658457817220";
    }

    public static class Ticket {
        public String createChannelCategory = def;
        public String vcToCreate = def;
        public String notificationChannel = def;
        public String strefaPomocy = def;
    }

    public static class Apelacje {
        public Map<String, List<Integer>> dni = new HashMap<>();
    }

    public static class Rekrutacyjny {
        public String guildId;
        public String ogloszeniaId;
        public String nieobecnosciId;
        public String ankietyId;

        public String admin = def;
        public String mod = def;
        public String pom = def;
        public String staz = def;
        public String chatmod = def;
        public String ekipa = def;
    }

    public static class Zespoly {
        public List<String> zespoly = new ArrayList<>();
        public Map<String, List<String>> liderzy = new HashMap<>();
    }

    public static class Sentry {
        public String dns = def;
    }

}
