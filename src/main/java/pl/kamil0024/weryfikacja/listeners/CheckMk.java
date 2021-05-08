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

package pl.kamil0024.weryfikacja.listeners;

import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.UserUtil;

import java.awt.*;
import java.time.ZoneOffset;
import java.util.Date;

public class CheckMk {

    @Getter
    private final Member member;

    public CheckMk(Member member) {
        this.member = member;
    }

    public void check() {
        if (new Date(System.currentTimeMillis() - 1209600000).toInstant().atOffset(ZoneOffset.UTC).isBefore(getMember().getTimeCreated())) {
            TextChannel txt = member.getJDA().getTextChannelById(Ustawienia.instance.channel.moddc);
            if (txt == null) {
                Log.newError("Ustawienia.instance.channel.moddc == null", CheckMk.class);
                return;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(Color.red);
            eb.setFooter("Podejrzane konto!");
            eb.addField("Nick", UserUtil.getLogName(getMember()), false);
            eb.addField("Powód", "Konto bez rangi oraz data stworzenia krótsza niż **14 dni**!", false);
            eb.addField("", "Sprawdź czy te konto nie omija blacklisty lub bana", false);
            txt.sendMessage(eb.build()).queue();
        }

    }

}
