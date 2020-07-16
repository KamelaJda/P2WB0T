package pl.kamil0024.commands.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.ModLog;
import pl.kamil0024.commands.listener.GiveawayListener;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.GiveawayDao;
import pl.kamil0024.core.database.config.GiveawayConfig;
import pl.kamil0024.core.util.Duration;
import pl.kamil0024.core.util.EmbedPageintaor;
import pl.kamil0024.core.util.EventWaiter;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class GiveawayCommand extends Command {

    @Getter private static HashMap<String, KonkursBuilder> konkurs = new HashMap<>();

    private GiveawayDao giveawayDao;
    private EventWaiter eventWaiter;
    private GiveawayListener giveawayListener;

    private static final String CZAS = "\n\n*Czas na odpowiedź to 1 minuta*";

    public GiveawayCommand(GiveawayDao giveawayDao, EventWaiter eventWaiter, GiveawayListener giveawayListener) {
        name = "konkurs";
        aliases.add("giveaway");
        permLevel = PermLevel.ADMINISTRATOR;

        this.giveawayDao = giveawayDao;
        this.eventWaiter = eventWaiter;
        this.giveawayListener = giveawayListener;
    }

    @Override
    public boolean execute(CommandContext context) {
        String typ = context.getArgs().get(0);
        if (typ == null) typ = "create";
        getKonkurs().remove(context.getUser().getId());
        getKonkurs().put(context.getUser().getId(), new KonkursBuilder());
        if (typ.equals("list") || typ.equals("history")) {
            List<EmbedBuilder> strony = new ArrayList<>();
            giveawayDao.getAll().forEach(kd -> strony.add(giveawayListener.createEmbed(kd)));
            Collections.reverse(strony);
            if (strony.isEmpty()) {
                context.send("Nie było żadnych konkursów :(").queue();
                return false;
            }
            new EmbedPageintaor(strony, context.getUser(), eventWaiter, context.getJDA()).create(context.getChannel());
        }
        if (typ.equals("create") || typ.equals("stworz")) {
            Message msg = context.getChannel().sendMessage("Wybierz kanał, na którym ma się odbyć konkurs..." + CZAS).complete();
            initWaiter(context.getUser().getIdLong(), context.getChannel().getIdLong(), context.getJDA(), msg, context.getParsed());
        }
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
                    if (kb.getWybor() == 4)  {
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
                    assert getKonkurs().get(String.valueOf(userId)) != null;
                    txt.sendMessage(String.format("<@%s>, twój czas na odpowiedź minał!", userId)).queue();
                }
        );
    }


    @AllArgsConstructor
    @Data
    private class KonkursBuilder {
        public KonkursBuilder() { }

        private String napisz;
        private TextChannel txt;
        private int wybor = 0;
        private long end;
        private int iloscOsob;
        private String nagroda;
    }

}