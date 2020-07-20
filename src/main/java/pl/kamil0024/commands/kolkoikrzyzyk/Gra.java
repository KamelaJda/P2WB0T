package pl.kamil0024.commands.kolkoikrzyzyk;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.EventWaiter;

import java.awt.*;
import java.util.concurrent.TimeUnit;

@Data
public class Gra {

    public static String KOLKO = "\uD83D\uDD34";
    public static String KRZYZYK = "❌";

    private Member osoba1;
    private Member osoba2;
    private Member kogoRuch;

    private TextChannel channel;

    private long dataRozpoczecia;

    private Message botMsg;

    private EventWaiter eventWaiter;

    public Gra(Member osoba1, Member osoba2, TextChannel channel, EventWaiter eventWaiter) {
        this.osoba1 = osoba1;
        this.osoba2 = osoba2;
        this.channel = channel;

        this.kogoRuch = osoba2;
        this.dataRozpoczecia = new BDate().getTimestamp();

        this.eventWaiter = eventWaiter;
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
        eb.setDescription(getOsoba1().getAsMention() + " vs " + getOsoba2().getAsMention());
        eb.addField("Kogo ruch?", getKogoRuch().getAsMention(), false);

        eb.addField("Plansza", getPlansza(), false);

        return eb;
    }

    public String getPlansza() {
        BetterStringBuilder sb = new BetterStringBuilder();
        sb.append("```");

        sb.appendLine("1 ❌ | ❌ | ❌");
        sb.appendLine("2 ❌ | ❌ | ❌");
        sb.appendLine("3 ❌ | ❌ | ❌");
        sb.appendLine("   A    B    C");

        sb.append("```");
        return sb.toString();
    }

    private boolean checkRuch(GuildMessageReceivedEvent event) {
        Log.debug("check1");
        String msg = event.getMessage().getContentRaw();
        if (msg.isEmpty() || !msg.toLowerCase().startsWith("gra:")) return false;
        Log.debug("check2");
        return true;
    }

    private void ruch(GuildMessageReceivedEvent event) {
        Log.debug("nowy ruch");
        String msg = event.getMessage().getContentRaw();
        setKogoRuch(getKogoRuch().getId().equals(osoba1.getId()) ? osoba2 : osoba1);
        getBotMsg().editMessage(getEmbed().build()).complete();
        waitForRuch();
    }

    public void waitForRuch() {
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
                this::checkRuch, this::ruch, 1, TimeUnit.MINUTES, this::stopGame);
    }

    private void stopGame() {
        Log.debug("koniec gry");
        getBotMsg().editMessage("koniec").complete();
    }

}
