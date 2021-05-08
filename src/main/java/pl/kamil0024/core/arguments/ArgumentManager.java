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

package pl.kamil0024.core.arguments;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ArgumentManager {

    private static final Logger logger = LoggerFactory.getLogger(ArgumentManager.class);

    @Getter
    public Map<String, Args> arguments;

    public ArgumentManager() {
        arguments = new HashMap<>();
    }

    public void registerAll() {
        ArrayList<Args> args = new ArrayList<>();

        args.add(new UserArgument());
        args.add(new MemberArgument());
        args.add(new TextChannelArgument());

        args.forEach(this::register);
    }

    public void register(Args arg) {
        if (arg == null) return;
        if (arguments.containsKey(arg.toString()))
            throw new IllegalArgumentException("Ten argument jest juz zarejestrowany!");
        arguments.put(arg.toString(), arg);
        logger.debug("Rejestruje argument '{}'", arg.getName());
    }

    public Args getArgument(String name) {
        return getArguments().get(name);
    }
}
