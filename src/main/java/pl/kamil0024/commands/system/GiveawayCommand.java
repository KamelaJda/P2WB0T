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

package pl.kamil0024.commands.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.listener.GiveawayListener;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.SubCommand;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.GiveawayDao;
import pl.kamil0024.core.database.config.GiveawayConfig;
import pl.kamil0024.core.util.Duration;
import pl.kamil0024.core.util.EmbedPageintaor;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.moderation.listeners.ModLog;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GiveawayCommand extends Command {

    @Getter
    private static final HashMap<String, KonkursBuilder> konkurs = new HashMap<>();

    private final GiveawayDao giveawayDao;
    private final EventWaiter eventWaiter;
    private final GiveawayListener giveawayListener;

    private static final String CZAS = "\n\n*Czas na odpowiedź to 1 minuta*";

    public GiveawayCommand(GiveawayDao giveawayDao, EventWaiter eventWaiter, GiveawayListener giveawayListener) {
        name = "giveaway";
        aliases.add("konkurs");
        permLevel = PermLevel.ADMINISTRATOR;

        this.giveawayDao = giveawayDao;
        this.eventWaiter = eventWaiter;
        this.giveawayListener = giveawayListener;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        throw new UsageException();
    }

    @SubCommand(name = "list", aliases = {"history"})
    public boolean list(CommandContext context) {
        List<EmbedBuilder> strony = new ArrayList<>();
        giveawayDao.getAll().forEach(kd -> strony.add(giveawayListener.createEmbed(kd)));
        Collections.reverse(strony);
        if (strony.isEmpty()) {
            context.sendTranslate("giveaway.emptygive").queue();
            return false;
        }
        new EmbedPageintaor(strony, context.getUser(), eventWaiter).create(context.getChannel(), context.getMessage());
        return true;
    }

    @SubCommand(name = "create", aliases = {"stworz", "stwórz"})
    public boolean create(CommandContext context) {
        getKonkurs().remove(context.getUser().getId());
        getKonkurs().put(context.getUser().getId(), new KonkursBuilder());
        Message msg = context.sendTranslate("giveaway.create", CZAS).complete();
        initWaiter(context.getUser().getIdLong(), context.getChannel().getIdLong(), context.getJDA(), msg, context.getParsed());
        return true;
    }

    @SuppressWarnings("UnnecessaryReturnStatement")
    private void initWaiter(long userId, long channelId, JDA api, Message botMsg, CommandContext.ParsedArgumenty parsed) {
        eventWaiter.waitForEvent(
                GuildMessageReceivedEvent.class,
                (event) -> event.getAuthor().getIdLong() == userId && event.getChannel().getIdLong() == channelId,
                (event) -> {
                    KonkursBuilder kb = getKonkurs().get(String.valueOf(userId));
                    if (kb == null) return;
                    kb.setWybor(kb.getWybor() + 1);
                    TextChannel c = event.getChannel();
                    String umsg = event.getMessage().getContentRaw();
                    botMsg.delete().queue();
                    if (kb.getWybor() == 1) {
                        TextChannel txt = parsed.getTextChannel(umsg);
                        if (txt == null) {
                            konkurs.remove(String.valueOf(userId));
                            c.sendMessage("Kanał jest nieprawidłowy! Anuluje akcje.").queue();
                            return;
                        }
                        kb.setTxt(txt);
                        kb.setNapisz("ile ma trwać konkurs");
                    }
                    if (kb.getWybor() == 2) {
                        Long czas = new Duration().parseLong(umsg);
                        if (czas == null) {
                            konkurs.remove(String.valueOf(userId));
                            c.sendMessage("Czas jest nieprawidłowy!").queue();
                            return;
                        }
                        kb.setEnd(czas);
                        kb.setNapisz("ile osób ma wygrać");
                    }
                    if (kb.getWybor() == 3) {
                        Integer ind = parsed.getNumber(umsg);
                        if (ind == null) {
                            konkurs.remove(String.valueOf(userId));
                            c.sendMessage("Liczba osób jest nieprawidłowa!").queue();
                            return;
                        }
                        kb.setIloscOsob(ind);
                        kb.setNapisz("co jest do wygrania");
                    }
                    if (kb.getWybor() == 4) {
                        if (umsg.isEmpty()) {
                            konkurs.remove(String.valueOf(userId));
                            c.sendMessage("Powód jest pusty (jak patrycja) lol").queue();
                            return;
                        }
                        kb.setNagroda(umsg);
                        StringBuilder sb = new StringBuilder("Tworze konkurs!").append("\n");
                        sb.append("Kanał: ").append(kb.getTxt().getAsMention()).append("\n");

                        BDate bd = new BDate(ModLog.getLang());
                        sb.append("Kończy się za: ").append(bd.difference(kb.getEnd())).append("\n");

                        sb.append("Wygra osób: ").append(kb.getIloscOsob()).append("\n");
                        sb.append("Nagroda: ").append(kb.getNagroda()).append("\n");

                        c.sendMessage(sb.toString()).queue();


                        GiveawayConfig kc = giveawayDao.get();
                        kc.setKanalId(kb.getTxt().getId());
                        kc.setEnd(kb.getEnd());
                        kc.setStart(new Date().getTime());
                        kc.setWygranychOsob(kb.getIloscOsob());
                        kc.setNagroda(kb.getNagroda());
                        kc.setOrganizator(event.getAuthor().getId());
                        giveawayListener.createMessage(kc);

                        konkurs.remove(String.valueOf(userId));
                        return;
                    } else {
                        Message msg = c.sendMessage("Napisz " + kb.getNapisz() + CZAS).complete();
                        if (kb.getWybor() < 4) {
                            konkurs.put(String.valueOf(userId), kb);
                            initWaiter(userId, channelId, api, msg, parsed);
                        } else konkurs.remove(String.valueOf(userId));
                    }
                },
                2, TimeUnit.MINUTES,
                () -> {
                    TextChannel txt = api.getTextChannelById(channelId);
                    if (getKonkurs().get(String.valueOf(userId)) != null) {
                        Objects.requireNonNull(txt).sendMessage(String.format("<@%s>, twój czas na odpowiedź minał!", userId)).queue();
                    }
                }
        );
    }

    @AllArgsConstructor
    @Data
    private static class KonkursBuilder {
        public KonkursBuilder() {
        }

        private String napisz;
        private TextChannel txt;
        private int wybor = 0;
        private long end;
        private int iloscOsob;
        private String nagroda;
    }

}
