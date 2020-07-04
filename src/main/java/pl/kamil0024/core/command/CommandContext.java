package pl.kamil0024.core.command;

import com.google.errorprone.annotations.CheckReturnValue;
import com.google.gson.Gson;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.arguments.Args;
import pl.kamil0024.core.arguments.ArgumentManager;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings({"unused", "UnusedReturnValue", "StringBufferMayBeStringBuilder"})
public class CommandContext {

    @Getter private final GuildMessageReceivedEvent event;
    @Getter private final String prefix;
    @Getter private final HashMap<Integer, String> args;

    private final ArgumentManager argumentManager;
    private final Command cmd;
    private Tlumaczenia tlumaczenia;

    private static final Pattern URLPATTERN = Pattern.compile("(https?://(?:www\\.|(?!www))[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\." +
            "[^\\s]{2,}|www\\.[a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9]\\.[^\\s]{2,}|https?://(?:www\\.|(?!www))[a-zA-Z0-9]" +
            "\\.[^\\s]{2,}|www\\.[a-zA-Z0-9]\\.[^\\s]{2,})");

    public CommandContext(GuildMessageReceivedEvent event, String prefix, @Nullable HashMap<Integer, String> args, Tlumaczenia tlumaczenia, ArgumentManager argumentManager, Command cmd) {
        this.event = event;
        this.prefix = prefix;
        this.args = args;
        this.tlumaczenia = tlumaczenia;
        this.argumentManager = argumentManager;
        this.cmd = cmd;
    }

    public Command getCommand() {
        return cmd;
    }

    @Nullable
    public Object getParsedArgument(String argument, String value, CommandContext context) {
        Args a = argumentManager.getArgument(argument);
        if (a == null) throw new IllegalArgumentException("Nie ma takiego argumentu jak " + argument);
        try {
            return a.parsed(value, event.getJDA(), context);
        } catch (Exception e) {
            throw new UnsupportedOperationException("Przy uzyskiwaniu parseda od argumenta " + a.getName() + " wystapil blad!");
        }
    }

    public User getSender() {
        return event.getAuthor();
    }

    public User getUser() {
        return getSender();
    }

    public Member getMember() {
        return event.getMember();
    }

    public SelfUser getBot() {
        return event.getJDA().getSelfUser();
    }

    public Message getMessage() {
        return event.getMessage();
    }

    public TextChannel getChannel() {
        return event.getChannel();
    }

    public Guild getGuild() {
        return event.getGuild();
    }

    public MessageAction send(String msg) {
        return send(msg, true);
    }

    public MessageAction send(CharSequence msg, boolean checkUrl) {
        String message = String.valueOf(msg);
        if (checkUrl && URLPATTERN.matcher(msg).matches()) {
            message = message.replaceAll(String.valueOf(URLPATTERN), "[LINK]");
        }
        return event.getChannel().sendMessage(message.replaceAll("@(everyone|here)", "@\u200b$1"));
    }

    public MessageAction send(MessageEmbed message) {
        return event.getChannel().sendMessage(message);
    }

    public MessageAction sendTranslate(String key, Object... obj) {
        return send(getTranslate(key, obj));
    }

    public MessageAction sendTranslate(String key) {
        return send(getTranslate(key));
    }

    @CheckReturnValue
    public String getTranslate(String msg) {
        return tlumaczenia.get(msg);
    }

    @CheckReturnValue
    public String getTranslate(String key, String... argi) {
        return tlumaczenia.get(key, argi);
    }

    @CheckReturnValue
    public String getTranslate(String key, Object... argi) {
        ArrayList<String> parsedArgi = new ArrayList<>();
        for (Object arg : argi) {
            parsedArgi.add(arg.toString());
        }
        return tlumaczenia.get(key, parsedArgi.toArray(new String[]{}));
    }

    @Nullable
    public String getArgsToString(Integer num) {
        StringBuffer args = new StringBuffer();
        for (Map.Entry<Integer, String> a : getArgs().entrySet()) {
            if (a.getKey() >= num) {
                args.append(a.getValue()).append(" ");
            }
        }
        if (args.toString().isEmpty()) return null;
        return args.toString();
    }

    public ParsedArgumenty getParsed() {
        return new ParsedArgumenty(this);
    }

    public JDA getJDA() {
        return event.getJDA();
    }

    public ShardManager getShardManager() {
        return event.getJDA().getShardManager();
    }

    @Override
    public String toString() {
        return "user=" + UserUtil.getName(getUser()) + " " +
                "msg=" + getMessage().getContentRaw() + " " +
                "args=" + new Gson().toJson(getArgs());
    }

    @SuppressWarnings("InnerClassMayBeStatic")
    public class ParsedArgumenty {

        private final CommandContext context;

        public ParsedArgumenty(CommandContext context) {
            this.context = context;
        }

        @Nullable
        public User getUser(String user) {
            return (User) context.getParsedArgument("user", user, context);
        }

        @Nullable
        public Member getMember(String member) {
            return (Member) context.getParsedArgument("member", member, context);
        }

        @Nullable
        public TextChannel getTextChannel(@Nullable String channel) {
            if (channel == null) return null;
            return (TextChannel) context.getParsedArgument("textchannel", channel, context);
        }

        @Nullable
        public Integer getNumber(String num) {
            Integer n = null;
            try {
                n = Integer.parseInt(num);
            } catch (NumberFormatException ignored) { }
            return n;
        }

        @Nullable
        public Double getDouble(String num) {
            Double n = null;
            try {
                n = Double.parseDouble(num);
            } catch (Exception ignored) { }
            return n;
        }

    }
}