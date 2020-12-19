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

package pl.kamil0024.embedgenerator.commands;

import com.google.gson.Gson;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

public class EmbedCommand extends Command {

    private static Gson GSON = new Gson();

    private final EmbedRedisManager embedRedisManager;

    public EmbedCommand(EmbedRedisManager embedRedisManager) {
        name = "embed";
        aliases.add("embeds");
        permLevel = PermLevel.ADMINISTRATOR;
        this.embedRedisManager = embedRedisManager;
    }

    @Override
    public boolean execute(CommandContext context) {
        String firsta = context.getArgs().get(0);
        if (firsta == null || (!firsta.equalsIgnoreCase("send")) && !firsta.equalsIgnoreCase("edit")) throw new UsageException();

        TextChannel kanal = context.getParsed().getTextChannel(context.getArgs().get(1));
        if (kanal == null) {
            context.sendTranslate("embed.requiredchannel").queue();
            return false;
        }
        if (!kanal.canTalk() || !context.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_EMBED_LINKS)) {
            context.sendTranslate("embed.permissionerror").queue();
            return false;
        }
        String kod = getCode(context.getArgs());
        if (kod.startsWith("embed.")) {
            context.sendTranslate(kod).queue();
            return false;
        }
        if (firsta.equalsIgnoreCase("send")) {
            try {
                kanal.sendMessage(getEmed(kod).build()).complete();
            } catch (Exception e) {
                context.sendTranslate("embed.parseerror");
                Log.newError(e, getClass());
                return false;
            }
            context.sendTranslate("embed.successsend").queue();
            return true;
        }
        if (firsta.equalsIgnoreCase("edit")) {
            Message msg;
            try {
                msg = kanal.retrieveMessageById(context.getArgs().get(3)).complete();
                if (!msg.getAuthor().getId().equals(Ustawienia.instance.bot.botId)) throw new Exception();
            } catch (Exception e) {
                context.sendTranslate("embed.badmessage").queue();
                return false;
            }
            try {
                msg.editMessage(getEmed(kod).build()).queue();
            } catch (Exception e) {
                context.sendTranslate("embed.parseerror");
                Log.newError(e, getClass());
                return false;
            }
            context.sendTranslate("embed.successedit").queue();
            return true;
        }
        throw new UsageException();
    }

    private String getCode(HashMap<Integer, String> args) {
        String kod = args.get(2);
        if (kod == null) return "embed.requiredcode";
        String eb = embedRedisManager.get(kod);
        if (eb == null) return "embed.badcode";
        return eb;
    }
    
    private EmbedBuilder getEmed(String json) throws Exception {
        EmbedBuilder eb = new EmbedBuilder();
        Embed embed = GSON.fromJson(json, Embed.class);

        if (embed.getKolor() != null) eb.setColor(Color.decode(embed.getKolor()));
        eb.setDescription(embed.getDescription());

        if (embed.getTitleurl() != null) {
            if (EmbedBuilder.URL_PATTERN.matcher(embed.getTitle()).matches()) {
                eb.setTitle(embed.getTitle(), embed.getTitleurl());
            }
        } else eb.setTitle(embed.getTitle());
        try {
            if (EmbedBuilder.URL_PATTERN.matcher(embed.getThumbnail()).matches()) eb.setThumbnail(embed.getThumbnail());
            if (EmbedBuilder.URL_PATTERN.matcher(embed.getImage()).matches()) eb.setImage(embed.getImage());
        } catch (Exception ignored) { }

        eb.setAuthor(embed.getAuthor(), embed.getAuthorlink(), embed.getAuthorurl());

        if (embed.getFields() != null) {
            embed.getFields().forEach(f -> eb.addField(f.getName(), f.getValue(), false));
        }

        return eb;
    }

    @Data
    @AllArgsConstructor
    private static class Embed {
        private final String description;
        private final String title;
        private final String thumbnail;
        private final String image;
        private final String author;
        private final String authorlink;
        private final String authorurl;
        private final String kolor;
        private final String titleurl;
        private final List<Field> fields;
        
        @Data
        @AllArgsConstructor
        private static class Field {
            private final Integer id;
            private final String name;
            private final String value;
        }
        
    }
    
}
