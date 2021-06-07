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

package pl.kamil0024.chat.listener;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.chat.Action;
import pl.kamil0024.chat.SwearWords;
import pl.kamil0024.core.Main;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.CaseDao;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.Emoji;
import pl.kamil0024.core.util.ImageUtil;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.Dowod;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.logs.logger.FakeMessage;
import pl.kamil0024.moderation.commands.MuteCommand;
import pl.kamil0024.moderation.commands.PunishCommand;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.stats.StatsModule;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChatListener extends ListenerAdapter {

    public static final Pattern HTTP = Pattern.compile("([0-9a-z_-]+\\.)+(com|infonet|net|org|pro|de|ggmc|md|me|tt|tv|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt)");
    public static final Pattern DISCORD_INVITE = Pattern.compile("(https?://)?(www\\.)?(discord\\.(gg|io|me|li)|discordapp\\.com/invite)/.+[a-z]");
    public static final Pattern EMOJI = Pattern.compile("<(a?):(\\w{2,32}):(\\d{17,19})>");
    public static final Pattern YOUTUBE_LINK = Pattern.compile("(http(s)?://)?(www\\.)?(m\\.)?(youtube\\.com|youtu\\.be)/\\S+");
    public static final Pattern SKROTY = Pattern.compile("[jJ][ ]?[a-z-A-Z]{1,2}");

    private final KaryJSON karyJSON;
    private final CaseDao caseDao;
    private final ModLog modLog;
    private final StatsModule statsModule;
    private final KaryListener karyListener;

    @Getter
    private final SwearWords swearWords;

    @Getter
    private final static List<String> przeklenstwa = loadWords("przeklenstwa.api");

    public ChatListener(KaryJSON karyJSON, CaseDao caseDao, ModLog modLog, StatsModule statsModule, KaryListener karyListener) {
        this.karyJSON = karyJSON;
        this.modLog = modLog;
        this.caseDao = caseDao;
        this.statsModule = statsModule;
        this.karyListener = karyListener;
        this.swearWords = new SwearWords();
    }

    public static List<String> loadWords(String file) {
        InputStream res = Main.class.getClassLoader().getResourceAsStream(file);
        if (res == null) {
            Log.newError("Plik " + file + " jest nullem", ChatListener.class);
            throw new NullPointerException("Plik " + file + " jest nullem");
        }

        List<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res, UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                words.add(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (words.isEmpty()) Log.newError("Lista slow w pliku " + file + "  jest pusta!", ChatListener.class);
        return words;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (!e.getGuild().getId().equals(Ustawienia.instance.bot.guildId) || e.getAuthor().isBot()) return;
        if (e.getChannel().getParent() != null && e.getChannel().getParent().getId().equals("539819570358386698"))
            return;
        if (UserUtil.getPermLevel(e.getAuthor()).getNumer() >= PermLevel.CHATMOD.getNumer()) return;
        if (e.getAuthor().isBot()) return;
        if (e.getChannel().getId().equals("426809411378479105") ||
                e.getChannel().getId().equals("503294063064121374") ||
                e.getChannel().getId().equals("573873102757429256") ||
                e.getChannel().getId().equals("816436328417198081")) return;

        char kurwa = 'a';
        try {
            kurwa = e.getMessage().getContentRaw().toCharArray()[1];
        } catch (Exception ignored) {
        }

        if (e.getChannel().getId().equals("426864003562864641") && !e.getAuthor().isBot() &&
                !e.getMessage().getContentRaw().isEmpty() && kurwa == 'p') {
            return;
        }
        checkMessage(e.getMember(), e.getMessage(), karyJSON, caseDao, modLog);
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent e) {
        if (!e.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        if (UserUtil.getPermLevel(e.getAuthor()).getNumer() >= PermLevel.CHATMOD.getNumer()) return;
        if (e.getAuthor().isBot() || e.getMessage().getContentRaw().isEmpty()) return;
        if (e.getChannel().getId().equals("426809411378479105") || e.getChannel().getId().equals("503294063064121374") || e.getChannel().getId().equals("573873102757429256"))
            return;
        checkMessage(e.getMember(), e.getMessage(), karyJSON, caseDao, modLog);
    }

    public void checkMessage(Member member, Message msg, KaryJSON karyJSON, CaseDao caseDao, ModLog modLog) {
        if (MuteCommand.hasMute(member)) return;

        Action action = new Action();
        action.setMsg(FakeMessage.convertWithoutImage(msg));

        if (!msg.getContentRaw().isEmpty()) {
            String czystaWiadomosc = msg.getContentRaw();
            String[] split = czystaWiadomosc.split("\n");
            if (czystaWiadomosc.startsWith("> ") && split.length >= 1) {
                try {
                    czystaWiadomosc = czystaWiadomosc.replaceAll(split[0], "");
                } catch (Exception ignored) { }
            }

            String msgRaw = czystaWiadomosc.replaceAll("<@!?([0-9])*>", "")
                    .replaceAll("3", "e")
                    .replaceAll("1", "i")
                    .replaceAll("0", "o")
                    .replaceAll("v", "u")
                    .replaceAll("<#(\\d+)>", "");

            String przeklenstwa = msgRaw;

            String[] tak = new String[]{"a;ą", "c;ć", "e;ę", "l;ł", "n;ń", "o;ó", "s;ś", "z;ź", "z;ż"};
            for (String s : tak) {
                String[] kurwa = s.split(";");
                przeklenstwa = przeklenstwa.replaceAll(kurwa[1], kurwa[0]);
            }
            przeklenstwa = przeklenstwa.replaceAll("[^\\u0020\\u0030-\\u0039\\u0041-\\u005A\\u0061-\\u007A\\u00C0-\\u1D99]", "");

            if (containsSwear(przeklenstwa.split(" ")) != null) {
                msg.delete().queue();

                KaryJSON.Kara kara = karyJSON.getByName("Wszelkiej maści wyzwiska, obraza, wulgaryzmy, prowokacje, groźby i inne formy przemocy");
                Dowod d = new Dowod(1, msg.getGuild().getSelfMember().getId(), msg.getContentDisplay(), null);
                if (kara == null) {
                    Log.newError("Powod przy nadawaniu kary za przeklenstwa jest nullem", ChatListener.class);
                } else {
                    PunishCommand.putPun(kara,
                            Collections.singletonList(member),
                            member.getGuild().getSelfMember(),
                            msg.getTextChannel(),
                            caseDao, modLog, statsModule, d, null);
                    return;
                }
            }

            if (containsLink(msgRaw.split(" ")) && !msg.getChannel().getId().equals("426864003562864641")) {
                action.setKara(Action.ListaKar.LINK);
                action.send(karyListener, msg.getGuild());
                return;
            }

            if (containsInvite(msgRaw.split(" "))) {
                msg.delete().queue();
                action.setDeleted(true);
                action.setKara(Action.ListaKar.LINK);
                action.send(karyListener, msg.getGuild());
                return;
            }

            if (YOUTUBE_LINK.matcher(msgRaw).find()) {
                if (!msg.getChannel().getId().equals("426864003562864641")) {
                    Role miniyt = member.getGuild().getRoleById("425670776272715776");
                    Role yt = member.getGuild().getRoleById("425670600049295360");
                    if (miniyt == null || yt == null) {
                        Log.newError("Rola miniyt/yt jest nullem", ChatListener.class);
                        return;
                    }
                    if (!member.getRoles().contains(miniyt) || !member.getRoles().contains(yt)) {
                        msg.delete().queue();
                        action.setDeleted(true);
                        action.setKara(Action.ListaKar.LINK);
                        action.send(karyListener, msg.getGuild());
                    }
                }

            }

            String takMsg = czystaWiadomosc.replaceAll("<@(&?)(!?)([0-9])*>", "")
                    .replaceAll("<#(\\d+)>", "");

            String bezEmotek = takMsg.replaceAll(EMOJI.toString(), "");
            String capsMsg = bezEmotek.replaceAll("[^\\w\\s]*", "");

            if (!msg.getChannel().getId().equals("652927860943880224")) {
                if (containsCaps(capsMsg) >= 50 || emoteCount(takMsg, msg.getJDA()) >= 10) {
                    msg.delete().queue();
                    action.setDeleted(true);
                    action.setKara(Action.ListaKar.FLOOD);
                    action.send(karyListener, msg.getGuild());
                    return;
                }
                if (containsFlood(bezEmotek) >= 10) {
                    action.setPewnosc(false);
                    action.setDeleted(false);
                    action.setKara(Action.ListaKar.FLOOD);
                    action.send(karyListener, msg.getGuild());
                    return;
                }
            }

            // Może to nie być w 100% prawdziwe
            action.setPewnosc(false);
            action.setDeleted(false);

            for (String s : getPrzeklenstwa()) {
                if (przeklenstwa.toLowerCase().contains(s) || przeklenstwa.replaceAll(" ", "").toLowerCase().contains(s)) {
                    action.setKara(Action.ListaKar.ZACHOWANIE);
                    action.send(karyListener, msg.getGuild());
                    return;
                }
            }

            if (msg.getMentionedMembers().stream().map(Member::getId).collect(Collectors.toList()).contains("371532684716933120")) {
                action.setKara(Action.ListaKar.PING);
                action.send(karyListener, msg.getGuild());
                return;
            }

            String sw = swearWords.checkSwear(przeklenstwa);
            if (sw != null) {
                action.setKara(Action.ListaKar.ZACHOWANIE);
                action.setDescription("Wykryto w wiadomości wulgarny zwrot `" + sw + "`");
                action.send(karyListener, msg.getGuild());
            }

        }

        if (!msg.getChannel().getId().equals("652263735645175818") && msg.getAttachments().size() > 0) {
            ImageUtil.readFile(msg.getAttachments().get(0)).thenAcceptAsync(st -> {
                if (st == null) return;

                action.setKara(Action.ListaKar.TEXT_SWEAR);
                action.setImageUrl(msg.getAttachments().get(0).getUrl());

                if (containsSwear(st.split(" ")) != null) {
                    action.send(karyListener, msg.getGuild());
                    return;
                }

                for (String s : getPrzeklenstwa()) {
                    if (st.toLowerCase().contains(s) || st.replaceAll(" ", "").toLowerCase().contains(s)) {
                        action.send(karyListener, msg.getGuild());
                        return;
                    }
                }

            });
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
            if (s.contains("derpmc") || s.contains("roizy") || s.contains("p2w") || s.contains("hypixel") || s.contains("discord") || s.contains("tenor"))
                continue;
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
            if (DISCORD_INVITE.matcher(s).find()) return true;
        }
        return false;
    }

    public static int containsFlood(String msg) {
        if (msg.length() < 3 || containsLink(new String[]{msg})) return 0;

        int tak = 0;
        int flood = 0;
        String[] ssplit = msg.split("");
        String floodowanyZnak = null;
        for (String split : ssplit) {
            try {
                if (split.equals(" ")) continue;
                String nastepnaLitera = ssplit[tak + 1];
                if (floodowanyZnak == null && !split.equals("") && !nastepnaLitera.isEmpty() && split.equalsIgnoreCase(nastepnaLitera)) {
                    floodowanyZnak = nastepnaLitera;
                    flood++;
                } else if (floodowanyZnak != null && floodowanyZnak.equalsIgnoreCase(split)) {
                    flood++;
                } else {
                    floodowanyZnak = nastepnaLitera;
                    if (flood < 10) flood = 0;
                }
                tak++;
            } catch (Exception ignored) { }
        }
        return flood;
    }

    public static int containsCaps(String msg) {
        msg = msg.replaceAll(" ", "")
                .replaceAll("<@!?([0-9])*>", "");

        if (containsFlood(msg) < 10) {
            msg = msg.replaceAll("([xX])", "").replaceAll("([dD])", "");
        }
        int caps = 0;
        char[] split = msg.toCharArray();
        if (split.length < 5) return 0;

        for (char s : split) {
            if (!String.valueOf(s).equals("") && Character.isUpperCase(s)) {
                caps++;
            }
        }

        try {
            return ((caps / split.length) * 100);
        } catch (Exception e) {
            return 0;
        }

    }

    public static int emoteCount(String msg, JDA api) {
        int count = 0;
        Matcher m = EMOJI.matcher(msg);
        while (m.find()) {
            count++;
        }

        List<String> list = new ArrayList<>(Arrays.asList(msg.split(" ")));
        count += checkEmote(list, api);

        return count;
    }

    private static long checkEmote(List<String> list, JDA api) {
        return list.stream().filter(m -> Emoji.resolve(m, api) != null).count();
    }

}
