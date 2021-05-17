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

package pl.kamil0024.moderation.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.SubCommand;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.config.NieobecnosciConfig;
import pl.kamil0024.core.util.EmbedPageintaor;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.nieobecnosci.NieobecnosciManager;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;
import pl.kamil0024.nieobecnosci.config.Zmiana;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NieobecnoscCommand extends Command {

    private final NieobecnosciManager nieobecnosciManager;
    private final EventWaiter eventWaiter;
    private final NieobecnosciDao nieobecnosciDao;

    public NieobecnoscCommand(NieobecnosciManager nieobecnosciManager, EventWaiter eventWaiter, NieobecnosciDao nieobecnosciDao) {
        name = "nieobecnosc";
        aliases.add("nieobecnosci");
        aliases.add("nb");
        permLevel = PermLevel.ADMINISTRATOR;
        this.nieobecnosciManager = nieobecnosciManager;
        this.eventWaiter = eventWaiter;
        this.nieobecnosciDao = nieobecnosciDao;
        enabledInRekru = true;
    }

    @Override
    protected boolean execute(@NotNull CommandContext context) {
        String arg = context.getArgs().get(0);
        if (arg == null) throw new UsageException();

        Member mem = context.getParsed().getMember(arg);
        if (mem != null) {
            NieobecnosciConfig nbConf = nieobecnosciDao.get(mem.getId());
            if (nbConf.getNieobecnosc().isEmpty()) {
                context.sendTranslate("nieobecnosci.userdonthavenb").queue();
                return false;
            }
            List<EmbedBuilder> pages = new ArrayList<>();
            for (Nieobecnosc nieobecnosc : nbConf.getNieobecnosc()) {
                EmbedBuilder eb = NieobecnosciManager.getEmbed(nieobecnosc, mem, true);
                if (!nieobecnosc.isAktywna()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                    eb.addField(context.getTranslate("nieobecnosci.list.start"), sdf.format(new Date(nieobecnosc.getStart())), false);
                    eb.addField(context.getTranslate("nieobecnosci.list.end"), sdf.format(new Date(nieobecnosc.getEnd())), false);
                }
                List<Zmiana> zmiany = nieobecnosc.getZmiany();
                eb.addField("Ilość zmian:", zmiany == null || zmiany.isEmpty() ? "0" : zmiany.size() + "", false);
                eb.addField("Ostatnia zmiana:",
                        zmiany == null || zmiany.isEmpty() ? context.getTranslate("nieobecnosci.brak") : zmiany.get(zmiany.size() - 1).toString(context.getGuild())
                        , false);
                pages.add(eb);
            }
            Collections.reverse(pages);
            new EmbedPageintaor(pages, context.getUser(), eventWaiter, 320)
                    .create(context.getChannel(), context.getMessage());
            return true;
        }

        throw new UsageException();
    }

    @SubCommand(name = "aktywne")
    public boolean aktywne(CommandContext context) {
        ArrayList<Nieobecnosc> nball = nieobecnosciDao.getAllAktywne();
        if (nball.isEmpty()) {
            context.sendTranslate("nieobecnosci.aktywne.empty").queue();
            return false;
        }
        List<EmbedBuilder> pages = new ArrayList<>();
        for (Nieobecnosc nieobecnosc : nball) {
            Member mem = context.getParsed().getMember(nieobecnosc.getUserId());
            if (mem == null) continue;
            EmbedBuilder eb = NieobecnosciManager.getEmbed(nieobecnosc, mem, true);
            List<Zmiana> zmiany = nieobecnosc.getZmiany();
            eb.addField(context.getTranslate("nieobecnosci.aktywne.changesize"), zmiany == null || zmiany.isEmpty() ? "0" : zmiany.size() + "", false);
            eb.addField(context.getTranslate("nieobecnosci.aktywne.lastchange"),
                    zmiany == null || zmiany.isEmpty() ? context.getTranslate("nieobecnosci.brak") : zmiany.get(zmiany.size() - 1).toString(context.getGuild())
                    , false);
            pages.add(eb);
        }
        new EmbedPageintaor(pages, context.getUser(), eventWaiter, 320)
                .create(context.getChannel(), context.getMessage());
        return true;
    }

    @SubCommand(name = "powod", aliases = {"reason"})
    public boolean powod(CommandContext context) {
        String rawMember = context.getArgs().get(1);
        String powod = context.getArgsToString(2);
        if (rawMember == null || powod == null || powod.isEmpty()) {
            context.sendTranslate("nieobecnosci.powod.usage").queue();
            return false;
        }
        Member maNieobecnosc = context.getParsed().getMember(rawMember);
        if (maNieobecnosc == null) {
            context.sendTranslate("nieobecnosci.powod.usage").queue();
            return false;
        }
        NieobecnosciConfig nbConf = nieobecnosciDao.get(maNieobecnosc.getId());
        if (nbConf.getNieobecnosc().isEmpty()) {
            context.sendTranslate("nieobecnosci.userdonthavenb").queue();
            return false;
        }
        Nieobecnosc last = nbConf.getNieobecnosc().get(nbConf.getNieobecnosc().size() - 1);
        nbConf.getNieobecnosc().remove(last);
        Zmiana zmiana = new Zmiana();
        zmiana.setKtoZmienia(context.getUser().getId());
        zmiana.setCoZmienia(Zmiana.Enum.REASON);
        zmiana.setKiedy(new Date().getTime());
        zmiana.setKomentarz(context.getTranslate("nieobecnosci.sp") + " " + last.getPowod() + context.getTranslate("nieobecnosci.np") + " " + powod);
        zmiana.sendLog(context.getGuild(), last.getUserId(), last.getId());
        if (last.getZmiany() == null) last.setZmiany(new ArrayList<>());
        last.getZmiany().add(zmiana);
        last.setPowod(powod);
        nbConf.getNieobecnosc().add(last);
        nieobecnosciDao.save(nbConf);
        context.sendTranslate("nieobecnosci.powod.success").queue();
        nieobecnosciManager.update();
        return true;
    }

}
