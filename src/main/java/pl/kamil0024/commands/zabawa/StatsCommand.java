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

package pl.kamil0024.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.joda.time.DateTime;

import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.database.UserstatsDao;
import pl.kamil0024.core.database.config.UserstatsConfig;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.moderation.listeners.ModLog;

import java.time.Instant;
import java.util.*;

public class StatsCommand extends Command {

    private static final String DATE = "27.02.2021";

    private final UserstatsDao userstatsDao;

    public StatsCommand(UserstatsDao userstatsDao) {
        name = "stats";
        cooldown = 60;
        category = CommandCategory.ZABAWA;
        enabledInRekru = true;
        this.userstatsDao = userstatsDao;
    }

    @Override
    public boolean execute(CommandContext context) {

        final Message msg = context.sendTranslate("generic.loading").complete();

        new Thread(() -> {

            long wszystkieWiadomosci = 0, cztery = 0, siedem = 0, dwadziescia = 0;
            long vcAll = 0, vcCzternascie = 0, vcSiedem = 0, vcDoba = 0;

            Map<String, Long> kanaly = new HashMap<>();

            List<UserstatsConfig> conf = userstatsDao.getFromMember(context.getUser().getId(), 30);
            if (conf.isEmpty()) {
                msg.editMessage("Nie mamy Twoich statystyk :(").queue();
                return;
            }

            for (UserstatsConfig entry : conf) {
                UserstatsConfig.Config memStat = entry.getMembers().get(context.getUser().getId());
                if (memStat == null) continue;
                wszystkieWiadomosci += memStat.getMessageCount();
                vcAll += memStat.getVoiceTimestamp();
                if (Long.parseLong(entry.getDate()) >= getRawDate(1)) {
                    vcDoba += memStat.getVoiceTimestamp();
                    dwadziescia += memStat.getMessageCount();
                }
                if (Long.parseLong(entry.getDate()) >= getRawDate(7)) {
                    vcSiedem += memStat.getVoiceTimestamp();
                    siedem += memStat.getMessageCount();
                }
                if (Long.parseLong(entry.getDate()) >= getRawDate(14)) {
                    vcCzternascie += memStat.getVoiceTimestamp();
                    cztery += memStat.getMessageCount();
                }

                for (Map.Entry<String, Long> channelEntry : memStat.getChannels().entrySet()) {
                    long suma = kanaly.getOrDefault(channelEntry.getKey(), 0L);
                    kanaly.put(channelEntry.getKey(), suma + channelEntry.getValue());
                }

            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setTimestamp(Instant.now());
            eb.setFooter("Statystyki: " + UserUtil.getMcNick(context.getMember(), true));
            eb.setThumbnail(context.getUser().getAvatarUrl());
            eb.setDescription("Statystyki liczone od: `" + DATE + "`");

            BetterStringBuilder sb = new BetterStringBuilder();

            String s = "%s: `%s wiadomości`";
            String vs = "%s: `%s`";
            sb.appendLine(String.format(s, "__30 dni__", wszystkieWiadomosci));
            sb.appendLine(String.format(s, "14 dni", cztery));
            sb.appendLine(String.format(s, "7 dni", siedem));
            sb.appendLine(String.format(s, "24 godz.", dwadziescia));
            eb.addField("Wiadomości", sb.toString(), false);

            sb = new BetterStringBuilder();
            sb.appendLine(String.format(vs, "__30 dni__", format(vcAll)));
            sb.appendLine(String.format(vs, "14 dni", format(vcCzternascie)));
            sb.appendLine(String.format(vs, "7 dni", format(vcSiedem)));
            sb.appendLine(String.format(vs, "24 godz.", format(vcDoba)));
            eb.addField("Kanały głosowe", sb.toString(), false);

            sb = new BetterStringBuilder();
            int i = 1;
            for (Map.Entry<String, Long> entry : sortByValue(kanaly).entrySet()) {
                sb.appendLine(String.format("%s. <#%s>: `%s wiadomości`", i, entry.getKey(), entry.getValue()));
                if (i == 3) break;
                i++;
            }
            eb.addField("Najbardziej aktywne kanały", sb.build(), false);

            MessageBuilder mb = new MessageBuilder();
            mb.setContent(null);
            mb.setEmbed(eb.build());
            msg.editMessage(mb.build()).queue();
        }).start();
        return true;
    }

    private static HashMap<String, Long> sortByValue(Map<String, Long> hm) {
        List<Map.Entry<String, Long> > list =
                new LinkedList<>(hm.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        HashMap<String, Long> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Long> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

    private long getRawDate(int minusDays) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(new DateTime().minusDays(minusDays).getMillis()));
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private String format(long l) {
        return new BDate(ModLog.getLang()).difference(new Date().getTime() + l);
    }

}
