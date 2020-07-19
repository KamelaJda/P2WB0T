package pl.kamil0024.core.command;

import lombok.Getter;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.arguments.ArgumentManager;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.UserDao;
import pl.kamil0024.core.database.config.UserConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.Error;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.WebhookUtil;

import java.time.Instant;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommandExecute extends ListenerAdapter {
    ArgumentManager argumentManager;
    CommandManager commandManager;
    Tlumaczenia tlumaczenia;
    UserDao userDao;

    @Getter HashMap<String, UserConfig> userConfig;

    private final Map<String, Instant> cooldowns = new HashMap<>();

    public CommandExecute(CommandManager commandManager, Tlumaczenia tlumaczenia, ArgumentManager argumentManager, UserDao userDao) {
        this.commandManager = commandManager;
        this.tlumaczenia = tlumaczenia;
        this.argumentManager = argumentManager;
        this.userDao = userDao;
        reloadConfig();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getAuthor().isBot() || e.getAuthor().isFake() || e.getMessage().isWebhookMessage() ||
                e.getMessage().getContentRaw().isEmpty()) return;
        if (!e.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        String prefix = Ustawienia.instance.prefix;

        UserConfig uc = userConfig.get(e.getAuthor().getId());
        if (uc != null) prefix = uc.getPrefix();

        String msg = e.getMessage().getContentRaw();
        String[] args = msg.split(" ");

        if (args.length == 0) return;
        if (!args[0].startsWith(prefix)) return;

        String cmd = args[0].replaceAll(prefix, "").toLowerCase();
        if (cmd.isEmpty()) return;

        Command c = commandManager.commands.get(cmd);

        if (c == null) {
            for (Map.Entry<String, Command> alias : commandManager.getAliases().entrySet()) {
                if (alias.getKey().toLowerCase().equals(cmd)) {
                    c = alias.getValue();
                    break;
                }
            }
        }

        if (c == null) { return; }

        PermLevel jegoPerm = UserUtil.getPermLevel(e.getAuthor());

        if (Ustawienia.instance.disabledCommand.contains(e.getChannel().getId())) {
            if (jegoPerm.getNumer() == PermLevel.MEMBER.getNumer()) {
                zareaguj(e.getMessage(), e.getAuthor(), false);
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        e.getMessage().clearReactions().complete();
                    } catch (Exception ignored) { }
                }).start();
                return;
            }
        }

        e.getChannel().sendTyping().queue();

        if (c.getPermLevel().getNumer() > jegoPerm.getNumer()) {
            String wymaga = tlumaczenia.get(c.getPermLevel().getTranlsateKey());
            String ma = tlumaczenia.get(jegoPerm.getTranlsateKey());

            e.getChannel().sendMessage(tlumaczenia.get("generic.noperm", wymaga, c.getPermLevel().getNumer(),
                    ma, jegoPerm.getNumer())).queue();

            zareaguj(e.getMessage(), e.getAuthor(), false);
            return;
        }

        if (haveCooldown(e.getAuthor(), c) != 0) {
            e.getChannel().sendMessage(tlumaczenia.get("generic.cooldown", haveCooldown(e.getAuthor(), c))).queue();
            zareaguj(e.getMessage(), e.getAuthor(), false);
            return;
        }

        boolean udaloSie = false;
        HashMap<Integer, String> parsedArgs = new HashMap<>();
        int jest = 0;
        for (int i = 1; i < args.length; i++) {
            if (!args[i].isEmpty()) {
                parsedArgs.put(jest, args[i]);
                jest++;
            }
        }

        CommandContext cmdc = new CommandContext(e, prefix, parsedArgs, tlumaczenia, argumentManager, c);
        try {
            if (c.execute(cmdc)) udaloSie = true;
        } catch (UsageException u) {
            Error.usageError(cmdc);
        } catch (Exception omegalul) {
            omegalul.printStackTrace();
            Log.newError("`%s` uzyl komendy %s (%s) ale wystapil blad: %s", e.getAuthor().getName(), c.getName(), c.getClass().getName(), omegalul);
            Log.newError(omegalul);
            e.getChannel().sendMessage(String.format("Wystąpił błąd! `%s`.", omegalul)).queue();
        }
        if (udaloSie && jegoPerm.getNumer() < 10) setCooldown(e.getAuthor(), c);
        zareaguj(e.getMessage(), e.getAuthor(), udaloSie);

        try {
            onExecuteEvent(cmdc);
        } catch (Exception ignored) {}
    }

    public static Emote getReaction(User user, boolean bol) {
        try {
            Guild g = user.getJDA().getGuildById(Ustawienia.instance.bot.guildId);
            if (g == null) throw new IllegalArgumentException("guild == null");

            Emote em = g.getEmoteById(Ustawienia.instance.emote.green);
            if (!bol) em = g.getEmoteById(Ustawienia.instance.emote.red);

            if (em == null) throw new IllegalArgumentException("emote == null");
            return em;
        } catch (Exception ignored) {}
        return null;
    }

    private static void zareaguj(Message msg, User user, boolean bol) {
        try {
            try {
                msg.addReaction(getReaction(user, bol)).complete();
            } catch (ErrorResponseException ignored) { }
        } catch (Exception ignored) {}
    }

    private void setCooldown(User user, Command command) {
        Calendar cal = Calendar.getInstance();
        if (command.getCooldown() == 0) return;
        cal.add(Calendar.SECOND, command.getCooldown());
        cooldowns.put(user.getId() + command.getName(), cal.toInstant());
    }

    private int haveCooldown(User user, Command command) {
        if (command.getCooldown() == 0) return 0;

        Instant cooldown = cooldowns.getOrDefault(user.getId() + command.getName(), Instant.now());
        long teraz = Instant.now().toEpochMilli();

        if (cooldown.toEpochMilli() - teraz > 0) {
            return (int) TimeUnit.SECONDS.convert(cooldown.toEpochMilli() - teraz, TimeUnit.MILLISECONDS);
        }
        return 0;
    }

    public void onExecuteEvent(@Nullable CommandContext context) {
        if (context == null) return;
        String msg = "`%s` użył komendy %s(%s) na serwerze %s[%s]";
        StringBuilder b = new StringBuilder(" ");

        for (Map.Entry<Integer, String> m : context.getArgs().entrySet()) { b.append(m.getValue()).append(",");}

        msg = String.format(msg, UserUtil.getLogName(context.getUser()),
                context.getCommand(), b.toString().replaceAll("@", "@\u200b$1"), context.getGuild(), context.getGuild().getId());

        WebhookUtil web = new WebhookUtil();
        web.setMessage(msg);
        web.setType(WebhookUtil.LogType.CMD);
        web.send();
        Log.debug(msg);
    }

    public void reloadConfig() {
        this.userConfig = new HashMap<>();
        for (UserConfig userConfig : userDao.getAll()) {
            getUserConfig().put(userConfig.getId(), userConfig);
        }
    }

}
