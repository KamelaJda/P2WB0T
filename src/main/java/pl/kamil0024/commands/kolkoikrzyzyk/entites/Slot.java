package pl.kamil0024.commands.kolkoikrzyzyk.entites;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Member;
import pl.kamil0024.commands.kolkoikrzyzyk.Gra;
import pl.kamil0024.core.logger.Log;

import java.util.Arrays;
import java.util.HashMap;


public class Slot {

    @Getter public HashMap<Integer, String> sloty;

    public Slot() {
        this.sloty = new HashMap<>();

        for (int i = 1; i < 10; i++) {
            sloty.put(i, Gra.PUSTE);
        }

    }

    public boolean check(String s, Gra gra, Member osoba) {
        String[] slot = s.split("");

        int jeden;
        String tak = slot[1].toLowerCase();

        try {
            jeden = Integer.parseInt(slot[0]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (jeden < 1 || jeden > 3) {
            return false;
        }
        if (!tak.equals("a") && !tak.equals("b") && !tak.equals("c")) {
            return false;
        }

        String kekw = sloty.get(format(slot));
        if (!kekw.equals(Gra.PUSTE)) {
            return false;
        }

        Log.debug("5");
        sloty.put(format(slot), gra.getEmote(osoba));
        return true;
    }

    private int format(String[] s) {
        // s[0] = numer to lewej
        // s[1] = litera na dole

        // 1 [1] | [2] | [3]
        // 2 [4] | [5] | [6]
        // 3 [7] | [8] | [9]
        //    A     B    C

        Log.debug(Arrays.toString(s));
        int slot = Integer.parseInt(s[0]);

        if (s[1].equals("a")) return jebacMatme(slot, 1, 4, 7);
        if (s[1].equals("b")) return jebacMatme(slot, 2, 5, 8);
        if (s[1].equals("c")) return jebacMatme(slot, 3, 6, 9);
        Log.debug("kurwa " + s[1]);
        return 0;
    }

    private int jebacMatme(int slot, int moze1, int moze2, int moze3) {
        if (slot == moze1) return moze1;
        if (slot == moze2) return moze2;
        if (slot == moze3) return moze3;
        Log.debug("xdddddddddddddddddddd");
        Log.debug(slot + "");
        Log.debug(moze1 + moze2 + moze3 + "");
        Log.debug("xdddddddddddddddddddd");
        throw new UnsupportedOperationException();
    }

}
