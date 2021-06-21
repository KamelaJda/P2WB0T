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

package pl.kamil0024.chat;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.chat.listener.ChatListener;

import java.util.ArrayList;
import java.util.List;

public class SwearWords {

    @Getter
    private final List<String> zwroty;

    public SwearWords() {
        this.zwroty = ChatListener.loadWords("zwroty.api");
    }

    @Nullable
    public String checkSwear(String message) {
        String[] msgsplit = message.split(" ");

        for (String s : zwroty) {
            String[] splitZwrot = s.split(" ");

            List<String> slowa = new ArrayList<>();

            boolean dodaj = false;
            int index = 0;
            int wykryteSlowa = 0;

            for (String msg : msgsplit) {
                if (dodaj) slowa.add(msg);
                else if (splitZwrot[0].equals("*") && splitZwrot[1].equalsIgnoreCase(msg)) {
                    slowa.add(msgsplit[index]);
                    slowa.add(msg);
                    dodaj = true;
                } else if (splitZwrot[0].equalsIgnoreCase(msg)) {
                    slowa.add(msg);
                    dodaj = true;
                }
                index++;
            }

            for (int i = 0; i < slowa.size(); i++) {
                String slowo = slowa.get(i);
                if (i >= splitZwrot.length) continue;
                String zwrot = splitZwrot[i];
                if (zwrot.equals("*") || zwrot.equalsIgnoreCase(slowo)) wykryteSlowa++;
            }

            if (wykryteSlowa == splitZwrot.length) return s;
        }
        return null;
    }

}
