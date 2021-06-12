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
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.logger.Log;

import java.util.*;
import java.util.concurrent.*;

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


    public static final Button TICKET_TAKE = Button.success("TICKET-TAKE", "Przydziel siebie do pomocy");
    public static final Button TICKET_CREATE_VC = Button.secondary("TICKET-CREATE_VC", "Utwórz kanał głosowy");
    public static final Button TICKET_CLOSE = Button.danger("TICKET-CLOSE", "Zamknij kanał pomocy");

    /**
     * TODO: Zmień na
     * @see pl.kamil0024.core.Ustawienia Ustawienia
     */
    private static final String CATEGORY = "762345284457332787";

    private static final long VC_RAW_PERMS = Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL);
    private static final long TXT_RAW_PERMS = Permission.getRaw(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE);

    private final ScheduledExecutorService ses;

    private final Map<String, ScheduledFuture<?>> futureMap;
    private final List<String> toDelete;

    public ComponentListener() {
        ses =  Executors.newScheduledThreadPool(5);
        futureMap = new HashMap<>();
        toDelete = new ArrayList<>();
    }

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
                break;
            case "TICKET-CREATE_VC":
            case "TICKET-CLOSE":
                Member member = e.getMember();
                if (member != null)
                    member.getRoles().stream()
                            .filter(f -> f.getId().equals("762345077624274964")) // TODO: Ustawienia.instance.rangi.ekipa
                            .findAny()
                            .ifPresent(r -> channelAction(e));
        }
    }

    private void createChannel(ButtonClickEvent e) {
        Guild guild = e.getGuild();
        if (guild == null) return;
        e.deferEdit().queue();

        Category category = getCategory(guild);

        if (category == null) {
            Log.newError("getCategory(Guild) == null", getClass());
            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie udało się odnaleźć kategorii");
            return;
        }

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

            futureMap.put(channel.getId(), ses.schedule(() -> channel.delete().complete(), 10, TimeUnit.SECONDS)); // TODO: 5 minut

        } catch (Exception ex) {
            Log.newError(ex, getClass());
            sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie udało się stworzyć kanału :(");
        }
    }

    private void chooseCategory(ButtonClickEvent e) {
        if (e.getMessage() != null) e.getMessage().delete().queue();

        ScheduledFuture<?> future = futureMap.get(e.getChannel().getId());
        future.cancel(true);
        futureMap.remove(e.getChannel().getId());

        e.deferEdit().queue();

        String extraContext = "";
        if (e.getComponentId().equals("TICKET-APELACJE")) {
            extraContext = "\n**UWAGA** Aby administrator podjął odpowiednie czynności musisz napisać " +
                    "apelacje na forum (<https://p2w.pl/forum/9-odwołanie-od-bana/>). " +
                    "Po napisaniu apelacji, podeślij tutaj linka do tematu.";
        }

        e.getTextChannel()
                .sendMessage("Opisz tutaj swój problem i poczekaj, aż któryś z administratorów dołączy do Twojego zgłoszenia. " +
                        "\n\nAkcje pod tą wiadomością może wykonywać **tylko** administracja.\n" + extraContext)
                .setActionRows(ActionRow.of(TICKET_TAKE, TICKET_CREATE_VC, TICKET_CLOSE))
                .complete();
    }

    private void channelAction(ButtonClickEvent e) {
        if (e.getGuild() == null) return;
        e.deferEdit().queue();

        if (e.getComponentId().equals("TICKET-TAKE")) {
            try {
                e.getMessage().editMessage(e.getMessage().getContentRaw())
                        .setActionRows(ActionRow.of(TICKET_TAKE.asDisabled(), TICKET_CREATE_VC, TICKET_CLOSE))
                        .complete();
                e.getTextChannel().sendMessage("Administrator " + e.getUser().getAsMention() + " dołącza do pomocy")
                        .complete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        if (e.getComponentId().equals("TICKET-CREATE_VC")) {
            if (getTicketChannel(ChannelType.VOICE, e.getGuild(), e.getUser().getId()) != null) {
                e.getTextChannel().sendMessage(e.getUser().getAsMention() + ", kanał głosowy jest już stworzony!")
                        .complete();
                return;
            }

            Category category = getCategory(e.getGuild());

            if (category == null) {
                Log.newError("getCategory(Guild) == null", getClass());
                sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie udało się odnaleźć kategorii");
                return;
            }

            if (category.getChannels().size() >= MAX_CHANNELS) {
                sendAndDelete(e.getTextChannel(), e.getUser().getAsMention() + ", nie można stworzyć kanału z powodu zbyt dużej ilości kanałów! " +
                        "Spróbuj ponownie później");
                return;
            }

            ChannelAction<VoiceChannel> action = e.getGuild().createVoiceChannel(String.format(CHANNEL_FORMAT, e.getUser().getId()))
                    .setParent(getCategory(e.getGuild()))
                    .addMemberPermissionOverride(e.getUser().getIdLong(), VC_RAW_PERMS, 0)
                    .addRolePermissionOverride(Long.parseLong(Ustawienia.instance.rangi.ekipa), VC_RAW_PERMS, 0)
                    .addMemberPermissionOverride(e.getGuild().getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), 0)
                    .addRolePermissionOverride(e.getGuild().getPublicRole().getIdLong(), 0, VC_RAW_PERMS);

            VoiceChannel channel = action.complete();
            e.getTextChannel()
                    .sendMessage(e.getUser().getAsMention() + ", kanał głosowy " + channel.getAsMention() + " został stworzony!")
                    .complete();

            return;
        }

        if (e.getComponentId().equals("TICKET-CLOSE")) {
            if (toDelete.contains(e.getChannel().getId())) return;

            toDelete.add(e.getChannel().getId());
            e.getTextChannel()
                    .sendMessage(e.getUser().getAsMention() + ", kanał zostanie zamknięty za **30 sekund**!")
                    .complete();
            Runnable run = () -> {
                try {
                    e.getTextChannel().delete().complete();
                    GuildChannel channel = getTicketChannel(ChannelType.VOICE, e.getGuild(), e.getUser().getId());
                    if (channel != null) channel.delete().complete();
                } catch (Exception exception) {
                    Log.newError(exception, getClass());
                    e.getTextChannel().sendMessage("Nie udało się usunąć kanału! :(").complete();
                }
            };
            ses.schedule(run, 30, TimeUnit.SECONDS);

        }

    }

    private void sendAndDelete(TextChannel c, String msg) {
        c.sendMessage(msg).queue(m -> m.delete().queueAfter(8, TimeUnit.SECONDS));
    }

    @Nullable
    public static Category getCategory(@Nullable Guild guild) {
        if (guild == null) return null;
        return guild.getCategoryById(CATEGORY);
    }

    public static GuildChannel getTicketChannel(ChannelType type, Guild guild, String user) {
        List<? extends GuildChannel> gc;
        switch (type) {
            case TEXT:
                gc = guild.getTextChannelsByName(String.format(CHANNEL_FORMAT, user), true);
                break;
            case VOICE:
                gc = guild.getVoiceChannelsByName(String.format(CHANNEL_FORMAT, user), true);
                break;
            default:
                return null;
        }
        if (gc.size() == 0) return null;
        return gc.get(0);
    }

}
