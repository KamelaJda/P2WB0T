package pl.kamil0024.stats.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.stats.entities.Statystyka;

import java.util.ArrayList;

public class StatsCommand extends Command {

    private static final String DAY_OF_STATS = "18.07.2020";

    private StatsDao statsDao;

    public StatsCommand(StatsDao statsDao) {
        name = "stats";
        permLevel = PermLevel.MODERATOR;

        this.statsDao = statsDao;
    }

    @Override
    public boolean execute(CommandContext context) {
        Member mem = context.getParsed().getMember(context.getArgs().get(0));
        if (mem == null) mem = context.getMember();

        StatsConfig sc = statsDao.get(mem.getId());

        if (sc.getStats().isEmpty()) {
            context.send("Chłop co ma puste staty xD").queue();
            return false;
        }

        EmbedBuilder glowny = new EmbedBuilder();
        glowny.setColor(UserUtil.getColor(mem));
        glowny.setFooter("Statystyki liczone od: " + DAY_OF_STATS);

        glowny.setThumbnail(mem.getUser().getAvatarUrl());
        glowny.setTitle(UserUtil.getName(mem.getUser()));

        glowny.addField("Statystyki z dzisiaj", getStringForStats(getStatsOfDayMinus(sc.getStats(), 0)).toString(), false);
        glowny.addField("Statystyki z 7 dni", getStringForStats(getStatsOfDayMinus(sc.getStats(), 7)).toString(), false);
        glowny.addField("Statystyki z 14 dni", getStringForStats(getStatsOfDayMinus(sc.getStats(), 14)).toString(), false);
        glowny.addField("Statystyki z 30 dni", getStringForStats(getStatsOfDayMinus(sc.getStats(), 30)).toString(), false);
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
            }
        }

        return statystyka;
    }

    public static StringBuilder getStringForStats(Statystyka statystyka) {
        StringBuilder sb = new StringBuilder();
        sb.append("Zmutowane osoby: ").append(statystyka.getZmutowanych()).append("\n");
        sb.append("Zbanowane osoby: ").append(statystyka.getZbanowanych()).append("\n");
        sb.append("Usunięte wiadomości: ").append(statystyka.getUsunietychWiadomosci()).append("\n");
        sb.append("Napisane wiadomości: ").append(statystyka.getNapisanychWiadomosci());
        return sb;
    }

}