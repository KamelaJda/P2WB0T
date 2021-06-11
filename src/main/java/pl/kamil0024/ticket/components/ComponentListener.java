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

package pl.kamil0024.ticket.components;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ComponentListener extends ListenerAdapter {

    public static final String BUTTON_NAME = "CREATE-TICKET";
    public static final int MAX_CHANNELS = 50;
    public static final String CHANNEL_FORMAT = "pomoc-%s";

    public static final ActionRow categoryRow = ActionRow.of(
            Button.primary("TICKET-APELACJE", "Odwołania od bana"),
            Button.secondary("TICKET-MINECRAFT", "Pomoc serwera Minecraft"),
            Button.secondary("TICKET-FORUM", "Pomoc forum P2W.PL"),
            Button.primary("TICKET-DISCORD", "Pomoc Discorda")
    );

    public static final ActionRow actionsRow = ActionRow.of(
            Button.secondary("TICKET-CREATE_VC", "Utwórz kanał głosowy"),
            Button.danger("TICKET-CLOSE", "Utwórz kanał głosowy")
    );

    /**
     * TODO: Zmień na
     * @see pl.kamil0024.core.Ustawienia Ustawienia
     */
    private static final String CATEGORY = "762345284457332787";

    private static final long VC_RAW_PERMS = Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL);
    private static final long TXT_RAW_PERMS = Permission.getRaw(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

    public ComponentListener() { }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent e) {
        switch (e.getComponentId()) {
            case "TICKET-APELACJE":
            case "TICKET-MINECRAFT":
            case "TICKET-FORUM":
            case "TICKET-DISCORD":
                chooseCategory(e);
                break;
            case BUTTON_NAME:
                createChannel(e);
        }

    }

    private void createChannel(ButtonClickEvent e) {
        Guild guild = e.getGuild();
        if (guild == null) return;

        Category category = getCategory(guild);

        if (category.getChannels().size() >= MAX_CHANNELS) {
            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie można stworzyć ticketa z powodu zbyt dużej ilości kanałów! " +
                    "Spróbuj ponownie później");
            return;
        }

        if (getTicketChannel(ChannelType.TEXT, guild, e.getUser().getId()) != null) {
            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", Twój wcześniejszy ticket nie został jeszcze zamknięty!");
            return;
        }

        try {
            ChannelAction<TextChannel> action = guild.createTextChannel(String.format(CHANNEL_FORMAT, e.getUser().getId()))
                    .setParent(category)
                    .addMemberPermissionOverride(e.getUser().getIdLong(), TXT_RAW_PERMS, 0)
                    .addRolePermissionOverride(Long.parseLong(Ustawienia.instance.rangi.ekipa), TXT_RAW_PERMS, 0)
                    .addMemberPermissionOverride(e.getGuild().getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), 0)
                    .addRolePermissionOverride(e.getGuild().getPublicRole().getIdLong(), 0, TXT_RAW_PERMS)
                    .setTopic("Kanał pomocy użytkownika " + e.getUser().getAsMention());

            TextChannel channel = action.complete();

            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", kanał pomocy " + channel.getAsMention() + " został stworzony!");

            channel.sendMessage("Cześć " + e.getUser().getAsMention() + ", \n" +
                    "Wybierz kategorie pomocy jaką potrzebujesz klikając w odpowiedni przycisk w tej wiadomości." +
                    "Na kliknięcie masz 5 minut. Jeżeli w tym czasie nie podejmiesz żadnej akcji, kanał zostanie usunięty.")
                    .allowedMentions(Collections.singleton(Message.MentionType.USER))
                    .setActionRows(categoryRow)
                    .complete();

        } catch (Exception ex) {
            Log.newError(ex, getClass());
            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie udało się stworzyć kanału :(");
        }
    }

    private void chooseCategory(ButtonClickEvent e) {
        if (e.getMessage() != null) e.getMessage().delete().queue();
        e.deferEdit().queue();

        String extraContext = "";
        if (e.getComponentId().equals("TICKET-APELACJE")) {
            extraContext = "Aby administrator podjął odpowiednie czynności musisz napisać " +
                    "apelacje na forum (<https://p2w.pl/forum/9-odwołanie-od-bana/>). " +
                    "Po napisaniu apelacji, podeślij tutaj linka do tematu.";
        }

        e.getTextChannel()
                .sendMessage("W sumie nie wiem co tutaj dać. Akcje pod tą wiadomością może wykonywać **tylko** administracja.\n" + extraContext)
                .setActionRows(actionsRow)
                .complete();

    }

    public static Category getCategory(Guild guild) {
        Category byId = guild.getCategoryById(CATEGORY);
        if (byId == null) {
            Log.newError("Kategoria jest nullem", ComponentListener.class);
            throw new NullPointerException("Kategoria jest nullem!");
        }
        return byId;
    }

    private static void sendAndDelete(TextChannel c, String msg) {
        c.sendMessage(msg).queue(m -> m.delete().queueAfter(8, TimeUnit.SECONDS));
    }

    public static GuildChannel getTicketChannel(ChannelType type, Guild guild, String user) {
        switch (type) {
            case TEXT:
                return guild.getTextChannelsByName(String.format(CHANNEL_FORMAT, user), true).get(0);
            case VOICE:
                return guild.getVoiceChannelsByName(String.format(CHANNEL_FORMAT, user), true).get(0);
            default:
                return null;
        }
    }

}
