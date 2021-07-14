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

package pl.kamil0024.core.arguments;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.jetbrains.annotations.Nullable;
import pl.kamil0024.core.command.CommandContext;

public class MemberArgument extends Args {

    public MemberArgument() {
        name = "member";
    }

    @Override
    public Member parsed(String s, @Nullable JDA jda, CommandContext context) {
        if (jda == null) throw new UnsupportedOperationException("jda==null");
        String prawieUser = String.valueOf(s);
        if (prawieUser == null) return null;
        try {
            Member member;
            try {
                member = context.getGuild().retrieveMemberById(prawieUser).complete();
            } catch (ErrorResponseException e) {
                if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER || e.getErrorResponse() == ErrorResponse.UNKNOWN_USER)
                    member = null;
                else throw e;
            }
            if (member != null) return member;
            if (!context.getEvent().getMessage().getMentionedMembers().isEmpty() && context.getEvent().getMessage().getMentionedMembers().get(0) != null)
                return context.getEvent().getMessage().getMentionedMembers().get(0);
        } catch (Exception ignored) {
            if (!context.getEvent().getMessage().getMentionedMembers().isEmpty() && context.getEvent().getMessage().getMentionedMembers().get(0) != null)
                return context.getEvent().getMessage().getMentionedMembers().get(0);
        }
        return null;
    }

}
