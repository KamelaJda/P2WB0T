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

import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.SlashContext;
import pl.kamil0024.core.command.enums.CommandCategory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class SasinCommand extends Command {

    public SasinCommand() {
        name = "sasin";
        category = CommandCategory.ZABAWA;
        enabledInRekru = true;
        hideSlash = true;
        commandData = getData()
                .addOption(OptionType.INTEGER, "liczba", "Liczba, która ma być przerobiona na sasiny", true);
    }

    @Override
    public boolean execute(@NotNull SlashContext context) {
        long aLong = Objects.requireNonNull(context.getEvent().getOption("liczba")).getAsLong();

        try {
            int liczba = Integer.parseInt(aLong + "");
            BigDecimal sasiny = new BigDecimal(liczba / 70_000_000d).setScale(9, RoundingMode.HALF_UP);
            String sasinyStr;
            if (sasiny.intValue() == sasiny.doubleValue()) sasinyStr = String.valueOf(sasiny.intValue());
            else sasinyStr = sasiny.toPlainString();
            context.sendTranslate("sasin.result", liczba, sasinyStr);
            return true;
        } catch (Exception e) {
            context.send("Zła liczba!");
            return false;
        }
    }

}
