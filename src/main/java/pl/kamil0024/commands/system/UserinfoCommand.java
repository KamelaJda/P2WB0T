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

package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.moderation.listeners.ModLog;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.Objects;

public class UserinfoCommand extends Command {

    public UserinfoCommand() {
        name = "userinfo";
        aliases.add("infouser");
        cooldown = 60;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        SimpleDateFormat sfd = new SimpleDateFormat("dd.MM.yyyy `@` HH:mm:ss");
        EmbedBuilder eb = new EmbedBuilder();

        User user = context.getUser();
        User userArg = context.getParsed().getUser(context.getArgs().get(0));
        if (userArg != null) user = userArg;

        Member member = null;
        try {
            member = context.getGuild().retrieveMemberById(user.getId()).complete();
        } catch (ErrorResponseException ignored) {
        }

        eb.setColor(UserUtil.getColor(context.getMember()));
        eb.setFooter("Wykonane przez: " + UserUtil.getName(context.getMember().getUser()) + " [" + UserUtil.getMcNick(context.getMember()) + "]");
        eb.setTimestamp(Instant.now());
        eb.setThumbnail(user.getAvatarUrl());

        eb.addField("Nick", user.getAsMention() + " [" + MarkdownSanitizer.escape(UserUtil.getMcNick(member)) + "]", false);

        long date = new BDate().getTimestamp();

        BDate discord = new BDate(user.getTimeCreated().toInstant().toEpochMilli(), ModLog.getLang());
        eb.addField(context.getTranslate("userinfo.dcjoin"), sfd.format(new Date(discord.getTimestamp())) + " `" + discord.difference(date) + "` temu", false); // + " `" + new BDate(date, ModLog.getLang()).difference(lonk) + "` temu"

        if (member != null) {
            BDate serwer = new BDate(member.getTimeJoined().toInstant().toEpochMilli(), ModLog.getLang());

            eb.addField(context.getTranslate("userinfo.serverjoin"), sfd.format(new Date(serwer.getTimestamp())) + " `" + serwer.difference(new BDate()) + "` temu", false); // + " `" + new BDate(lonk2, ModLog.getLang()).difference(date) + "` temu"
            if (member.getOnlineStatus() != OnlineStatus.OFFLINE)
                eb.addField(context.getTranslate("userinfo.status"), translateStatus(member.getOnlineStatus()), false);
            try {
                eb.addField(context.getTranslate("userinfo.game"), member.getActivities().get(0).getName(), false);
            } catch (Exception ignored) {
            }
        }
        PermLevel pm = UserUtil.getPermLevel(user);
        eb.addField(context.getTranslate("userinfo.permlvl"), context.getTranslate(pm.getTranlsateKey()) + " (" + pm.getNumer() + ")", false);
        if (!user.getFlags().isEmpty()) {
            eb.addField(context.getTranslate("userinfo.bagnes"), formatFlags(user.getFlags(), context.getJDA()), false);
        }
        context.send(eb.build()).queue();
        return true;
    }

    private String formatFlags(EnumSet<User.UserFlag> flags, JDA jda) {
        StringBuilder sb = new StringBuilder();
        Emote green = Objects.requireNonNull(jda.getGuildById(Ustawienia.instance.bot.guildId)).retrieveEmoteById(Ustawienia.instance.emote.green).complete();
        for (User.UserFlag value : User.UserFlag.values()) {
            if (flags.contains(value)) {
                sb.append(green.getAsMention()).append(" ").append(value.getName()).append("\n");
            }
        }
        return sb.toString();
    }

    private String translateStatus(OnlineStatus onlineStatus) {
        switch (onlineStatus) {
            case ONLINE:
                return "Online";
            case IDLE:
                return "Zaraz wracam";
            case DO_NOT_DISTURB:
                return "Nie przeszkadzać";
            case INVISIBLE:
                return "Niewidzialny lol";
            case OFFLINE:
                return "Offline";
            case UNKNOWN:
                return "Unknow";
            default:
                return "? (" + onlineStatus.toString() + ")";
        }
    }

}
