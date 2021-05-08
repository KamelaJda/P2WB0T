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
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.util.UserUtil;

import java.time.Instant;
import java.util.HashMap;

public class ForumCommand extends Command {

    public ForumCommand() {
        name = "forum";
        cooldown = 60;
        enabledInRekru = true;
    }

    @Override
    public boolean execute(@NotNull CommandContext context) {
        EmbedBuilder eb = new EmbedBuilder();
        HashMap<Integer, String[]> tak = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        String oj = "[%s](%s)";
        eb.setColor(UserUtil.getColor(context.getMember()));
        eb.setTitle(context.getTranslate("forum.cate"));
        eb.setFooter("Forum", "https://images-ext-1.discordapp.net/external/nEimyViUHXsLUA2ltyosXai_uLaOdc0eeWhcINuYGrM/https/derpmc.pl/assets/images/DerpMC/header_logo.png");
        eb.setTimestamp(Instant.now());

        tak.put(0, new String[]{"P2W", "https://p2w.pl/"});
        tak.put(1, new String[]{"Zgłoś błąd na serwerze", "https://p2w.pl/forum/11-zg%C5%82o%C5%9B-b%C5%82%C4%85d-na-serwerze/"});
        tak.put(2, new String[]{"Propozycje", "https://p2w.pl/forum/12-propozycje/"});
        tak.put(3, new String[]{"Pytania i problemy", "https://p2w.pl/forum/25-pytania-i-problemy/"});
        tak.put(4, new String[]{"Zgłoś gracza", "https://p2w.pl/forum/8-zg%C5%82o%C5%9B-gracza/"});
        tak.put(5, new String[]{"Odwołanie od bana", "https://p2w.pl/forum/9-odwo%C5%82anie-od-bana/"});
        tak.put(6, new String[]{"Problem z płatnościami", "https://p2w.pl/forum/26-problem-z-p%C5%82atno%C5%9Bciami/"});
        tak.put(7, new String[]{"Problemy z kontem", "https://p2w.pl/forum/57-problemy-z-kontem/"});
        tak.put(8, new String[]{"Zdjęcie logowania premium", "https://p2w.pl/forum/47-zdj%C4%99cie-logowania-premium/"});
        tak.put(9, new String[]{"Rekrutacja", "https://p2w.pl/forum/38-rekrutacja/"});

        int s = 0;
        while (tak.size() != s) {
            String[] v = tak.get(s);
            sb.append(String.format(oj, v[0], v[1])).append("\n—————————————\n");
            s++;
        }
        eb.setDescription(sb.toString());
        context.send(eb.build()).queue();
        return true;
    }

}
