package pl.kamil0024.stats.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.util.EmbedPageintaor;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.stats.entities.Statystyka;

import java.util.*;

public class TopCommand extends Command {

    private final StatsDao statsDao;
    private final EventWaiter eventWaiter;
    private final NieobecnosciDao nieobecnosciDao;

    public TopCommand(StatsDao statsDao, EventWaiter eventWaiter, NieobecnosciDao nieobecnosciDao) {
        name = "top";
        permLevel = PermLevel.MODERATOR;
        this.statsDao = statsDao;
        this.eventWaiter = eventWaiter;
        this.nieobecnosciDao = nieobecnosciDao;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean execute(CommandContext context) {

        if (UserUtil.getPermLevel(context.getMember()).getNumer() < PermLevel.ADMINISTRATOR.getNumer() && !context.getMember().getId().equals("416264257978761217")) {
            context.send("Nie możesz użyć tej komendy!").queue();
            return false;
        }

        Integer dni = context.getParsed().getNumber(context.getArgs().get(0));
        if (dni == null) throw new UsageException();

        Message msg = context.send("Ładuje...").complete();
        context.getChannel().sendTyping().queue();

        new Thread(() -> {
            List<StatsConfig> staty = statsDao.getAll();
            if (staty.isEmpty()) {
                msg.editMessage("Nikt nie ma statystyk lol").queue();
            }

            HashMap<String, Suma> mapa = new HashMap<>();
            HashMap<String, Integer> top = new HashMap<>();
            ArrayList<EmbedBuilder> pages = new ArrayList<>();

            Emote green = CommandExecute.getReaction(context.getUser(), true);
            Emote red = CommandExecute.getReaction(context.getUser(), false);

            for (StatsConfig statsConfig : staty) {
                int suma = 0;
                Statystyka statyZParuDni = StatsCommand.getStatsOfDayMinus(statsConfig.getStats(), dni);
                suma += (statyZParuDni.getWyrzuconych() +
                        statyZParuDni.getZbanowanych() +
                        statyZParuDni.getZmutowanych());

                mapa.put(statsConfig.getId(), new Suma(suma, statyZParuDni));
            }

            for (Map.Entry<String, Suma> entry : mapa.entrySet()) {
                top.put(entry.getKey(), entry.getValue().getNadaneKary());
            }

            int rank = 1;
            for (Map.Entry<String, Integer> entry : sortByValue(top).entrySet()) {
                EmbedBuilder eb = new EmbedBuilder();
                User user = context.getParsed().getUser(entry.getKey());

                eb.setColor(UserUtil.getColor(context.getMember()));
                eb.setTitle("Miejsce #" + rank);
                eb.setThumbnail(user.getAvatarUrl());
                eb.setDescription(UserUtil.getFullName(user) + "\n\n" +
                        StatsCommand.getStringForStats(mapa.get(entry.getKey()).getStatystyka()) +
                        "\nMa nieobecność? " + (nieobecnosciDao.hasNieobecnosc(user.getId()) ? green.getAsMention() : red.getAsMention()));
                pages.add(eb);
                rank++;
            }

            new EmbedPageintaor(pages, context.getUser(), eventWaiter, context.getJDA(), 240).create(msg);
        }).start();
        return true;
    }

    @Data
    @AllArgsConstructor
    private class Suma {
        private Integer nadaneKary;
        private Statystyka statystyka;
    }

    public static HashMap<String, Integer> sortByValue(HashMap<String, Integer> hm) {
        List<Map.Entry<String, Integer> > list =
                new LinkedList<>(hm.entrySet());
        list.sort(Map.Entry.comparingByValue());
        Collections.reverse(list);
        HashMap<String, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

}
