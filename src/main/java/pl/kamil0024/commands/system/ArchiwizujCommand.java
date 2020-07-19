package pl.kamil0024.commands.system;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import pl.kamil0024.core.command.Command;
import pl.kamil0024.core.command.CommandContext;
import pl.kamil0024.core.command.enums.PermLevel;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ArchiwizujCommand extends Command {

    private final static String CATEGORY = "573855098250461185";

    public ArchiwizujCommand() {
        name = "archiwizuj";
        permLevel = PermLevel.DEVELOPER;
    }

    @Override
    public boolean execute(CommandContext context) {
        TextChannel txt = context.getParsed().getTextChannel(context.getArgs().get(0));
        Category cate = context.getGuild().getCategoryById(CATEGORY);
        if (cate == null) throw new NullPointerException("Kategoria do archiwum jest nullem");

        if (txt == null) {
            context.send("Nie ma takiego kanału!").queue();
            return true;
        }

        if (!context.getGuild().getSelfMember().hasPermission(txt, Permission.MANAGE_CHANNEL)) {
            context.send("Nie mam permisji do zarządzania kanałem " + txt.getAsMention() + " !").queue();
            return false;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy");
        txt.getManager().setName(sdf.format(new Date()) + txt.getName()).complete();
        txt.getManager().setParent(cate).complete();

        for (PermissionOverride permissionOverride : cate.getPermissionOverrides()) {
            txt.getManager().putPermissionOverride(permissionOverride.getPermissionHolder(), permissionOverride.getAllowedRaw(),permissionOverride.getDeniedRaw()).queue();
        }

        context.send("Pomyślnie zarchiwizowano!").queue();
        return true;
    }

}
