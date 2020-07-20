package pl.kamil0024.commands.kolkoikrzyzyk;

import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.commands.kolkoikrzyzyk.entites.Zaproszenie;
import pl.kamil0024.core.util.EventWaiter;

import java.util.HashMap;

@Data
public class KolkoIKrzyzykManager {
    
    public HashMap<String, Zaproszenie> zaproszenia;

    private ShardManager api;
    private EventWaiter eventWaiter;

    public KolkoIKrzyzykManager(ShardManager api, EventWaiter eventWaiter) {
        zaproszenia = new HashMap<>();

        this.api = api;
        this.eventWaiter = eventWaiter;
    }

    public void stop() {
        getZaproszenia().clear();
    }

    public synchronized ZaproszenieStatus zapros(Member zapraszajacy, Member zapraszajaGo, TextChannel textChannel) {
        if (hasInvite(zapraszajacy.getId())) return ZaproszenieStatus.FAILED;

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
        new Thread(() -> {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ignored) { }
            getZaproszenia().remove(zapro.getZapraszajacy());
        }).start();
    }

    public boolean hasInvite(String id) {
        return zaproszenia.get(id) != null;
    }

    public void nowaGra(Zaproszenie zapro) {
        Member osoba1 = zapro.getChannel().getGuild().retrieveMemberById(zapro.getZapraszajacy()).complete();
        Member osoba2 = zapro.getChannel().getGuild().retrieveMemberById(zapro.getZapraszajaGo()).complete();

        if (osoba1 == null || osoba2 == null) throw new NullPointerException("osoba1 || osoba2 == null");

        new Gra(osoba1, osoba2, zapro.getChannel(), eventWaiter).create();
    }

    @Getter
    public enum ZaproszenieStatus {

        SUCCES("Pomyślnie zaproszono. Użytkownik ma **30 sekund** na napisane /kolko akceptuj %s", false),
        FAILED("Nie możesz zaprosić tego gracza! Możliwe, że masz już jedno aktywne zaproszenie", true);

        private final String msg;
        private final boolean error;

        ZaproszenieStatus(String msg, boolean error) {
            this.msg = msg;
            this.error = error;
        }

    }
    
}
