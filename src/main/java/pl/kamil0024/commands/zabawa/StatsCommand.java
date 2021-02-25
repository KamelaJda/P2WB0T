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
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.UserstatsDao;
import pl.kamil0024.core.database.config.UserstatsConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.GsonUtil;
import pl.kamil0024.core.util.UserUtil;

import java.time.Instant;
import java.util.*;

public class StatsCommand extends Command {

    private final UserstatsDao userstatsDao;

    public StatsCommand(UserstatsDao userstatsDao) {
        name = "stats";
        cooldown = 60;
        permLevel = PermLevel.MODERATOR;
        category = CommandCategory.ZABAWA;
        enabledInRekru = true;
        this.userstatsDao = userstatsDao;
    }

    @Override
    public boolean execute(CommandContext context) {

        final Message msg = context.sendTranslate("generic.loading").complete();

        new Thread(() -> {

            long wszystkieWiadomosci = 0;
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

                for (Map.Entry<String, Long> channelEntry : memStat.getChannels().entrySet()) {
                    long suma = kanaly.getOrDefault(channelEntry.getKey(), 0L);
                    kanaly.put(channelEntry.getKey(), suma + channelEntry.getValue());
                }

            }

            Log.debug("Kanały:");
            Log.debug(GsonUtil.toJSON(kanaly));

            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setTimestamp(Instant.now());
            eb.setDescription(String.format("Napisanych wiadomości: **%s**", wszystkieWiadomosci));

            BetterStringBuilder sb = new BetterStringBuilder();
            sb.appendLine("Wiadomości na kanałach:");
            for (Map.Entry<String, Long> entry : sortByValue(kanaly).entrySet()) {
                sb.appendLine(String.format("<#%s> - **%s**", entry.getKey(), entry.getValue()));
            }
            eb.addField("Wiadomości na kanałach", sb.build(), false);

            MessageBuilder mb = new MessageBuilder();
            mb.setContent("Twoje statystyki z ostatnich **30** dni");
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

}
