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

import lombok.Getter;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.core.logger.Log;

import java.lang.reflect.Method;
import java.util.*;

public class CommandManager extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CommandManager.class);

    private final ShardManager shardManager;

    @Getter
    public Set<Command> registered;

    @Getter
    public Map<String, Command> commands;

    @Getter
    public Map<String, Command> aliases;

    @Getter
    public List<Command> slashCommands = new ArrayList<>();

    public CommandManager(ShardManager shardManager) {
        this.commands = new HashMap<>();
        this.registered = new HashSet<>();
        this.aliases = new HashMap<>();
        this.shardManager = shardManager;
    }

    public void registerCommand(Command command) {
        if (command == null) return;
        if (commands.containsKey(command.getName()))
            logger.error("Komenda o nazwie {} ({}) jest już zarejestrowana!", command.getName(), command.getClass().getName());
        if (command.getName() == null || command.getName().isEmpty()) {
            logger.error("Nazwa komendy {} jest pusta!", command.getClass().getName());
        }

        if (command.getCommandData() != null) {
            logger.debug("Dodaje slash komende {}", command.getCommandData().getName());
            slashCommands.add(command);
        }

        for (Method method : command.getClass().getMethods()) {
            try {
                if (method.isAnnotationPresent(SubCommand.class) && method.getParameterCount() == 1) {
                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    String name = subCommand.name().isEmpty() ? method.getName() : subCommand.name();
                    command.getSubCommands().put(name.toLowerCase(), method);
                    logger.debug("Zarejestrowano subkomendę: {} -> {}", name, method);
                    for (String alias : subCommand.aliases()) {
                        command.getSubCommands().put(alias.toLowerCase(), method);
                        logger.debug("Zarejestrowano alias: {} -> {} -> {}", alias, name, method);
                    }
                }
            } catch (Exception e) {
                logger.error("Nie udało się zarejestrować subkomendy!", e);
                Log.newError(e, getClass());
            }
        }

        registered.add(command);
        commands.put(command.getName(), command);
        registerAliases(command);
        logger.debug("Rejestruje komende {}", command.getName());
    }

    public void registerAliases(Command command) {
        if (command.getAliases().isEmpty()) return;
        command.getAliases().forEach(alias -> getAliases().put(alias, command));
    }

    public void unregisterCommands(List<Command> cmds) {
        for (Command command : cmds) {
            commands.values().removeIf(cmd -> command == cmd || cmd.getName().equals(command.getName()));
            registered.removeIf(cmd -> command == cmd || cmd.getName().equals(command.getName()));
        }
    }

}