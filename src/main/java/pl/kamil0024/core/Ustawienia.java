package pl.kamil0024.core;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.List;

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
    }

    public static class Channel {
        public String modlog = def;
        public String nieobecnosci = def;
        public String status = def;
        public String liczek = def;
        public String wiadomosci = def;
        public String moddc = def;
    }

    public static class Inne {
        public String kategoriaArchiwum = def;
    }

}
