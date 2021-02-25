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

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.util.DynamicEmbedPageinator;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;
import pl.kamil0024.stats.entities.Statystyka;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TopCommand extends Command {

    private final StatsDao statsDao;
    private final EventWaiter eventWaiter;
    private final NieobecnosciDao nieobecnosciDao;

    public TopCommand(StatsDao statsDao, EventWaiter eventWaiter, NieobecnosciDao nieobecnosciDao) {
        name = "top";
        permLevel = PermLevel.HELPER;
        this.statsDao = statsDao;
        this.eventWaiter = eventWaiter;
        this.nieobecnosciDao = nieobecnosciDao;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public boolean execute(@NotNull CommandContext context) {
        if (UserUtil.getPermLevel(context.getMember()) != PermLevel.DEVELOPER && !context.getUser().getId().equals("550961672714452993")) {
            context.send("Nie możesz tego użyć!").queue();
            return false;
        }
        Integer dni = context.getParsed().getNumber(context.getArgs().get(0));
        if (dni == null) throw new UsageException();

        Message msg = context.send("Ładuje...").reference(context.getMessage()).complete();
        context.getChannel().sendTyping().queue();

        Runnable task = () -> {
            List<StatsConfig> staty = statsDao.getAll();
            if (staty.isEmpty()) {
                msg.editMessage(context.getTranslate("top.lol")).queue();
                return;
            }

            HashMap<String, Suma> mapa = new HashMap<>();
            HashMap<String, Integer> top = new HashMap<>();
            ArrayList<EmbedBuilder> pages = new ArrayList<>();

            Emote green = CommandExecute.getReaction(context.getUser(), true);
            Emote red = CommandExecute.getReaction(context.getUser(), false);

            for (StatsConfig statsConfig : staty) {
                int suma = 0;
                Statystyka statyZParuDni = ChatmodStatsCommand.getStatsOfDayMinus(statsConfig.getStats(), dni);
                suma += (statyZParuDni.getWyrzuconych() +
                        statyZParuDni.getZbanowanych() +
                        statyZParuDni.getZmutowanych());

                mapa.put(statsConfig.getId(), new Suma(suma, statyZParuDni));
            }

            for (Map.Entry<String, Suma> entry : mapa.entrySet()) {
                top.put(entry.getKey(), entry.getValue().getNadaneKary());
            }
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            int rank = 1;

            for (Map.Entry<String, Integer> entry : sortByValue(top).entrySet()) {
                EmbedBuilder eb = new EmbedBuilder();
                User user = context.getParsed().getUser(entry.getKey());

                eb.setColor(UserUtil.getColor(context.getMember()));
                eb.setTitle(context.getTranslate("top.rank", rank));
                eb.setThumbnail(user.getAvatarUrl());
                Nieobecnosc lastNieobecnosci = nieobecnosciDao.lastNieobecnosc(user.getId());
                String tak = "???";
                if (lastNieobecnosci != null) {
                    tak = sdf.format(new Date(lastNieobecnosci.getEnd()));
                }
                eb.setDescription(UserUtil.getFullName(user) + "\n\n" +
                        ChatmodStatsCommand.getStringForStats(mapa.get(entry.getKey()).getStatystyka()) +
                        "\nMa nieobecność? " + (nieobecnosciDao.hasNieobecnosc(user.getId()) ? green.getAsMention() : red.getAsMention()) +
                        "\nOstatnia nieobecność skończyła się o: " + tak);
                pages.add(eb);
                rank++;
            }
            List<FutureTask<EmbedBuilder>> futurePages = new ArrayList<>();
            for (EmbedBuilder page : pages) {
                futurePages.add(new FutureTask<>(() -> page));
            }
            new DynamicEmbedPageinator(futurePages, context.getUser(), eventWaiter, context.getJDA(), 240).create(msg);
        };
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.schedule(task, 1, TimeUnit.NANOSECONDS);
        return true;
    }

    @Data
    @AllArgsConstructor
    public static class Suma {
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