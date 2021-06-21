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

package pl.kamil0024.commands.kolkoikrzyzyk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.kolkoikrzyzyk.entites.Zaproszenie;
import pl.kamil0024.core.util.EventWaiter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Data
public class KolkoIKrzyzykManager {

    public HashMap<String, Zaproszenie> zaproszenia;
    public static final ArrayList<String> graja = new ArrayList<>();

    private ShardManager api;
    private EventWaiter eventWaiter;

    private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();

    public KolkoIKrzyzykManager(ShardManager api, EventWaiter eventWaiter) {
        zaproszenia = new HashMap<>();
        this.api = api;
        this.eventWaiter = eventWaiter;
    }

    public void stop() {
        graja.clear();
        getZaproszenia().clear();
    }

    public synchronized ZaproszenieStatus zapros(Member zapraszajacy, Member zapraszajaGo, TextChannel textChannel) {
        if (hasInvite(zapraszajacy.getId())) return ZaproszenieStatus.FAILED;

        if (graja.contains(zapraszajacy.getId()) || graja.contains(zapraszajaGo.getId()))
            return ZaproszenieStatus.IN_GAME;

        Zaproszenie zapro = new Zaproszenie();
        zapro.setZapraszajacy(zapraszajacy.getId());
        zapro.setZapraszajaGo(zapraszajaGo.getId());
        zapro.setKiedy(new BDate().getTimestamp());
        zapro.setId(getZaproszenia().size() + 1);
        zapro.setChannel(textChannel);

        zaproszenia.put(zapraszajacy.getId(), zapro);
        waitForRemove(zapro);

        return ZaproszenieStatus.SUCCES;
    }

    private void waitForRemove(Zaproszenie zapro) {
        ses.schedule(() -> getZaproszenia().remove(zapro.getZapraszajacy()), 30, TimeUnit.SECONDS);
    }

    public boolean hasInvite(String id) {
        return zaproszenia.get(id) != null;
    }

    @Nullable
    public Zaproszenie getZaproById(int id) {
        for (Map.Entry<String, Zaproszenie> zapro : getZaproszenia().entrySet()) {
            if (zapro.getValue().getId() == id) return zapro.getValue();
        }
        return null;
    }

    public void nowaGra(Zaproszenie zapro) {
        Member osoba1 = zapro.getChannel().getGuild().retrieveMemberById(zapro.getZapraszajacy()).complete();
        Member osoba2 = zapro.getChannel().getGuild().retrieveMemberById(zapro.getZapraszajaGo()).complete();
        if (osoba1 == null || osoba2 == null) throw new NullPointerException("osoba1 || osoba2 == null");
        graja.add(osoba1.getId());
        graja.add(osoba2.getId());
        new Gra(osoba1, osoba2, zapro.getChannel(), eventWaiter).create();
    }

    @AllArgsConstructor
    @Getter
    public enum ZaproszenieStatus {

        SUCCES("Pomyślnie zaproszono. Użytkownik ma **30 sekund** na napisane /kolko akceptuj %s", false),
        IN_GAME("Nie możesz zaprosić tej osoby, ponieważ ona (lub Ty) jesteście podczas gry!", true),
        FAILED("Nie możesz zaprosić tego gracza! Możliwe, że masz już jedno aktywne zaproszenie", true);

        private final String msg;
        private final boolean error;

    }

}
