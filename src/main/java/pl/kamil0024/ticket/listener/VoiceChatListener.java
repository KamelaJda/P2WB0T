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

package pl.kamil0024.ticket.listener;

import com.google.common.collect.ImmutableList;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogOption;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import org.joda.time.DateTime;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.TicketDao;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.redis.Cache;
import pl.kamil0024.core.redis.RedisManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.ticket.config.ChannelTicketConfig;
import pl.kamil0024.ticket.config.TicketRedisManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class VoiceChatListener extends ListenerAdapter {

    private final static long EKIPA_ID = 561102835715145728L;
    private final static long RAW_PERMS = Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL);
    private final static List<String> USERS = ImmutableList.of("322644408799461377", "140580556881526784", "400561036845121536", "343467373417857025");

    private final TicketDao ticketDao;
    private final TicketRedisManager ticketRedisManager;
    private final EventWaiter eventWaiter;

    private final Cache<Long> cooldownCache;
    private final Cache<String> messagesCache;
    private final Cache<Boolean> pingCache;

    public VoiceChatListener(TicketDao ticketDao, TicketRedisManager ticketRedisManager, EventWaiter eventWaiter, RedisManager redisManager) {
        this.ticketDao = ticketDao;
        this.ticketRedisManager = ticketRedisManager;
        this.eventWaiter = eventWaiter;
        this.cooldownCache = redisManager.new CacheRetriever<Long>() {}.getCache(7200);
        this.messagesCache = redisManager.new CacheRetriever<String>() {}.getCache(7200);
        this.pingCache = redisManager.new CacheRetriever<Boolean>() {}.getCache(7200);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        Guild guild = event.getGuild();
        if (event.getChannelJoined().getId().equals(Ustawienia.instance.ticket.vcToCreate)) {
            String[] name = event.getChannelLeft().getName().split(" ");
            String channelName = name[name.length - 1].toLowerCase();
            try {
                Category cate = Objects.requireNonNull(guild.getCategoryById(Ustawienia.instance.ticket.createChannelCategory));
                ChannelAction<VoiceChannel> action = guild.createVoiceChannel(channelName + "-" + event.getMember().getId())
                        .setParent(cate)
                        .addMemberPermissionOverride(event.getMember().getIdLong(), RAW_PERMS, 0)
                        .addRolePermissionOverride(EKIPA_ID, RAW_PERMS, 0)
                        .addMemberPermissionOverride(event.getJDA().getSelfUser().getIdLong(), Permission.getRaw(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL), 0)
                        .addRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), 0, RAW_PERMS);

                VoiceChannel vc = action.complete();
                guild.moveVoiceMember(event.getMember(), vc).queue();

                List<AuditLogEntry> audit = event.getGuild().retrieveAuditLogs().type(ActionType.MEMBER_VOICE_MOVE).complete();
                User adm = null;
                for (AuditLogEntry auditlog : audit) {
                    if (auditlog.getType() == ActionType.MEMBER_VOICE_MOVE
                            && auditlog.getTimeCreated().isAfter(OffsetDateTime.now().minusMinutes(1))
                            && Ustawienia.instance.ticket.vcToCreate.equals(auditlog.getOption(AuditLogOption.CHANNEL)) && !auditlog.getUser().isBot()) {
                        adm = auditlog.getUser();
                        break;
                    }
                }

                ChannelTicketConfig ctc = new ChannelTicketConfig();
                if (adm != null) ctc.setAdmId(adm.getId());
                ctc.setChannelId(vc.getId());
                ctc.setCreatedTime(new Date().getTime());
                ctc.setUserId(event.getMember().getId());
                ctc.setKategoria(channelName);
                ticketRedisManager.putChannelConfig(ctc);

                deleteMessage(event.getMember().getId(), event.getJDA());
            } catch (ErrorResponseException er) {
                Log.newError("Nie udało się stworzyć kanału do ticketa!", getClass());
                er.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
                Log.newError("Nie udało się stworzyć kanału do ticketa!", getClass());
                Log.newError(e, getClass());
            }
            return;
        }
        checkRemoveTicket(event.getChannelLeft());
        channelJoin(event.getMember(), event.getChannelJoined());
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        channelJoin(event.getMember(), event.getChannelJoined());
    }

    private void channelJoin(Member mem, VoiceChannel channelJoined) {
        String id = channelJoined.getId();
        String memId = mem.getId();
        ChannelTicketConfig ctc = ticketRedisManager.getChannel(channelJoined.getId());
        if (ctc != null) {
            if (ctc.getAdmId() == null && UserUtil.getPermLevel(mem).getNumer() > 0) {
                ctc.setAdmId(mem.getId());
                ticketRedisManager.removeChannel(id);
                ticketRedisManager.putChannelConfig(ctc);
                return;
            }
        }
        //noinspection ConstantConditions
        if (!channelJoined.getParent().getId().equals(Ustawienia.instance.ticket.strefaPomocy)) return;

        Runnable task = () -> {
            try {
                GuildVoiceState state;
                try {
                    state = channelJoined.getGuild().retrieveMemberById(memId).complete().getVoiceState();
                } catch (Exception e) {
                    return;
                }

                if (state != null && state.getChannel() != null && state.getChannel().getId().equals(id)) {
                    Long col = cooldownCache.getIfPresent(memId);
                    if (col == null || col - new Date().getTime() <= 0) {
                        TextChannel txt = channelJoined.getGuild().getJDA().getTextChannelById(Ustawienia.instance.ticket.notificationChannel);
                        if (txt == null) {
                            Log.newError("Kanał do powiadomień ticketów jest nullem!", VoiceChatListener.class);
                            return;
                        }
                        VcType vcType = VcType.MINECRAFT;
                        cooldownCache.put(memId, new DateTime().plusMinutes(10).getMillis());
                        String msg = String.format("Użytkownik <@%s> czeka na ", memId);
                        String name = channelJoined.getName().toLowerCase();
                        if (name.contains("discord")) {
                            vcType = VcType.DISCORD;
                            msg += "kanale pomocy serwera Discord\n\n" + getMentions(VcType.DISCORD, channelJoined.getGuild());
                        } else if (name.contains("p2w")) {
                            vcType = VcType.P2W;
                            msg += "kanale pomocy forum P2W\n\n" + getMentions(VcType.P2W, channelJoined.getGuild());
                        } else if (name.contains("minecraft")) {
                            msg += "kanale pomocy serwera Minecraft\n\n" + getMentions(VcType.MINECRAFT, channelJoined.getGuild());
                        } else {
                            msg += "kanale pomocy, który nie jest wpisany do bota lol (" + name + ")\n\n" + getMentions(VcType.MINECRAFT, channelJoined.getGuild());
                        }
                        Message mmsg = txt.sendMessage(msg).allowedMentions(Collections.singleton(Message.MentionType.USER)).complete();
                        deleteMessage(memId, channelJoined.getJDA());
                        messagesCache.put(memId, mmsg.getId() + "-" + vcType.name());
                    }
                }
            } catch (Exception e) {
                Log.newError(e, getClass());
            }
        };
        ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
        ses.schedule(task, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        if (!event.getGuild().getId().equals(Ustawienia.instance.bot.guildId)) return;
        TextChannel txt = event.getJDA().getTextChannelById(Ustawienia.instance.ticket.notificationChannel);
        if (txt == null) {
            Log.newError("Kanał do powiadomień ticketów jest nullem!", VoiceChatListener.class);
            return;
        }

        String id = event.getChannel().getId();
        ChannelTicketConfig conf = ticketRedisManager.getChannel(id);
        if (conf != null) {
            String st = "<@%s>, czy chcesz wysłać ankietę dotyczącą tego zgłoszenia do gracza <@%s>?";
            Message msg = txt.sendMessage(String.format(st, conf.getAdmId(), conf.getUserId()))
                    .allowedMentions(Collections.singleton(Message.MentionType.USER))
                    .complete();
            Emote red = event.getJDA().getEmoteById(Ustawienia.instance.emote.red);
            Emote green = event.getJDA().getEmoteById(Ustawienia.instance.emote.green);
            msg.addReaction(Objects.requireNonNull(green)).queue();
            msg.addReaction(Objects.requireNonNull(red)).queue();
            ticketRedisManager.removeChannel(id);
            //noinspection ConstantConditions
            eventWaiter.waitForEvent(MessageReactionAddEvent.class,
                    (e) -> e.getUser().getId().equals(conf.getAdmId()),
                    (e) -> {
                        if (e.getReactionEmote().getId().equals(red.getId())) {
                            msg.delete().queue();
                            return;
                        }
                        ticketDao.sendMessage(event.getGuild().retrieveMemberById(conf.getUserId()).complete(),
                                event.getGuild().retrieveMemberById(conf.getAdmId()).complete(),
                                conf);
                        msg.delete().queue();
                    },
                    120, TimeUnit.SECONDS,
                    () -> msg.delete().queue());
        }

    }

    @Override
    public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
        checkRemoveTicket(event.getChannelLeft());
        deleteMessage(event.getMember().getId(), event.getJDA());
    }

    @SuppressWarnings("ConstantConditions")
    private void checkRemoveTicket(@Nullable VoiceChannel voiceChannel) {
        if (voiceChannel == null) return;
        synchronized (this) {
            if (voiceChannel == null) return;
            if (voiceChannel.getMembers().size() == 0
                    && voiceChannel.getParent().getId().equals(Ustawienia.instance.ticket.createChannelCategory)
                    && !voiceChannel.getId().equals(Ustawienia.instance.ticket.vcToCreate)) {
                try {
                    voiceChannel.delete().complete();
                } catch (Exception e) {
                    Log.newError("Nie udało się usunąć kanału z ticketem!", VoiceChatListener.class);
                    Log.newError(e, VoiceChatListener.class);
                }
            }
        }
    }

    public void deleteMessage(String id, JDA jda) {
        try {
            TextChannel xd = jda.getTextChannelById(Ustawienia.instance.ticket.notificationChannel);
            String msg = messagesCache.getIfPresent(id);
            if (msg == null || xd == null) return;
            String[] s = msg.split("-");
            xd.retrieveMessageById(s[0]).complete().delete().complete();
            pingCache.invalidate(s[1]);
        } catch (Exception ignored) { }
    }

    private String getMentions(VcType type, Guild g) {
        if (pingCache.getIfPresent(type.name()) != null && pingCache.getIfPresent(type.name())) return "";
        pingCache.put(type.name(), true);
        List<Member> l = g.getMembersWithRoles(g.getRoleById(Ustawienia.instance.rangi.ekipa))
                .stream().filter(m -> UserUtil.getPermLevel(m).getNumer() >= PermLevel.HELPER.getNumer() && filtr(m))
                .collect(Collectors.toList());

        if (type == VcType.P2W || type == VcType.DISCORD) {
            String rola = type == VcType.P2W ? Ustawienia.instance.rangi.moderatorforum : Ustawienia.instance.roles.chatMod;
            l = g.getMembersWithRoles(g.getRoleById(rola))
                    .stream().filter(this::filtr)
                    .collect(Collectors.toList());
        }

        l.removeIf(m -> USERS.contains(m.getId()));

        StringBuilder s = new StringBuilder();
        for (Member entry : l) {
            s.append(entry.getAsMention()).append(", ");
	    }

        return s.toString();
    }

    private boolean filtr(Member mem) {
        return !mem.getUser().isBot() && mem.getOnlineStatus() != OnlineStatus.IDLE && mem.getOnlineStatus() != OnlineStatus.OFFLINE;
    }

    private enum VcType {
        MINECRAFT, DISCORD, P2W
    }

}
