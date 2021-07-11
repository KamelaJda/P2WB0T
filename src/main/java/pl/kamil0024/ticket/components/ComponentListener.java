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

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.database.TXTTicketDao;
import pl.kamil0024.core.database.config.TXTTicketConfig;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ComponentListener extends ListenerAdapter {

    public static final String BUTTON_NAME = "CREATE-TICKET";
    public static final  int   MAX_CHANNELS = 50;
    public static final String CHANNEL_FORMAT = "pomoc-%s";

    public static final ActionRow categoryRow = ActionRow.of(
            SelectionMenu.create("TICKET-CHOOSE-CATEGORY")
                    .setPlaceholder("Wybierz kategorię")
                    .addOption("Odwołania od bana", "TICKET-APELACJE")
                    .addOption("Pomoc serwera Minecraft", "TICKET-MINECRAFT")
                    .addOption("Pomoc forum P2W.PL", "TICKET-FORUM")
                    .addOption("Pomoc Discorda", "TICKET-DISCORD")
                    .setRequiredRange(1, 1)
                    .build()
    );

    public static final Button TICKET_TAKE = Button.success("TICKET-TAKE", "Przydziel siebie do pomocy");
    public static final Button TICKET_CREATE_VC = Button.secondary("TICKET-CREATE_VC", "Utwórz kanał głosowy");
    public static final Button TICKET_CLOSE = Button.danger("TICKET-CLOSE", "Zamknij kanał pomocy");

    private static final Random RANDOM = new Random();

    private static final long VC_RAW_PERMS = Permission.getRaw(
            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.VIEW_CHANNEL
    );
    private static final long TXT_RAW_PERMS = Permission.getRaw(
            Permission.MESSAGE_ATTACH_FILES,
            Permission.MESSAGE_HISTORY,
            Permission.MESSAGE_READ,
            Permission.MESSAGE_WRITE
    );

    private final Map<String, ScheduledFuture<?>> futureMap;
    private final Cache<TXTTicketConfig> daoCache;
    private final List<String> toDelete;
    private final TXTTicketDao txtTicketDao;
    private final ScheduledExecutorService ses;

    public ComponentListener(TXTTicketDao txtTicketDao, RedisManager redisManager) {
        this.txtTicketDao = txtTicketDao;
        ses =  Executors.newScheduledThreadPool(5);
        futureMap = new HashMap<>();
        toDelete = new ArrayList<>();
        daoCache = redisManager.new CacheRetriever<TXTTicketConfig>() {}.getCache(0);
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent e) {
        if (!e.getComponentId().equals("TICKET-CHOOSE-CATEGORY")) return;
        switch (e.getValues().get(0)) {
            case "TICKET-APELACJE":
            case "TICKET-MINECRAFT":
            case "TICKET-FORUM":
            case "TICKET-DISCORD":
                e.deferEdit().queue();
                chooseCategory(e);
        }
    }

    @Override
    public void onButtonClick(ButtonClickEvent e) {
        switch (e.getComponentId()) {
            case BUTTON_NAME:
                createChannel(e);
                break;
            case "TICKET-CREATE_VC":
            case "TICKET-TAKE":
            case "TICKET-CLOSE":
                Member member = e.getMember();
                if (member != null) {
                    Role role = member.getRoles().stream()
                            .filter(f -> f.getId().equals(Ustawienia.instance.rangi.ekipa))
                            .findAny().orElse(null);
                    if (role != null) {
                        e.deferReply(false).queue();
                        channelAction(e);
                    } else {
                        e.deferReply(true).queue();
                        e.getHook().sendMessage("Nie możesz wykonać tej akcji!").queue();
                    }
                }
        }
    }

    private void createChannel(ButtonClickEvent e) {
        Guild guild = e.getGuild();
        if (guild == null) return;
        e.deferReply(true).queue();

        Category category = getCategory(guild, e.getTextChannel());
        if (category == null) return;

        if (category.getChannels().size() >= MAX_CHANNELS) {
            e.getHook().sendMessage(Tlumaczenia.get("ticket.toomuchchannels", e.getUser().getAsMention())).queue();
            return;
        }

        if (getTicketChannel(ChannelType.TEXT, guild, e.getUser().getId()) != null) {
            e.getHook().sendMessage(Tlumaczenia.get("ticket.lastt", e.getUser().getAsMention())).queue();
            return;
        }

        try {
            ChannelAction<TextChannel> action = guild.createTextChannel(String.format(CHANNEL_FORMAT, e.getUser().getId()))
                    .setParent(category)
                    .addMemberPermissionOverride(e.getUser().getIdLong(), TXT_RAW_PERMS, 0)
                    .addRolePermissionOverride(Long.parseLong(Ustawienia.instance.rangi.ekipa), TXT_RAW_PERMS, 0)
                    .addMemberPermissionOverride(e.getGuild().getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ), 0)
                    .addRolePermissionOverride(e.getGuild().getPublicRole().getIdLong(), 0, TXT_RAW_PERMS)
                    .setTopic(Tlumaczenia.get("ticket.channeltopic", e.getUser().getAsMention()));

            TextChannel channel = action.complete();
            e.getHook().sendMessage(Tlumaczenia.get("ticket.create", e.getUser().getAsMention(), channel.getAsMention())).queue();
            channel.sendMessage(Tlumaczenia.get("ticket.choosecategory", e.getUser().getAsMention()))
                    .allowedMentions(Collections.singleton(Message.MentionType.USER))
                    .setActionRows(categoryRow)
                    .complete();

            TXTTicketConfig ticketConfig = new TXTTicketConfig(String.valueOf(RANDOM.nextInt(Integer.MAX_VALUE)));
            ticketConfig.setUserId(e.getUser().getId());
            ticketConfig.setUserNick(UserUtil.getMcNick(e.getMember()));
            ticketConfig.setCreatedAt(new Date().getTime());
            daoCache.put(channel.getId(), ticketConfig);

            futureMap.put(channel.getId(), ses.schedule(() -> {
                daoCache.invalidate(channel.getId());
                channel.delete().complete();
            }, 3, TimeUnit.MINUTES));

        } catch (Exception ex) {
            Log.newError(ex, getClass());
            sendAndDelete(e.getTextChannel(), Tlumaczenia.get("ticket.createerror", e.getUser().getAsMention()));
        }
    }

    private void chooseCategory(SelectionMenuEvent e) {
        if (e.getMessage() != null) e.getMessage().delete().queue();

        ScheduledFuture<?> future = futureMap.get(e.getChannel().getId());
        future.cancel(true);
        futureMap.remove(e.getChannel().getId());

        TicketCategory category = null;

        try {
            category = TicketCategory.valueOf(e.getValues().get(0).split("-")[1]);
        } catch (Exception ignored) { }

        String c = category != null ? category.getName() : e.getComponentId();

        String extraContext = "";
        if (e.getComponentId().equals("TICKET-APELACJE")) extraContext = Tlumaczenia.get("ticket.extrahelp") + "\n\n";

        e.getTextChannel().sendMessage(Tlumaczenia.get("ticket.info", c, e.getUser().getAsMention(), extraContext))
                .setActionRows(ActionRow.of(TICKET_TAKE, TICKET_CREATE_VC, TICKET_CLOSE))
                .complete();

        TXTTicketConfig config = daoCache.getIfPresent(e.getTextChannel().getId());
        config.setCategory(c);
        daoCache.put(e.getTextChannel().getId(), config);
    }

    private void channelAction(ButtonClickEvent e) {
        if (e.getGuild() == null) return;

        if (e.getComponentId().equals("TICKET-TAKE")) {
            try {
                TXTTicketConfig config = daoCache.getIfPresent(e.getTextChannel().getId());
                config.setAdmId(e.getUser().getId());
                config.setAdmNick(UserUtil.getMcNick(e.getMember()));
                daoCache.put(e.getTextChannel().getId(), config);
                Objects.requireNonNull(e.getMessage()).editMessage(e.getMessage().getContentRaw())
                        .setActionRows(ActionRow.of(TICKET_TAKE.asDisabled(), TICKET_CREATE_VC, TICKET_CLOSE))
                        .complete();
                e.getHook().sendMessage(Tlumaczenia.get("ticket.admjoin", e.getUser().getAsMention())).complete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        if (e.getComponentId().equals("TICKET-CREATE_VC")) {
            if (getTicketChannel(ChannelType.VOICE, e.getGuild(), e.getUser().getId()) != null) {
                e.getHook().sendMessage((Tlumaczenia.get("ticket.vcalreadycreated", e.getUser().getAsMention()))).queue();
                return;
            }
            Category category = getCategory(e.getGuild(), e.getTextChannel());
            if (category == null) return;

            if (category.getChannels().size() >= MAX_CHANNELS) {
                e.getHook().sendMessage(Tlumaczenia.get("ticket.toomuchchannels", e.getUser().getAsMention())).queue();
                return;
            }

            ChannelAction<VoiceChannel> action = Objects.requireNonNull(e.getGuild()).createVoiceChannel(String.format(CHANNEL_FORMAT, e.getUser().getId()))
                    .setParent(getCategory(e.getGuild(), e.getTextChannel()))
                    .addMemberPermissionOverride(e.getUser().getIdLong(), VC_RAW_PERMS, 0)
                    .addRolePermissionOverride(Long.parseLong(Ustawienia.instance.rangi.ekipa), VC_RAW_PERMS, 0)
                    .addMemberPermissionOverride(e.getGuild().getSelfMember().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), 0)
                    .addRolePermissionOverride(e.getGuild().getPublicRole().getIdLong(), 0, VC_RAW_PERMS);

            TXTTicketConfig config = daoCache.getIfPresent(e.getTextChannel().getId());
            if (config != null) {
                action = action.addMemberPermissionOverride(Long.parseLong(config.getUserId()), VC_RAW_PERMS, 0);
            }

            VoiceChannel channel = action.complete();
            e.getHook().sendMessage(Tlumaczenia.get("ticket.createvc", e.getUser().getAsMention(), channel.getAsMention())).queue();
            return;
        }

        if (e.getComponentId().equals("TICKET-CLOSE")) {
            if (toDelete.contains(e.getChannel().getId())) return;

            toDelete.add(e.getChannel().getId());
            e.getHook().sendMessage(Tlumaczenia.get("ticket.close", e.getUser().getAsMention())).queue();
            Runnable run = () -> {
                toDelete.remove(e.getChannel().getId());
                try {
                    GuildChannel channel = getTicketChannel(ChannelType.VOICE, e.getGuild(), e.getUser().getId());
                    if (channel != null) channel.delete().complete();
                    TXTTicketConfig conf = daoCache.getIfPresent(e.getChannel().getId());
                    txtTicketDao.save(conf);
                    daoCache.put(e.getTextChannel().getId(), conf);

                    List<String> messages = e.getTextChannel().getIterableHistory()
                            .takeAsync(1000)
                            .thenApply(ArrayList::new).join().stream()
                            .map(m -> String.format("%s[%s]: %s", UserUtil.getMcNick(m.getMember(), true), m.getId(), m.getContentRaw()))
                            .collect(Collectors.toList());
                    Collections.reverse(messages);

                    StringBuilder sb = new StringBuilder();
                    for (String s : messages) {
                        sb.append(s).append("\n");
                    }
                    String string = sb.toString();

                    int page = 0;
                    List<String> rawMessages = new ArrayList<>();
                    for (int i = 0; i < messages.size(); i++) {
                        int from = Math.max(0, page * 2_000);
                        int to = Math.min(string.length(), (page + 1) * 2_000);
                        if (from > string.length()) break;
                        try {
                            rawMessages.add(string.substring(from, to));
                        } catch (Exception exception) {
                            break;
                        }
                        page++;
                    }

                    try {
                        List<PrivateChannel> pv = new ArrayList<>();
                        if (conf.getAdmId() != null) {
                            try {
                                pv.add(e.getGuild().getMemberById(conf.getAdmId()).getUser().openPrivateChannel().complete());
                            } catch (Exception ignored) { }
                        }

                        try {
                            pv.add(e.getGuild().getMemberById(conf.getUserId()).getUser().openPrivateChannel().complete());
                        } catch (Exception ignored) { }

                        pv.forEach(p -> p.sendMessage(Tlumaczenia.get("ticket.transcript", conf.getId())).complete());
                        for (String s : rawMessages) {
                            pv.forEach(p -> p.sendMessage(s).complete());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    e.getTextChannel().delete().complete();
                } catch (Exception exception) {
                    Log.newError(exception, getClass());
                    toDelete.remove(e.getChannel().getId());
                    e.getHook().sendMessage(Tlumaczenia.get("ticket.deleteerror", e.getUser().getAsMention())).queue();
                }
            };
            ses.schedule(run, 30, TimeUnit.SECONDS);
        }
    }

    private void sendAndDelete(TextChannel c, String msg) {
        c.sendMessage(msg).queue(m -> m.delete().queueAfter(8, TimeUnit.SECONDS));
    }

    @Nullable
    public Category getCategory(@Nullable Guild guild, TextChannel channel) {
        if (guild == null) return null;
        Category id = guild.getCategoryById(Ustawienia.instance.ticket.createChannelCategory);
        if (id == null) {
            Log.newError("getCategory(Guild) == null", getClass());
            sendAndDelete(channel, Tlumaczenia.get("ticket.notfindcate"));
        }
        return id;
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

    @Getter
    @AllArgsConstructor
    private enum TicketCategory {
        APELACJE("Odwołanie od bana"),
        FORUM("Pomoc dotycząca forum"),
        DISCORD("Pomoc dotycząca Discorda"),
        MINECRAFT("Pomoc serwera Minecraft");

        String name;
    }

}