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

package pl.kamil0024.core.command;

import com.google.errorprone.annotations.CheckReturnValue;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.arguments.ArgumentManager;
import pl.kamil0024.core.util.Tlumaczenia;

import java.util.ArrayList;

import static pl.kamil0024.core.command.CommandContext.URLPATTERN;

public class SlashContext {

    @Getter
    private final SlashCommandEvent event;

    @Getter
    private final String prefix;

    private final ArgumentManager argumentManager;
    private final Command cmd;

    public SlashContext(SlashCommandEvent event, String prefix, ArgumentManager argumentManager, Command cmd) {
        this.event = event;
        this.prefix = prefix;
        this.argumentManager = argumentManager;
        this.cmd = cmd;
    }


    public Member getMember() {
        return event.getMember();
    }

    public User getUser() {
        return event.getUser();
    }

    public Guild getGuild() {
        return event.getGuild();
    }

    @CheckReturnValue
    public String getTranslate(String msg) {
        return Tlumaczenia.get(msg);
    }

    @CheckReturnValue
    public String getTranslate(String key, String... argi) {
        return Tlumaczenia.get(key, argi);
    }

    @CheckReturnValue
    public String getTranslate(String key, Object... argi) {
        ArrayList<String> parsedArgi = new ArrayList<>();
        for (Object arg : argi) {
            parsedArgi.add(arg.toString());
        }
        return Tlumaczenia.get(key, parsedArgi.toArray(new String[]{}));
    }

    public JDA getJDA() {
        return event.getJDA();
    }

    public ShardManager getShardManager() {
        return event.getJDA().getShardManager();
    }

    public boolean executedInRekru() {
        return getGuild().getId().equals(Ustawienia.instance.rekrutacyjny.guildId);
    }

    public InteractionHook getHook() {
        return getEvent().getHook();
    }

    public Message sendTranslate(String key, Object... obj) {
        return send(getTranslate(key, obj));
    }

    public Message sendTranslate(String key) {
        return send(getTranslate(key));
    }

    public Message send(CharSequence msg) {
        return send(msg, true);
    }

    public Message send(CharSequence msg, boolean checkUrl) {
        String message = String.valueOf(msg);
        if (checkUrl && URLPATTERN.matcher(msg).matches()) {
            message = message.replaceAll(String.valueOf(URLPATTERN), "[LINK]");
        }
        return getEvent().getHook().sendMessage(message.replaceAll("@(everyone|here)", "@\u200b$1")).complete();
    }

    public MessageChannel getChannel() {
        return getEvent().getChannel();
    }

}
