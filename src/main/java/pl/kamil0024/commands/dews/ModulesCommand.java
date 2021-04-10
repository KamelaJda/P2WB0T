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

package pl.kamil0024.commands.dews;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.util.BetterStringBuilder;
import pl.kamil0024.core.util.Error;
import pl.kamil0024.core.util.UserUtil;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;

public class ModulesCommand extends Command {

    private final ModulManager modulManager;

    public ModulesCommand(ModulManager modulManager) {
        name = "modules";
        aliases.add("modul");
        permLevel = PermLevel.DEVELOPER;
        category = CommandCategory.DEVS;
        enabledInRekru = true;

        this.modulManager = modulManager;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String arg = context.getArgs().get(0);
        ArrayList<Modul> modules = modulManager.getModules();

        String red = CommandExecute.getReaction(context.getUser(), false).getAsMention();
        String green = CommandExecute.getReaction(context.getUser(), true).getAsMention();
        String load = Objects.requireNonNull(context.getJDA().getEmoteById(Ustawienia.instance.emote.load)).getAsMention();

        if (arg == null) {
            BetterStringBuilder sb = new BetterStringBuilder();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setTimestamp(Instant.now());
            for (Modul modul : modules) {
                sb.appendLine((modul.isStart() ? green : red) + " " + modul.getName());
            }
            eb.setDescription("Ilość modułów: " + modules.size() + "\n\n" + sb.build());
            context.send(eb.build()).queue();
            return true;
        }

        String name = context.getArgs().get(1);
        if (name == null) {
            context.sendTranslate("modules.modulname").queue();
            return false;
        }

        Modul modul = null;
        for (Modul m : modulManager.getModules()) {
            if (m.getName().equalsIgnoreCase(name)) {
                modul = m;
                break;
            }
        }

        if (modul == null) {
            context.send("Moduł `" + name + "` nie istnieje").queue();
            return false;
        }

        if (arg.equalsIgnoreCase("reload")) {
            Message msg = context.send(getReloadEmbed(0, green, red, load)).complete();
            msg.editMessage(getReloadEmbed(2, green, red, load)).complete();
            try {
                modul.shutDown();
                msg.editMessage(getReloadEmbed(4, green, red, load)).complete();
            } catch (Exception e) {
                msg.editMessage(getReloadEmbed(3, green, red, load, "Nie udało się zatrzymać modułu!")).queue();
                return false;
            }
            try {
                modul.startUp();
                msg.editMessage(getReloadEmbed(6, green, red, load)).complete();
                return true;
            } catch (Exception e) {
                msg.editMessage(getReloadEmbed(5, green, red, load, "Nie znaleziono modułu")).complete();
                return false;
            }
        }

        if (arg.equalsIgnoreCase("start")) {
            if (modul.isStart()) {
                context.send("Ten moduł jest załadowany!").queue();
                return false;
            }
            try {
                modul.startUp();
                modul.setStart(true);
                context.send("Pomyślnie uruchomiono moduł " + modul.getName()).queue();
                return true;
            } catch (Exception e) {
                context.send("Nie udało się wystartować modułu! " + e.getLocalizedMessage());
                return false;
            }
        }

        if (arg.equalsIgnoreCase("stop")) {
            if (!modul.isStart()) {
                context.send("Ten moduł jest zatrzymany!").queue();
                return false;
            }
            try {
                modul.shutDown();
                modul.setStart(false);
                context.send("Pomyślnie zatrzymano moduł " + modul.getName()).queue();
                return true;
            } catch (Exception e) {
                context.send("Nie udało się zatrzymać modułu! " + e.getLocalizedMessage());
                return false;
            }
        }

        Error.usageError(context);
        return false;
    }

    private MessageEmbed getReloadEmbed(int status, String green, String red, String reload) {
        return getReloadEmbed(status, green, red, reload, "");
    }

    private MessageEmbed getReloadEmbed(int status, String green, String red, String reload, String dopisek) {
        EmbedBuilder eb = new EmbedBuilder();
        BetterStringBuilder sb = new BetterStringBuilder();
        eb.setColor(Color.green);

        switch (status) { // ale to jest rak
            case 0:
                sb.appendLine(reload + " Szukam modulu...");
                sb.appendLine(green + " Unload");
                sb.appendLine(green + " Reload");
                break;
            case 1:
                sb.appendLine(red + " Szukam modulu (" + dopisek + ")");
                sb.appendLine(green + " Unload");
                sb.appendLine(green + " Reload");
                break;
            case 2:
                sb.appendLine(green + " Szukam modulu");
                sb.appendLine(reload + " Unload...");
                sb.appendLine(green + " Reload");
                break;
            case 3:
                sb.appendLine(green + " Szukam modulu");
                sb.appendLine(red + " Unload (" + dopisek + ")");
                sb.appendLine(green + " Reload");
                break;
            case 4:
                sb.appendLine(green + " Szukam modulu");
                sb.appendLine(green + " Unload");
                sb.appendLine(reload + " Reload...");
                break;
            case 5:
                sb.appendLine(green + " Szukam modulu...");
                sb.appendLine(green + " Unload");
                sb.appendLine(red + " Reload (" + dopisek + ")");
                break;
            case 6:
                sb.appendLine(green + " Szukam modulu");
                sb.appendLine(green + " Unload");
                sb.appendLine(green + " Reload");
                break;
        }
        eb.setDescription(sb.build());

        return eb.build();
    }

}
