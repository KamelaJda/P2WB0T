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

package pl.kamil0024.nieobecnosci.config;

import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.nieobecnosci.NieobecnosciManager;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class Zmiana {

    public Zmiana() {
    }

    private String ktoZmienia;
    private long kiedy = 0;
    private Enum coZmienia = null;
    private String komentarz = null;

    public String toString(Guild g) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy `@` HH:mm:ss");
        User u = g.getJDA().retrieveUserById(getKtoZmienia()).complete();
        sb.append("Kto zmieniał: ").append(UserUtil.getLogName(u)).append("\n");
        sb.append("Kiedy zmieniał: ").append(sfd.format(new Date(getKiedy()))).append("\n");
        sb.append("Co zmieniał: ").append(getString(getCoZmienia())).append("\n");
        if (komentarz != null) {
            sb.append("Komentarz:\n").append(getKomentarz());
        }
        return sb.toString();
    }

    public void sendLog(Guild g, String autorNieobecnosci, int idNieobecnosci) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.cyan);
        eb.setFooter("Update nieobecności ID: " + idNieobecnosci);
        eb.addField("Kto zmienia?", UserUtil.getLogName(g.retrieveMemberById(getKtoZmienia()).complete()), false);
        eb.addField("Co się stało?", getString(getCoZmienia()), false);
        eb.addField("Komu zmienia?", UserUtil.getLogName(g.retrieveMemberById(autorNieobecnosci).complete()), false);
        if (getKomentarz() != null) {
            eb.addField("Komentarz", getKomentarz(), false);
        }
        sendEmbed(eb, g.getJDA());
    }

    public static void endNieobecnosci(Nieobecnosc nb, Member member) {
        EmbedBuilder eb = NieobecnosciManager.getEmbed(nb, member, true);
        eb.setFooter("Nieobecność się zakończyła!");
        sendEmbed(eb, member.getJDA());
    }

    public static void startNieobecnosci(Nieobecnosc nb, Member member) {
        EmbedBuilder eb = NieobecnosciManager.getEmbed(nb, member, true);
        eb.setFooter("Nieobecność się rozpoczeła!");
        sendEmbed(eb, member.getJDA());
    }

    private static void sendEmbed(EmbedBuilder eb, JDA jda) {
        TextChannel txt = jda.getTextChannelById(Ustawienia.instance.channel.loginieobecnosci);
        if (txt == null) {
            Log.newError("Ustawienia.instance.channel.loginieobecnosci == null", Zmiana.class);
            return;
        }
        txt.sendMessage(eb.build()).queue();
    }

    public static String getString(Enum en) {
        switch (en) {
            case ENDTIME:
                return "Koniec czasu";
            case REASON:
                return "Zmiana powodu";
            case CANCEL:
                return "Anulowanie nieobecności";
            case START:
                return "Start nieobecności";
            case EXTENSION:
                return "Przedłużenie nieobecności";
            default:
                return en.toString();
        }
    }

    public enum Enum {
        ENDTIME, REASON, CANCEL, START, EXTENSION
    }

}
