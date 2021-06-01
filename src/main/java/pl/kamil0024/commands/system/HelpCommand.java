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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UserUtil;

import java.util.Arrays;
import java.util.Map;

public class HelpCommand extends Command {

    private final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        name = "help";
        aliases = Arrays.asList("komendybota", "pomoc");
        enabledInRekru = true;
        this.commandManager = commandManager;
        commandData = new CommandData(name, Tlumaczenia.get(name + ".opis"))
                .addOption(OptionType.STRING, "cmd", "Komenda");
        hideSlash = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        String arg = context.getArgs().get(0);

        if (arg == null || arg.isEmpty()) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(context.getTranslate("help.title", commandManager.getCommands().size()));
            eb.setFooter(context.getTranslate("help.footer"));
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setDescription(context.getTranslate("help.desc", context.getPrefix()));

            for (CommandCategory cate : CommandCategory.values()) {
                StringBuilder komendy = new StringBuilder();
                for (Map.Entry<String, Command> cmd : commandManager.getCommands().entrySet()) {
                    if ((!context.executedInRekru() && cmd.getValue().isOnlyInRekru()) || (context.executedInRekru() && !cmd.getValue().isEnabledInRekru()))
                        continue;
                    if (cmd.getValue().getPermLevel().getNumer() <= UserUtil.getPermLevel(context.getMember()).getNumer()) {
                        if (cmd.getValue().getCategory() == cate) {
                            komendy.append(cmd.getKey()).append("`,` ");
                        }
                    }
                }
                if (!komendy.toString().isEmpty())
                    eb.addField(context.getTranslate("category." + cate.toString().toLowerCase()), komendy.toString(), false);
            }

            context.send(eb.build()).queue();
            return true;
        }

        Command cmd = commandManager.getCommands().getOrDefault(arg.toLowerCase(), null);

        if (cmd == null) {
            for (Map.Entry<String, Command> alias : commandManager.getAliases().entrySet()) {
                if (alias.getKey().equalsIgnoreCase(arg)) cmd = alias.getValue();
            }
        }
        if (cmd == null) {
            context.sendTranslate("help.command.doesntexist").queue();
            return false;
        }
        if (cmd.getPermLevel().getNumer() > UserUtil.getPermLevel(context.getMember()).getNumer()) cmd = null;

        EmbedBuilder eb = getUsage(context, cmd);
        context.send(eb.build()).queue();
        return true;
    }

    @Override
    public boolean execute(SlashContext context) {
        OptionMapping om = context.getEvent().getOption("cmd");

        if (om == null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(context.getTranslate("help.title", commandManager.getCommands().size()));
            eb.setFooter(context.getTranslate("help.footer"));
            eb.setColor(UserUtil.getColor(context.getMember()));
            eb.setDescription(context.getTranslate("help.desc", context.getPrefix()));

            for (CommandCategory cate : CommandCategory.values()) {
                StringBuilder komendy = new StringBuilder();
                for (Map.Entry<String, Command> cmd : commandManager.getCommands().entrySet()) {
                    if ((!context.executedInRekru() && cmd.getValue().isOnlyInRekru()) || (context.executedInRekru() && !cmd.getValue().isEnabledInRekru()))
                        continue;
                    if (cmd.getValue().getPermLevel().getNumer() <= UserUtil.getPermLevel(context.getMember()).getNumer()) {
                        if (cmd.getValue().getCategory() == cate) {
                            komendy.append(cmd.getKey()).append("`,` ");
                        }
                    }
                }
                if (!komendy.toString().isEmpty())
                    eb.addField(context.getTranslate("category." + cate.toString().toLowerCase()), komendy.toString(), false);
            }

            context.getHook().sendMessageEmbeds(eb.build()).queue();
            return true;
        }

        Command cmd = commandManager.getCommands().getOrDefault(om.getAsString().toLowerCase(), null);

        if (cmd == null) {
            for (Map.Entry<String, Command> alias : commandManager.getAliases().entrySet()) {
                if (alias.getKey().equalsIgnoreCase(om.getAsString())) cmd = alias.getValue();
            }
        }
        if (cmd == null) {
            context.sendTranslate("help.command.doesntexist");
            return false;
        }
        if (cmd.getPermLevel().getNumer() > UserUtil.getPermLevel(context.getMember()).getNumer()) cmd = null;

        EmbedBuilder eb = getUsage(context, cmd);
        context.getHook().sendMessageEmbeds(eb.build()).queue();
        return true;
    }

    public static EmbedBuilder getUsage(CommandContext context) {
        return getUsage(context, null);
    }

    public static EmbedBuilder getUsage(CommandContext context, @Nullable Command command) {
        return embed(command != null ? command : context.getCommand(), context.getPrefix(), context.getMember());
    }

    public static EmbedBuilder getUsage(SlashContext context, @Nullable Command command) {
        return embed(command != null ? command : context.getCmd(), context.getPrefix(), context.getMember());
    }

    private static EmbedBuilder embed(Command cmd, String prefix, Member author) {
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder desc = new StringBuilder();

        desc.append("```");
        desc.append(String.format(Tlumaczenia.get("help.cmd.usage"), Tlumaczenia.get(cmd.getName() + ".usage"))).append("\n");
        desc.append(String.format(Tlumaczenia.get("help.cmd.desc"), Tlumaczenia.get(cmd.getName() + ".opis"))).append("\n");
        desc.append(String.format(Tlumaczenia.get("help.cmd.category"), Tlumaczenia.get("category." + cmd.getCategory().name().toLowerCase()))).append("\n");
        desc.append(String.format(Tlumaczenia.get("help.cmd.perm"), Tlumaczenia.get(cmd.getPermLevel().getTranlsateKey()), cmd.getPermLevel().getNumer())).append("\n");
        if (cmd.getCooldown() != 0) {
            desc.append(String.format(Tlumaczenia.get("help.cmd.cooldown"), cmd.getCooldown())).append("\n");
        }

        if (!cmd.getAliases().isEmpty()) {
            desc.append(String.format(Tlumaczenia.get("help.cmd.aliases"), String.join(",", cmd.getAliases()))).append("\n");
        }

        String key = cmd.getName() + ".pomoc";
        String dodatkowaPomoc = Tlumaczenia.get(key);
        if (!dodatkowaPomoc.equals(".")) {
            eb.addField(Tlumaczenia.get("help.addpomoc"), "```\n" + dodatkowaPomoc + "```", false);
        }
        desc.append("```");
        eb.setTitle(String.format(Tlumaczenia.get("help.cmd.cmd"), prefix, cmd.getName()));
        eb.setDescription(desc.toString());
        eb.setColor(UserUtil.getColor(author));
        return eb;
    }


}
