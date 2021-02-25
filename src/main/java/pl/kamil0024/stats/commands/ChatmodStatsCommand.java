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

package pl.kamil0024.stats.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.stats.entities.Statystyka;

import java.util.ArrayList;

public class ChatmodStatsCommand extends Command {

    private static final String DAY_OF_STATS = "03.09.2020";

    private final StatsDao statsDao;

    public ChatmodStatsCommand(StatsDao statsDao) {
        name = "chatmodstats";
        permLevel = PermLevel.HELPER;
        this.statsDao = statsDao;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (UserUtil.getPermLevel(context.getMember()) != PermLevel.DEVELOPER && !context.getUser().getId().equals("550961672714452993")) {
            context.send("Nie możesz tego użyć!").queue();
            return false;
        }
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) mem = context.getMember();

        StatsConfig sc = statsDao.get(mem.getId());

        if (sc.getStats().isEmpty()) {
            context.send("Chłop co ma puste staty xD").queue();
            return false;
        }

        EmbedBuilder glowny = new EmbedBuilder();
        glowny.setColor(UserUtil.getColor(mem));
        glowny.setFooter(context.getTranslate("stats.dayofstats", DAY_OF_STATS));

        glowny.setThumbnail(mem.getUser().getAvatarUrl());
        glowny.setTitle(UserUtil.getName(mem.getUser()));

        glowny.addField(context.getTranslate("stats.dzisiaj"), getStringForStats(getStatsOfDayMinus(sc.getStats(), 0)).toString(), false);
        glowny.addField(context.getTranslate("stats.7day"), getStringForStats(getStatsOfDayMinus(sc.getStats(), 7)).toString(), false);
        glowny.addField(context.getTranslate("stats.14day"), getStringForStats(getStatsOfDayMinus(sc.getStats(), 14)).toString(), false);
        glowny.addField(context.getTranslate("stats.30day"), getStringForStats(getStatsOfDayMinus(sc.getStats(), 30)).toString(), false);
        context.send(glowny.build()).queue();
        return true;
    }

    public static Statystyka getStatsOfDayMinus(ArrayList<Statystyka> stats, int day) {
        Statystyka statystyka = new Statystyka();
        int minDni = new BDate().getDateTime().getDayOfYear() - day;

        for (Statystyka stat : stats) {
            if (stat.getDay() >= minDni) {
                statystyka.setNapisanychWiadomosci(stat.getNapisanychWiadomosci() + statystyka.getNapisanychWiadomosci());
                statystyka.setUsunietychWiadomosci(stat.getUsunietychWiadomosci() + statystyka.getUsunietychWiadomosci());
                statystyka.setZbanowanych(stat.getZbanowanych() + statystyka.getZbanowanych());
                statystyka.setZmutowanych(stat.getZmutowanych() + statystyka.getZmutowanych());
                statystyka.setWyrzuconych(stat.getWyrzuconych() + statystyka.getWyrzuconych());
            }
        }

        return statystyka;
    }

    public static StringBuilder getStringForStats(Statystyka statystyka) {
        StringBuilder sb = new StringBuilder();
        sb.append("Zmutowane osoby: ").append(statystyka.getZmutowanych()).append("\n");
        sb.append("Zbanowane osoby: ").append(statystyka.getZbanowanych()).append("\n");
        sb.append("Wyrzucone osoby: ").append(statystyka.getWyrzuconych()).append("\n");
        sb.append("Usunięte wiadomości: ").append(statystyka.getUsunietychWiadomosci()).append("\n");
        sb.append("Napisane wiadomości: ").append(statystyka.getNapisanychWiadomosci());
        return sb;
    }

}
