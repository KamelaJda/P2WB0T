package pl.kamil0024.commands.zabawa;

import net.dv8tion.jda.api.entities.Member;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.util.UsageException;

public class KolkoIKrzyzykCommand extends Command {

    public KolkoIKrzyzykCommand() {
        name = "kolkoikrzyzyk";
        aliases.add("kolko");
        aliases.add("krzyzyk");
        cooldown = 30;
    }

    @Override
    public boolean execute(CommandContext context) {
        String arg = context.getArgs().get(0);
        if (arg == null) throw new UsageException();

        if (arg.toLowerCase().equals("akceptuj")) {
            // soon
        } else {
            Member member = context.getParsed().getMember(context.getArgs().get(0));
            if (member == null) {
                context.send("Nie ma takiego użytkownika!").queue();
                return false;
            }
            if (member.getId().equals(context.getUser().getId())) {
                context.send("Kolegów nie masz, że musisz siebie do gry zapraszać? xD").queue();
                return false;
            }

        }

        return true;
    }

}
