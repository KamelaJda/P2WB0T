package pl.kamil0024.commands;

import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.commands.listener.GiveawayListener;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandExecute;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.module.ModulManager;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.core.util.Tlumaczenia;
import pl.kamil0024.core.util.kary.KaryJSON;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.stats.StatsModule;
import pl.kamil0024.commands.dews.*;
import pl.kamil0024.commands.moderation.*;
import pl.kamil0024.commands.system.*;
import pl.kamil0024.core.database.*;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CommandsModule
implements Modul {

        private ArrayList<Command> cmd;

        @Inject CommandManager commandManager;
        @Inject Tlumaczenia tlumaczenia;
        @Inject ShardManager api;
        @Inject EventWaiter eventWaiter;
        @Inject KaryJSON karyJSON;
        @Inject CaseDao caseDao;
        @Inject ModulManager modulManager;
        @Inject CommandExecute commandExecute;
        @Inject UserDao userDao;
        @Inject NieobecnosciDao nieobecnosciDao;
        @Inject RemindDao remindDao;
        @Inject GiveawayDao giveawayDao;
        @Inject StatsModule statsModule;
        @Inject MusicModule musicModule;

        private boolean start = false;
        private ModLog modLog;

    public CommandsModule(CommandManager commandManager, Tlumaczenia tlumaczenia, ShardManager api, EventWaiter eventWaiter, KaryJSON karyJSON, CaseDao caseDao, ModulManager modulManager, CommandExecute commandExecute, UserDao userDao, ModLog modLog, NieobecnosciDao nieobecnosciDao, RemindDao remindDao, GiveawayDao giveawayDao, StatsModule statsModule, MusicModule musicModule) {
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

            ScheduledExecutorService executorSche = Executors.newSingleThreadScheduledExecutor();
            executorSche.scheduleAtFixedRate(this::tak, 0, 5, TimeUnit.MINUTES);
        }

        @Override
        public boolean startUp() {
            GiveawayListener giveawayListener = new GiveawayListener(giveawayDao, api);

            cmd = new ArrayList<>();

            cmd.add(new PingCommand());
            cmd.add(new BotinfoCommand(commandManager, modulManager));
            cmd.add(new HelpCommand(commandManager));
            cmd.add(new PoziomCommand());
            cmd.add(new EvalCommand(eventWaiter, commandManager, caseDao, modLog, karyJSON, tlumaczenia, commandExecute, userDao, nieobecnosciDao, remindDao, modulManager, giveawayListener, giveawayDao, statsModule));
            cmd.add(new ForumCommand());
            cmd.add(new UserinfoCommand());
            cmd.add(new McpremiumCommand());
            cmd.add(new RemindmeCommand(remindDao, eventWaiter));
            cmd.add(new ModulesCommand(modulManager));
            cmd.add(new ClearCommand(statsModule));
            cmd.add(new CytujCommand());
            cmd.add(new CheckCommand(caseDao));
            cmd.add(new GiveawayCommand(giveawayDao, eventWaiter, giveawayListener));
            cmd.add(new StopCommand(modulManager, statsModule, eventWaiter, musicModule));
            cmd.add(new ShellCommand());
            cmd.add(new ArchiwizujCommand());

            // Moderacyjne:
            cmd.add(new StatusCommand(eventWaiter));
            cmd.add(new KarainfoCommand(caseDao));
            cmd.add(new UnmuteCommand(caseDao, modLog));
            cmd.add(new TempmuteCommand(caseDao, modLog, statsModule));
            cmd.add(new PunishCommand(karyJSON, eventWaiter, caseDao, modLog, statsModule));
            cmd.add(new KickCommand(caseDao, modLog, statsModule));
            cmd.add(new BanCommand(caseDao, modLog, statsModule));
            cmd.add(new TempbanCommand(caseDao, modLog, statsModule));
            cmd.add(new UnbanCommand(caseDao, modLog));
            cmd.add(new MuteCommand(caseDao, modLog, statsModule));
            cmd.add(new HistoryCommand(caseDao, eventWaiter));

            cmd.forEach(commandManager::registerCommand);
            setStart(true);
            return true;
        }

        private void tak() {
            RemindmeCommand.check(remindDao, api);
        }

        @Override
        public boolean shutDown() {
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
        public class KaraJSON {
            private final int id;
            private final String powod;
        }

        @Data
        @AllArgsConstructor
        public class WarnJSON {
            private final int warns;
            private final String kara;
            private final String czas;
        }
}
