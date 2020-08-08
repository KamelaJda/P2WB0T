package pl.kamil0024.music.commands.privates;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import pl.kamil0024.commands.system.HelpCommand;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.logger.Log;
import pl.kamil0024.core.musicapi.MusicAPI;
import pl.kamil0024.core.musicapi.MusicResponse;
import pl.kamil0024.core.musicapi.MusicRestAction;
import pl.kamil0024.music.commands.PlayCommand;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class PrivatePlayCommand extends Command {

    private MusicAPI musicAPI;

    public PrivatePlayCommand(MusicAPI musicAPI) {
        name = "pplay";
        aliases.add("privateplay");
        this.musicAPI = musicAPI;
    }

    @Override
    public boolean execute(CommandContext context) {
        if (!check(context)) return false;

        String link = context.getArgs().get(0);
        if (link == null) {
            context.send(HelpCommand.getUsage(context).build()).queue();
            return false;
        }

        int wolnyBot = 0;
        MusicRestAction restAction = null;

        for (Member member : PlayCommand.getVc(context.getMember()).getMembers()) {
            if (member.getUser().isBot()) {
                Integer agent = musicAPI.getPortByClient(member.getId());
                if (agent != null) {
                    wolnyBot = agent;
                    restAction = musicAPI.getAction(agent);
                }

            }
        }

        if (wolnyBot == 0 && restAction != null) {
            for (Integer port : musicAPI.getPorts()) {
                restAction = musicAPI.getAction(port);
                if (restAction.getVoiceChannel() == null) {
                    wolnyBot = port;
                    break;
                }
            }
        }

        if (wolnyBot == 0) {
            context.send("Aktualnie nie ma wolnych botów.").queue();
            return false;
        }

        try {
            restAction.connect(PlayCommand.getVc(context.getMember()));
        } catch (Exception e) {
            context.send("Nie udało się dołączyć na kanał głosowy.").queue();
            return false;
        }

        try {
            MusicResponse play = restAction.play(link.split("v=")[1]);
            if (play.isError()) {
                context.send("Nie udało się odtworzyć piosenki!").queue();
                // TODO: Jeżeli queue.size() == 0 to disconnect();
                return false;
            } else {
                context.send("Pomyślnie dodano piosenkę do kolejki!").queue();
                return false;
            }
        } catch (IOException e) {
            context.send("Wystąpił błąd z API! " + e.getLocalizedMessage());
            Log.newError(e);
        }

        return true;
    }

    public static boolean check(CommandContext context) {
        if (!PlayCommand.isVoice(context.getMember())) {
            context.sendTranslate("pplay.no.channel").queue();
            return false;
        }
        VoiceChannel vc = PlayCommand.getVc(context.getMember());
        if (vc.getParent() == null || !vc.getParent().getName().toLowerCase().contains("prywatne kanały")) {
            context.sendTranslate("pplay.no.private").queue();
            return false;
        }


        List<Member> members = vc.getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList());
        int size = members.size();

//        if (size < 4) {
//            context.sendTranslate("pplay.min.members").queue();
//            return false;
//        }
        if (!context.getMember().hasPermission(vc, Permission.MANAGE_CHANNEL)) {
            context.sendTranslate("pplay.no.channel.owner").queue();
            return false;
        }

        return true;
    }

}