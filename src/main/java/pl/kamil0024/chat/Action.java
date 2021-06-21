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

package pl.kamil0024.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import pl.kamil0024.chat.listener.KaryListener;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.logs.logger.FakeMessage;

import java.awt.*;
import java.util.Objects;

@Data
public class Action {

    private ListaKar kara;
    private FakeMessage msg;

    private boolean pewnosc = true;
    private boolean isDeleted = true;
    private String botMsg = null;
    private String imageUrl = null;
    private String description = null;

    public Action() { }

    public void send(KaryListener karyListener, Guild api) {
        if (kara == null || msg == null) throw new NullPointerException("kara lub msg jest nullem");

        String content = msg.getContent();
        if (content.length() >= 1024) content = content.substring(0, 1024);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(Color.red);

        eb.addField("Użytkownik", UserUtil.getFullNameMc(Objects.requireNonNull(api.getMemberById(getMsg().getAuthor()))), false);
        eb.addField("Treść wiadomości", content, false);
        eb.addField("Kanał", String.format("<#%s>", getMsg().getChannel()), false);
        eb.addField("Za co ukarać?", kara.getPowod(), false);

        if (!pewnosc) {
            eb.addField("UWAGA", "Zgłoszenie może ukazać się fałszywe. Radze zajrzeć do kontekstu prowadzonej rozmowy", false);
        }
        if (!isDeleted) eb.addField("Link do wiadomości", String.format("[%s](%s)", "KLIK",
                String.format("https://discord.com/channels/%s/%s/%s", api.getId(), getMsg().getChannel(), getMsg().getId())), false);

        if (imageUrl != null) eb.setImage(imageUrl);

        if (getDescription() != null) eb.addField("Dodatkowa pomoc", getDescription(), false);

        TextChannel txt = api.getTextChannelById(Ustawienia.instance.channel.moddc);
        if (txt == null) throw new NullPointerException("Kanał do modów dc jest nullem");
        ActionRow actionRow = ActionRow.of(
                Button.success("1", "Ukaraj"),
                Button.danger("2", "Anuluj zgłoszenie"),
                Button.primary("3", "Usuń wiadomość")
        );
        MessageAction action = txt.sendMessage(eb.build());
        Message msg = action.setActionRows(actionRow).complete();
        karyListener.getEmbedy().put(msg.getId(), this);
    }

    @AllArgsConstructor
    @Getter
    public enum ListaKar {
        ZACHOWANIE("Wszelkiej maści wyzwiska, obraza, wulgaryzmy, prowokacje, groźby i inne formy przemocy"),
        FLOOD("Nadmierny spam, flood lub caps lock wiadomościami lub emotikonami"),
        LINK("Reklama stron, serwisów lub serwerów gier/Discord niepowiązanych w żaden sposób z P2W.pl"),
        PING("Znieważanie osoby zmrałej"),
        TEXT_SWEAR("Umieszczanie zdjęć zawierających wulgaryzmy");

        private final String powod;
    }

}
