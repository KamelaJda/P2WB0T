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
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;

import java.util.Collections;

public class CytujCommand extends Command {

    public CytujCommand() {
        name = "cytuj";
        permLevel = PermLevel.HELPER;
        cooldown = 30;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String msgId = context.getArgs().get(0);
        String komentarz = context.getArgsToString(1);
        if (msgId == null) throw new UsageException();

        Message msg = null;

        if (!msgId.contains("-")) {
            msg = kurwaJDA(context.getChannel(), msgId);
        } else {
            String[] wybierajSe = msgId.split("-");
            TextChannel txt;
            txt = context.getGuild().getTextChannelById(wybierajSe[0]);
            if (txt != null) msg = kurwaJDA(txt, wybierajSe[1]);
            else {
                txt = context.getGuild().getTextChannelById(wybierajSe[1]);
                if (txt != null) msg = kurwaJDA(txt, wybierajSe[0]);
            }
        }

        if (msg == null) {
            context.send(context.getTranslate("cytuj.ivalid")).queue();
            return false;
        }

        if (msg.getContentRaw().length() >= MessageEmbed.VALUE_MAX_LENGTH) {
            context.send(context.getTranslate("cytuj.toolong")).queue();
            return false;
        }
        String takEmbed = msg.getEmbeds().isEmpty() ? "" : context.getTranslate("cytuj.containsembed");

        EmbedBuilder eb = new EmbedBuilder();
        eb.setColor(UserUtil.getColor(context.getMember()));
        eb.setFooter("Cytuj");
        eb.setAuthor(UserUtil.getName(msg.getAuthor()), null, msg.getAuthor().getAvatarUrl());
        eb.setDescription(msg.getContentRaw().isEmpty() ? context.getTranslate("cytuj.msgempty") + " " + takEmbed : msg.getContentRaw());
        eb.addField(context.getTranslate("cytuj.kolega"), String.format("[%s](%s)", "KLIK", msg.getJumpUrl()), false);
        eb.setTimestamp(msg.getTimeCreated());

        MessageBuilder mb = new MessageBuilder();
        if (komentarz != null) {
            context.getMessage().delete().queue();
            mb.setContent("**" + UserUtil.getMcNick(context.getMember(), true) + "**: " + komentarz);
        }
        mb.setEmbed(eb.build());
        context.getChannel().sendMessage(mb.build()).
                allowedMentions(Collections.singleton(Message.MentionType.EMOTE)).
                complete();
        if (!takEmbed.isEmpty()) context.send(msg.getEmbeds().get(0)).queue();
        return true;
    }

    @Nullable
    public static Message kurwaJDA(TextChannel txt, String msgId) {
        try {
            return txt.retrieveMessageById(msgId).complete();
        } catch (ErrorResponseException ignored) {
        }
        return null;
    }

}
