package pl.kamil0024.commands.kolkoikrzyzyk;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.kolkoikrzyzyk.entites.Slot;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.EventWaiter;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Data
public class Gra {

    public static String KOLKO = "\uD83D\uDD34";
    public static String KRZYZYK = "❌";
    public static String PUSTE = "\uD83D\uDED1";

    private Member osoba1;
    private Member osoba2;
    private Member kogoRuch;
    private TextChannel channel;
    private Message botMsg;

    private long dataRozpoczecia;

    private EventWaiter eventWaiter;
    private Slot slot;

    public Gra(Member osoba1, Member osoba2, TextChannel channel, EventWaiter eventWaiter) {
        this.osoba1 = osoba1;
        this.osoba2 = osoba2;
        this.channel = channel;

        this.kogoRuch = osoba2;
        this.dataRozpoczecia = new BDate().getTimestamp();

        this.eventWaiter = eventWaiter;
        this.slot = new Slot();
    }

    public void create() {
        Message msg = getChannel().sendMessage("Ładuje...").complete();

        MessageBuilder mb = new MessageBuilder();
        mb.setContent(" ");
        mb.setEmbed(getEmbed().build());

        msg.editMessage(mb.build()).complete();
        setBotMsg(msg);
        waitForRuch();
    }

    private EmbedBuilder getEmbed() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.cyan);
        eb.setTitle("Gra w Kółko i Krzyżyk");
        eb.setDescription(getOsoba1().getAsMention() + " vs " + getOsoba2().getAsMention()
                + "\n\n" + getOsoba1().getAsMention() + " " + getEmote(getOsoba1())
                + "\n" + getOsoba2().getAsMention() + " " + getEmote(getOsoba2()));
        eb.addField("Kogo ruch?", getKogoRuch().getAsMention(), false);

        eb.addField("Plansza", getPlansza(), false);
        eb.addField("Tipy", "Aby się ruszyć trzeba napisać `gra: <nr. planszy>`. np. `gra: 1b`\nJeżeli gracz nie ruszy się przez **30 sekund** gra zostaje przerwana!", false);

        return eb;
    }

    public String getPlansza() {
        BetterStringBuilder sb = new BetterStringBuilder();
        sb.append("```");

        sb.appendLine("1 %s | %s | %s");
        sb.appendLine("2 %s | %s | %s");
        sb.appendLine("3 %s | %s | %s");
        sb.appendLine("   A    B    C");

        sb.append("```");

        HashMap<Integer, String> s = getSlot().getSloty();
        return String.format(sb.toString(), s.get(1), s.get(2), s.get(3), s.get(4), s.get(5), s.get(6), s.get(7), s.get(8), s.get(9));
    }

    private boolean checkRuch(GuildMessageReceivedEvent event) {
        String msg = event.getMessage().getContentRaw();
        if (msg.isEmpty() || !msg.toLowerCase().startsWith("gra:")
                || !getKogoRuch().getId().equals(event.getAuthor().getId())
                || !getChannel().getId().equals(event.getChannel().getId())) return false;
        event.getMessage().delete().queue();
        msg = msg.replaceAll(" ", "");

        String[] plansza = msg.split("");
        if (plansza.length != 2) {
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", zły numer planszy! Użycie: `gra: <nr. planszy>`. np. `gra: 1b`")
                    .queueAfter(6, TimeUnit.SECONDS, m -> m.delete().queue());
            return false;
        }
        if (!getSlot().check(msg,this, event.getMember())) {
            event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", zły format planszy (lub chcesz zając zajęte już pole)! Użycie: `gra: <nr. planszy>`. np. `gra: 1b`")
                    .queueAfter(6, TimeUnit.SECONDS, m -> m.delete().queue());
            return false;
        }
        return true;
    }

    private void ruch(GuildMessageReceivedEvent event) {
        setKogoRuch(getKogoRuch().getId().equals(osoba1.getId()) ? osoba2 : osoba1);
        getBotMsg().editMessage(getEmbed().build()).complete();
        waitForRuch();
    }

    public void waitForRuch() {
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
                this::checkRuch, this::ruch, 30, TimeUnit.SECONDS, this::stopGame);
    }

    private void stopGame() {
        getBotMsg().editMessage("koniec").complete();
    }

    public String getEmote(Member member) {
        return member.getId().equals(getOsoba1().getId()) ? KOLKO : KRZYZYK;
    }

}
