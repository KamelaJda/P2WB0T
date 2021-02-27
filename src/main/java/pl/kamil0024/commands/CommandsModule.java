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

package pl.kamil0024.commands;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.commands.kolkoikrzyzyk.KolkoIKrzyzykManager;
import pl.kamil0024.commands.listener.GiveawayListener;
import pl.kamil0024.commands.listener.GuildListener;
import pl.kamil0024.commands.zabawa.KolkoIKrzyzykCommand;
import pl.kamil0024.commands.zabawa.PogodaCommand;
import pl.kamil0024.commands.zabawa.SasinCommand;
import pl.kamil0024.commands.zabawa.StatsCommand;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.socket.SocketManager;
import pl.kamil0024.core.userstats.manager.UserstatsManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.embedgenerator.entity.EmbedRedisManager;
import pl.kamil0024.moderation.commands.StatusCommand;
import pl.kamil0024.moderation.listeners.ModLog;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.stats.StatsModule;
import pl.kamil0024.commands.dews.*;
import pl.kamil0024.commands.system.*;
import pl.kamil0024.core.database.*;
import pl.kamil0024.weryfikacja.WeryfikacjaModule;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandsModule implements Modul {

    private ArrayList<Command> cmd;

    private final CommandManager commandManager;
    private final Tlumaczenia tlumaczenia;
    private final ShardManager api;
    private final EventWaiter eventWaiter;
    private final KaryJSON karyJSON;
    private final CaseDao caseDao;
    private final ModulManager modulManager;
    private final CommandExecute commandExecute;
    private final UserDao userDao;
    private final NieobecnosciDao nieobecnosciDao;
    private final RemindDao remindDao;
    private final GiveawayDao giveawayDao;
    private final StatsModule statsModule;
    private final MusicModule musicModule;
    private final MultiDao multiDao;
    private final TicketDao ticketDao;
    private final ApelacjeDao apelacjeDao;
    private final AnkietaDao ankietaDao;
    private final EmbedRedisManager embedRedisManager;
    private final WeryfikacjaDao weryfikacjaDao;
    private final WeryfikacjaModule weryfikacjaModule;
    private final RecordingDao recordingDao;
    private final SocketManager socketManager;
    private final DeletedMessagesDao deletedMessagesDao;
    private final AcBanDao acBanDao;
    private final UserstatsManager userstatsManager;

    private boolean start = false;
    private final ModLog modLog;

    // Listeners
    KolkoIKrzyzykManager kolkoIKrzyzykManager;
    GuildListener guildListener;
    GiveawayListener giveawayListener;

    public CommandsModule(CommandManager commandManager, Tlumaczenia tlumaczenia, ShardManager api, EventWaiter eventWaiter, KaryJSON karyJSON, CaseDao caseDao, ModulManager modulManager, CommandExecute commandExecute, UserDao userDao, ModLog modLog, NieobecnosciDao nieobecnosciDao, RemindDao remindDao, GiveawayDao giveawayDao, StatsModule statsModule, MusicModule musicModule, MultiDao multiDao, TicketDao ticketDao, ApelacjeDao apelacjeDao, AnkietaDao ankietaDao, EmbedRedisManager embedRedisManager, WeryfikacjaDao weryfikacjaDao, WeryfikacjaModule weryfikacjaModule, RecordingDao recordingDao, SocketManager socketManager, DeletedMessagesDao deletedMessagesDao, AcBanDao acBanDao, UserstatsManager userstatsManager) {
        this.commandManager = commandManager;
        this.tlumaczenia = tlumaczenia;
        this.api = api;
        this.eventWaiter = eventWaiter;
        this.karyJSON = karyJSON;
        this.caseDao = caseDao;
        this.modulManager = modulManager;
        this.commandExecute = commandExecute;
        this.userDao = userDao;
        this.modLog = modLog;
        this.nieobecnosciDao = nieobecnosciDao;
        this.remindDao = remindDao;
        this.giveawayDao = giveawayDao;
        this.statsModule = statsModule;
        this.musicModule = musicModule;
        this.multiDao = multiDao;
        this.ticketDao = ticketDao;
        this.apelacjeDao = apelacjeDao;
        this.ankietaDao = ankietaDao;
        this.embedRedisManager = embedRedisManager;
        this.weryfikacjaDao = weryfikacjaDao;
        this.weryfikacjaModule = weryfikacjaModule;
        this.recordingDao = recordingDao;
        this.socketManager = socketManager;
        this.deletedMessagesDao = deletedMessagesDao;
        this.acBanDao = acBanDao;
        this.userstatsManager = userstatsManager;

        ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
        executorSche.scheduleAtFixedRate(() -> tak(api), 0, 5, TimeUnit.MINUTES);
    }

    @Override
    public boolean startUp() {
        giveawayListener = new GiveawayListener(giveawayDao, api);
        kolkoIKrzyzykManager = new KolkoIKrzyzykManager(api, eventWaiter);
        guildListener = new GuildListener();

        api.addEventListener(guildListener);

        cmd = new ArrayList<>();

        cmd.add(new PingCommand());
        cmd.add(new BotinfoCommand(commandManager, modulManager, socketManager));
        cmd.add(new HelpCommand(commandManager));
        cmd.add(new PoziomCommand());
        cmd.add(new EvalCommand(eventWaiter, commandManager, caseDao, modLog, karyJSON, tlumaczenia, commandExecute, userDao, nieobecnosciDao, remindDao, modulManager, giveawayListener, giveawayDao, statsModule, multiDao, musicModule, ticketDao, apelacjeDao, ankietaDao, embedRedisManager, weryfikacjaDao, weryfikacjaModule, socketManager, deletedMessagesDao, acBanDao, userstatsManager));
        cmd.add(new ForumCommand());
        cmd.add(new UserinfoCommand());
        cmd.add(new McpremiumCommand());
        cmd.add(new RemindmeCommand(remindDao, eventWaiter));
        cmd.add(new ModulesCommand(modulManager));
        cmd.add(new CytujCommand());
        cmd.add(new GiveawayCommand(giveawayDao, eventWaiter, giveawayListener));
        cmd.add(new RebootCommand());
        cmd.add(new ShellCommand());
        cmd.add(new ArchiwizujCommand());
        cmd.add(new PogodaCommand());
        cmd.add(new KolkoIKrzyzykCommand(kolkoIKrzyzykManager));
        cmd.add(new RecordingCommand(recordingDao, eventWaiter));
        cmd.add(new SasinCommand());
        cmd.add(new StatsCommand(userstatsManager.userstatsDao));

        cmd.forEach(commandManager::registerCommand);
        setStart(true);
        return true;
    }

    private void tak(ShardManager api) {
        RemindmeCommand.check(remindDao, api);
        TextChannel txt = api.getTextChannelById(Ustawienia.instance.channel.status);
        if (txt == null) throw new NullPointerException("Kanal do statusu jest nullem");
        Message botMsg = null;
        MessageHistory history = txt.getHistoryFromBeginning(15).complete();
        if (history.isEmpty()) return;

        for (Message message : history.getRetrievedHistory()) {
            if (message.getAuthor().getId().equals(Ustawienia.instance.bot.botId)) {
                botMsg = message;
                break;
            }
        }

        if (botMsg != null) {
            try {
                String c = StatusCommand.getMsg(null, null, null, botMsg.getContentRaw());
                botMsg.editMessage(c).complete();
            } catch (Exception e) {
                Log.newError(e, getClass());
            }
        }

    }

    @Override
    public boolean shutDown() {
        kolkoIKrzyzykManager.stop();
        api.removeEventListener(guildListener);
        commandManager.unregisterCommands(cmd);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "commands";
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }

    @Data
    @AllArgsConstructor
    public static class KaraJSON {
        private final int id;
        private final String powod;
    }

    @Data
    @AllArgsConstructor
    public static class WarnJSON {
        private final int warns;
        private final String kara;
        private final String czas;
    }

}