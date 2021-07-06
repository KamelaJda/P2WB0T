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

package pl.kamil0024.commands.kolkoikrzyzyk;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.kolkoikrzyzyk.entites.Slot;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.EventWaiter;

import java.awt.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Data
public class Gra {

    public static String KOLKO = "⭕";
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

    boolean koniec = false;

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
        mb.setEmbeds(getEmbed().build());

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
        if (isKoniec()) return false;

        event.getMessage().delete().queue();
        msg = msg.replaceAll(" ", "").replaceAll("gra:", "");

        Slot.ReturnType returnn = getSlot().check(msg, this, event.getMember());

        switch (returnn) {
            case BAD_FORMAT:
                event.getChannel().sendMessage(event.getAuthor().getAsMention() + ", zły format planszy (lub chcesz zając zajęte już pole)! Użycie: `gra: <nr. planszy>`. np. `gra: 1b`")
                        .queue(m -> m.delete().queueAfter(7, TimeUnit.SECONDS));
                return false;
            case FULL_MAP:
                end(null, false);
                return false;
            case WIN:
                end(event.getMember(), false);
                return false;
        }
        return true;
    }

    private void ruch(GuildMessageReceivedEvent event) {
        setKogoRuch(getKogoRuch().getId().equals(osoba1.getId()) ? osoba2 : osoba1);
        getBotMsg().editMessageEmbeds(getEmbed().build()).complete();
        waitForRuch();
    }

    public void waitForRuch() {
        eventWaiter.waitForEvent(GuildMessageReceivedEvent.class,
                this::checkRuch, this::ruch, 30, TimeUnit.SECONDS, this::end);
    }

    public String getEmote(Member member) {
        return member.getId().equals(getOsoba1().getId()) ? KOLKO : KRZYZYK;
    }

    public void end() {
        end(null, true);
    }

    public void end(@Nullable Member member, boolean brakCzasu) {
        // member == null = remis
        if (isKoniec()) return;
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.red);
        eb.setTitle("Gra w Kółko i Krzyżyk");
        eb.setDescription(getOsoba1().getAsMention() + " " + getEmote(getOsoba1())
                + "\n" + getOsoba2().getAsMention() + " " + getEmote(getOsoba2()));

        eb.addField("Plansza", getPlansza(), false);

        if (!brakCzasu) {
            if (member != null) {
                eb.addField("Runda się zakończyła", "Wygrał: " + member.getAsMention()
                        + "\nEzem jest: " + (member.getId().equals(osoba1.getId()) ? osoba2 : osoba1).getAsMention(), false);
            } else {
                eb.addField("Runda się zakończyła", "Remis! Czyli "
                        + osoba1.getAsMention() + " i " + osoba2.getAsMention() + " sa ezami.", false);
            }
        } else {
            eb.addField("Runda się zakończyła", getKogoRuch().getAsMention() + " jest ślimakiem i się nie ruszył na czas!", false);
        }

        getBotMsg().editMessageEmbeds(eb.build()).queue();
        setKoniec(true);
        KolkoIKrzyzykManager.graja.remove(osoba1.getId());
        KolkoIKrzyzykManager.graja.remove(osoba2.getId());
    }

}
