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

package pl.kamil0024.commands.dews;

import com.google.gson.Gson;
import com.google.inject.Inject;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.kamil0024.commands.ModLog;
import pl.kamil0024.commands.listener.GiveawayListener;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.command.enums.CommandCategory;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.database.*;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.musicapi.MusicAPI;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.UsageException;
import pl.kamil0024.core.util.UserUtil;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.stats.StatsModule;
import pl.kamil0024.weryfikacja.WeryfikacjaModule;
import pl.kamil0024.youtrack.YouTrack;

import java.awt.*;
import java.io.File;

public class EvalCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(EvalCommand.class);

    @Inject private final EventWaiter eventWaiter;
    @Inject private final CommandManager commandManager;
    @Inject private final CaseDao caseDao;
    @Inject private final ModLog modLog;
    @Inject private final KaryJSON karyJSON;
    @Inject private final Tlumaczenia tlumaczenia;
    @Inject private final CommandExecute commandExecute;
    @Inject private final UserDao userDao;
    @Inject private final NieobecnosciDao nieobecnosciDao;
    @Inject private final RemindDao remindDao;
    @Inject private final ModulManager modulManager;
    @Inject private final GiveawayListener giveawayListener;
    @Inject private final GiveawayDao giveawayDao;
    @Inject private final StatsModule statsModule;
    @Inject private final MultiDao multiDao;
    @Inject private final MusicModule musicModule;
    @Inject private final MusicAPI musicAPI;
    @Inject private final YouTrack youTrack;
    @Inject private final TicketDao ticketDao;
    @Inject private final ApelacjeDao apelacjeDao;
    @Inject private final AnkietaDao ankietaDao;
    @Inject private final EmbedRedisManager embedRedisManager;
    @Inject private final WeryfikacjaDao weryfikacjaDao;
    @Inject private final WeryfikacjaModule weryfikacjaModule;

    public EvalCommand(EventWaiter eventWaiter, CommandManager commandManager, CaseDao caseDao, ModLog modLog, KaryJSON karyJSON, Tlumaczenia tlumaczenia, CommandExecute commandExecute, UserDao userDao, NieobecnosciDao nieobecnosciDao, RemindDao remindDao, ModulManager modulManager, GiveawayListener giveawayListener, GiveawayDao giveawayDao, StatsModule statsModule, MultiDao multiDao, MusicModule musicModule, MusicAPI musicAPI, YouTrack youTrack, TicketDao ticketDao, ApelacjeDao apelacjeDao, AnkietaDao ankietaDao, EmbedRedisManager embedRedisManager, WeryfikacjaDao weryfikacjaDao, WeryfikacjaModule weryfikacjaModule) {
        name = "eval";
        aliases.add("ev");
        category = CommandCategory.DEVS;
        permLevel = PermLevel.DEVELOPER;
        enabledInRekru = true;
        this.eventWaiter = eventWaiter;
        this.commandManager = commandManager;
        this.caseDao = caseDao;
        this.modLog = modLog;
        this.karyJSON = karyJSON;
        this.tlumaczenia = tlumaczenia;
        this.commandExecute = commandExecute;
        this.userDao = userDao;
        this.nieobecnosciDao = nieobecnosciDao;
        this.remindDao = remindDao;
        this.modulManager = modulManager;
        this.giveawayListener = giveawayListener;
        this.giveawayDao = giveawayDao;
        this.statsModule = statsModule;
        this.multiDao = multiDao;
        this.musicModule = musicModule;
        this.musicAPI = musicAPI;
        this.youTrack = youTrack;
        this.ticketDao = ticketDao;
        this.apelacjeDao = apelacjeDao;
        this.ankietaDao = ankietaDao;
        this.embedRedisManager = embedRedisManager;
        this.weryfikacjaDao = weryfikacjaDao;
        this.weryfikacjaModule = weryfikacjaModule;
    }

    @Override
    public boolean execute(CommandContext context) {
        String kod = context.getArgsToString(0);
        if (context.getArgs().get(0) == null || kod.isEmpty()) throw new UsageException();

        kod = kod.replaceAll("```", "");
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(binding);

        shell.setVariable("eventWaiter", eventWaiter);
        shell.setVariable("commandManager", commandManager);
        shell.setVariable("context", context);
        shell.setVariable("caseDao", caseDao);
        shell.setVariable("modLog", modLog);
        shell.setVariable("karyJSON", karyJSON);
        shell.setVariable("tlumaczenia", tlumaczenia);
        shell.setVariable("commandExecute", commandExecute);
        shell.setVariable("userDao", userDao);
        shell.setVariable("nieobecnosciDao", nieobecnosciDao);
        shell.setVariable("remindDao", remindDao);
        shell.setVariable("modulManager", modulManager);
        shell.setVariable("giveawayListener", giveawayListener);
        shell.setVariable("giveawayDao", giveawayDao);
        shell.setVariable("statsModule", statsModule);
        shell.setVariable("multiDao", multiDao);
        shell.setVariable("musicModule", musicModule);
        shell.setVariable("musicAPI", musicAPI);
        shell.setVariable("youTrack", youTrack);
        shell.setVariable("ticketDao", ticketDao);
        shell.setVariable("apelacjeDao", apelacjeDao);
        shell.setVariable("ankietaDao", ankietaDao);
        shell.setVariable("embedRedisManager", embedRedisManager);
        shell.setVariable("weryfikacjaDao", weryfikacjaDao);
        shell.setVariable("weryfikacjaModule", weryfikacjaModule);
        shell.setVariable("gson", new Gson());

        long ms = System.currentTimeMillis();
        Object value;
        boolean error = false;
        try {
            value = shell.evaluate(kod);
        } catch (Exception e) {
            error = true;
            value = e.getMessage();
        }
        ms = System.currentTimeMillis() - ms;

        EmbedBuilder eb = new EmbedBuilder();
        if (!error) eb.setColor(Color.green);
        else eb.setColor(Color.red);

        if (String.valueOf(value).length() >= 1000) {
            logger.debug(String.valueOf(value));
            value = context.getTranslate("eval.maxcharts");
        }

        eb.addField("\ud83d\udce5 INPUT", "```\n" + kod + "\n```", false);
        eb.addField("\ud83d\udce4 OUTPUT", "```\n" + value + "\n```", false);
        eb.setFooter(ms + "ms");
        context.send(eb.build()).queue();
        return true;
    }


}
