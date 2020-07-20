package pl.kamil0024.stats;

import com.google.inject.Inject;
import lombok.Getter;
import net.dv8tion.jda.api.sharding.ShardManager;
import pl.kamil0024.bdate.BDate;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandManager;
import pl.kamil0024.core.database.NieobecnosciDao;
import pl.kamil0024.core.database.StatsDao;
import pl.kamil0024.core.database.config.StatsConfig;
import pl.kamil0024.core.module.Modul;
import pl.kamil0024.core.util.EventWaiter;
import pl.kamil0024.music.MusicModule;
import pl.kamil0024.nieobecnosci.config.Nieobecnosc;
import pl.kamil0024.stats.commands.StatsCommand;
import pl.kamil0024.stats.commands.TekstCommand;
import pl.kamil0024.stats.commands.TopCommand;
import pl.kamil0024.stats.entities.StatsCache;
import pl.kamil0024.stats.entities.Statystyka;
import pl.kamil0024.stats.listener.StatsListener;

import java.util.ArrayList;

public class StatsModule implements Modul {

    private ArrayList<Command> cmd;

    @Inject CommandManager commandManager;
    @Inject ShardManager api;
    @Inject EventWaiter eventWaiter;
    @Inject StatsDao statsDao;
    @Inject MusicModule musicModule;
    @Inject NieobecnosciDao nieobecnosciDao;

    private boolean start = false;

    @Getter public StatsCache statsCache;

    StatsListener statsListener;

    public StatsModule(CommandManager commandManager, ShardManager api, EventWaiter eventWaiter, StatsDao statsDao, MusicModule musicModule, NieobecnosciDao nieobecnosciDao) {
        this.commandManager = commandManager;
        this.api = api;
        this.eventWaiter = eventWaiter;
        this.statsDao = statsDao;
        this.statsCache = new StatsCache(statsDao);
        this.musicModule = musicModule;
        this.nieobecnosciDao = nieobecnosciDao;

        for (StatsConfig statsConfig : statsDao.getAll()) {
            Statystyka s = StatsConfig.getStatsFromDay(statsConfig.getStats(), new BDate().getDateTime().getDayOfYear());
            if (s == null) continue;
            statsCache.save(statsConfig.getId(), s);
        }

    }

    @Override
    public boolean startUp() {
        this.statsListener = new StatsListener(this);
        api.addEventListener(statsListener);
        cmd = new ArrayList<>();

        cmd.add(new StatsCommand(statsDao));
        cmd.add(new TopCommand(statsDao, eventWaiter, nieobecnosciDao));
        cmd.add(new TekstCommand(eventWaiter, musicModule));

        cmd.forEach(commandManager::registerCommand);
        setStart(true);
        return true;
    }

    @Override
    public boolean shutDown() {
        commandManager.unregisterCommands(cmd);
        api.removeEventListener(statsListener);
        setStart(false);
        return true;
    }

    @Override
    public String getName() {
        return "stats";
    }

    @Override
    public boolean isStart() {
        return start;
    }

    @Override
    public void setStart(boolean bol) {
        this.start = bol;
    }

}
