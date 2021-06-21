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

package pl.kamil0024.commands.zabawa;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jsoup.Jsoup;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.util.NetworkUtil;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;

import java.util.Objects;

public class PogodaCommand extends Command {

    public PogodaCommand() {
        name = "pogoda";
        aliases.add("weather");
        category = CommandCategory.ZABAWA;
        cooldown = 30;
        enabledInRekru = true;
        commandData = new CommandData(name, Tlumaczenia.get(name + ".opis"))
                .addOption(OptionType.STRING, "miejsce", "Miasto lub miejsce na Ziemi", true);
    }

    @Override
    public boolean execute(SlashContext context) {
        String lokacja = Objects.requireNonNull(context.getEvent().getOption("miejsce")).getAsString();

        try {
            String downloaded = new String(NetworkUtil.download("http://en.wttr.in/" +
                    NetworkUtil.encodeURIComponent(lokacja) + "?T"));
            downloaded = Jsoup.parse(downloaded).getElementsByTag("body").text();
            if (downloaded.startsWith("ERROR:")) {
                context.sendTranslate("pogoda.errorapi");
                return false;
            }
            if (downloaded.contains("We were unable to find your location")) {
                context.sendTranslate("pogoda.badlocation");
                return false;
            }
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setTitle(context.getTranslate("pogoda.pogodaw", lokacja));
            eb.setImage("http://" + "pl.wttr.in/" +
                    NetworkUtil.encodeURIComponent(lokacja) + ".png?0m");
            context.getHook().sendMessageEmbeds(eb.build()).complete();
        } catch (Exception e) {
            context.sendTranslate("pogoda.badlocation");
            return false;
        }
        return true;
    }

}
