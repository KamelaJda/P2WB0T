package pl.kamil0024.chat.listener;

import com.google.inject.Inject;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.chat.Action;
import pl.kamil0024.commands.ModLog;
import pl.kamil0024.commands.moderation.MuteCommand;
import pl.kamil0024.commands.moderation.PunishCommand;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.Emoji;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.stats.StatsModule;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener extends ListenerAdapter {

    private static final File FILE = new File("res/przeklenstwa.api");
    private static final Pattern HTTP = Pattern.compile("([0-9a-z_-]+\\.)+(com|infonet|net|org|pro|de|ggmc|md|me|tt|tv|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt)");
    private static final String DISCORD_INVITE = "(https?://)?(www\\.)?(discord\\.(gg|io|me|li)|discordapp\\.com/invite)/.+[a-z]";

    private static final Pattern EMOJI = Pattern.compile("<(a?):(\\w{2,32}):(\\d{17,19})>");

    @Inject private ShardManager api;
    @Inject private KaryJSON karyJSON;
    @Inject private CaseDao caseDao;
    @Inject private ModLog modLog;
    @Inject private StatsModule statsModule;

    @Getter private List<String> przeklenstwa;

    public ChatListener(ShardManager api, KaryJSON karyJSON, CaseDao caseDao, ModLog modLog, StatsModule statsModule) {
        this.api = api;
        this.karyJSON = karyJSON;
        this.modLog = modLog;
        this.caseDao = caseDao;
        this.statsModule = statsModule;

        this.przeklenstwa = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line;
            while ((line = br.readLine()) != null) { przeklenstwa.add(line); }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (przeklenstwa.isEmpty()) Log.newError("Lista przeklenstw jest nullem!");
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent e) {
        if (UserUtil.getPermLevel(e.getMember()).getNumer() >= PermLevel.HELPER.getNumer()) return;

        if (e.getAuthor().isBot() || e.getAuthor().isFake() || e.isWebhookMessage() || e.getMessage().getContentRaw().isEmpty()) return;
        if (e.getChannel().getId().equals("426809411378479105") || e.getChannel().getId().equals("503294063064121374")) return;

        checkMessage(e.getMember(), e.getMessage(), karyJSON, caseDao, modLog);
    }

    @Override
    public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent e) {
        if (UserUtil.getPermLevel(e.getAuthor()).getNumer() >= PermLevel.HELPER.getNumer()) return;
        if (e.getAuthor().isBot() || e.getAuthor().isFake() || e.getMessage().getContentRaw().isEmpty()) return;
        if (e.getChannel().getId().equals("426809411378479105") || e.getChannel().getId().equals("503294063064121374")) return;
        checkMessage(e.getMember(), e.getMessage(), karyJSON, caseDao, modLog);
    }

    public void checkMessage(Member member, Message msg, KaryJSON karyJSON, CaseDao caseDao, ModLog modLog) {
        if (MuteCommand.hasMute(member)) return;

        String msgRaw = msg.getContentRaw().replaceAll("<@!?([0-9])*>", "");
        Action action = new Action(karyJSON);
        action.setMsg(msg);

        String przeklenstwa = msgRaw;

        String[] tak = new String[] {"a;ą", "c;ć","e;ę", "l;ł", "n;ń", "o;ó", "s;ś", "z;ź", "z;ż"};
        for (String s : tak) {
            String[] kurwa = s.split(";");
            przeklenstwa = przeklenstwa.replaceAll(kurwa[1], kurwa[0]);
        }
        przeklenstwa = przeklenstwa.replaceAll("[^\\u0020\\u0030-\\u0039\\u0041-\\u005A\\u0061-\\u007A\\u00C0-\\u1D99]", "");

        if (containsSwear(przeklenstwa.split(" ")) != null) {
            msg.delete().queue();
//                msg.getChannel().sendMessage(String.format("<@%s>, ładnie to tak przeklinać?", msg.getAuthor().getId())).queue();

            KaryJSON.Kara kara = karyJSON.getByName("Wszelkiej maści wyzwiska, obraza, wulgaryzmy, prowokacje, groźby i inne formy przemocy");
            if (kara == null) {
                Log.newError("Powod przy nadawaniu kary za przeklenstwa jest nullem");
            } else {
                PunishCommand.putPun(kara,
                        Collections.singletonList(member),
                        member.getGuild().getSelfMember(),
                        msg.getTextChannel(),
                        caseDao, modLog, statsModule);
            }
            return;
        }
        if (containsLink(msgRaw.split(" "))) {
            action.setKara(Action.ListaKar.LINK);
            action.send();
            return;
        }

        if (containsInvite(msgRaw.split(" "))) {
            msg.delete().queue();
            action.setKara(Action.ListaKar.LINK);
            action.send();
            return;
        }

        if (msgRaw.replaceAll("(http(s)?://)?(www\\.)?(m\\.)?(youtube\\.com|youtu\\.be)/\\S+", "kurwa").contains("kurwa")) {
            if (!msg.getChannel().getId().equals("426864003562864641")) {
                Role miniyt = member.getGuild().getRoleById("425670776272715776");
                Role yt = member.getGuild().getRoleById("425670600049295360");
                if (miniyt == null || yt == null) {
                    Log.newError("Rola miniyt/yt w " + this.getClass().toString() + " jest nullem");
                    return;
                }
                if (!member.getRoles().contains(miniyt) || !member.getRoles().contains(yt)) {
                    msg.delete().queue();
                    action.setKara(Action.ListaKar.LINK);
                    action.send();
                }
            }
        }

        int emote = emoteCount(msgRaw, msg.getJDA());

        int caps = 0;
        String capsMsg = msgRaw.replaceAll(EMOJI.toString(), "").replaceAll("[^\\w\\s]*", "");
        try {
            caps = (containsCaps(capsMsg) / capsMsg.length()) * 100;
        } catch (Exception ignored) {}

        int flood = containsFlood(msgRaw.replaceAll(EMOJI.toString(), ""));

        if (flood > 4 || caps >= 80 || emote > 3) {
            Log.debug("---------------------------");
            Log.debug("user: " + msg.getAuthor().getId());
            Log.debug("msg: " + msgRaw);
            Log.debug("int flooda: " + flood);
            Log.debug("procent capsa " + caps);
            Log.debug("int emotek: " + emote);
            Log.debug("---------------------------");
            msg.delete().queue();
            action.setKara(Action.ListaKar.FLOOD);
            action.send();
        }

        // Może to nie być w 100% prawdziwe
        action.setPewnosc(false);
        action.setDeleted(false);
        if (containsSwear(new String[] {przeklenstwa}) != null ||
                containsSwear(new String[] {przeklenstwa.replaceAll(" ", "")}) != null) {
            action.setKara(Action.ListaKar.ZACHOWANIE);
            action.send();
        }

        if (member.getId().equals("415266870275342379")) {
            Log.debug("1");
            if (skrotyCount(msgRaw.toLowerCase().split(" "))) {
                action.setKara(Action.ListaKar.SKROTY);
                action.send();
            }
            Log.debug("2");
            if (skrotyCount(new String[] {msgRaw.toLowerCase()})) {
                action.setKara(Action.ListaKar.SKROTY);
                action.send();
            }
        
        }
    }

    @Nullable
    public String containsSwear(String[] list) {
        for (String s : list) {
            if (s != null && !s.isEmpty()) {
                if (getPrzeklenstwa().contains(s.toLowerCase())) return s.toLowerCase();
            }
        }
        return null;
    }

    public static boolean containsLink(String[] list) {
        for (String s : list) {
            try {
                new URL(s);
                return true;
            } catch (MalformedURLException e) {
                Matcher mat = HTTP.matcher(s);
                if (mat.matches()) return true;
            }
        }
        return false;
    }

    public static boolean containsInvite(String[] list) {
        for (String s : list) {
            String tak = s.replaceAll(DISCORD_INVITE, "CzemuTutajJestJakisJebanyInvite");
            if (tak.contains("CzemuTutajJestJakisJebanyInvite")) return true;
        }
        return false;
    }

    public static int containsFlood(String msg) {
        if (msg.length() < 3) return 0;
        msg = msg.replaceAll(" ", "");

        int tak = 0;
        int flood = 0;
        String[] ssplit = msg.split("");
        for (String split : ssplit) {
            try {
                String nastepnaLitera = ssplit[tak + 1];
                if (!split.equals("") && !nastepnaLitera.equals("") && split.equals(nastepnaLitera)) flood++;
                tak++;
            } catch (Exception ignored) {}
        }
        return flood;
    }

    public static int containsCaps(String msg) {
        msg = msg.replaceAll(" ", "").replaceAll("<@!?([0-9])*>", "");

        int caps = 0;
        char[] split = msg.toCharArray();
        if (split.length < 5) return 0;

        for (char s : split) {
            if (!String.valueOf(s).equals("")) {
                if (Character.isUpperCase(s)) caps++;
            }
        }

        return caps;
    }

    public static int emoteCount(String msg, JDA api) {
        int count = 0;
        Matcher m = EMOJI.matcher(msg);
        while (m.find())
            count++;
        for (String s : msg.split(" ")) {
            Emoji tak = Emoji.resolve(s, api);
            if (tak != null) {
                count++;
            }
        }
        return count;
    }

    public static boolean skrotyCount(String[] msg) {
        ArrayList<String> whiteList = new ArrayList<>();
        whiteList.add("jj");
        whiteList.add("jak");
        whiteList.add("juz");
        whiteList.add("już");
        whiteList.add("ja");
        whiteList.add("jem");
        whiteList.add("jez");
        whiteList.add("jej");
        whiteList.add("joł");

        for (String s : msg) {
            if (whiteList.contains(s.toLowerCase())) {
                continue;
            }
            String pat = s.replaceAll("[^\\u0020\\u0030-\\u0039\\u0041-\\u005A\\u0061-\\u007A\\u00C0-\\u1D99]", "").replaceAll(EMOJI.toString(), "");
            Log.debug(" = chatlistener = ");
            Log.debug("Wiadomosc: " + s);
            Log.debug("Po replace: " + pat);
            Log.debug(" = chatlistener = ");
            if (pat.replaceAll("[jJ][ ]?[a-z-A-Z]{1,2}", "kurwa").equals("kurwa")) return true;
        }

        return false;
    }

}