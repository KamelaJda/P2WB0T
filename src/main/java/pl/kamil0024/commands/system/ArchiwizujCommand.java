package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.ChannelManager;
import pl.kamil0024.core.Ustawienia;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;
import pl.kamil0024.core.logger.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ArchiwizujCommand extends Command {

    public ArchiwizujCommand() {
        name = "archiwizuj";
        permLevel = PermLevel.DEVELOPER;
    }

    @Override
    public boolean execute(CommandContext context) {
        TextChannel txt = context.getParsed().getTextChannel(context.getArgs().get(0));
        Category cate = context.getGuild().getCategoryById(Ustawienia.instance.inne.kategoriaArchiwum);
        if (cate == null) throw new NullPointerException("Kategoria do archiwum jest nullem");

        if (txt == null) {
            context.send("Nie ma takiego kanału!").queue();
            return true;
        }

        if (!context.getGuild().getSelfMember().hasPermission(txt, Permission.MANAGE_CHANNEL)) {
            context.send("Nie mam permisji do zarządzania kanałem " + txt.getAsMention() + " !").queue();
            return false;
        }

        Log.debug("1");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy");
        String name = sdf.format(new Date()) + "-" + txt.getName();

        ChannelManager manager = txt.getManager().clearOverridesAdded().clearOverridesRemoved().setParent(cate).setName(name);
        Log.debug("2");

        for (PermissionOverride permissionOverride : cate.getPermissionOverrides()) {
            if (permissionOverride.getPermissionHolder() != null) {
                Log.debug("nie jest nullem");
                manager = manager.putPermissionOverride(permissionOverride.getPermissionHolder(), permissionOverride.getAllowed(), permissionOverride.getDenied());
            }
        }
        Log.debug("3");
        manager.complete();
        Log.debug("4");

        context.send("Pomyślnie zarchiwizowano!").queue();
        return true;
    }

}
