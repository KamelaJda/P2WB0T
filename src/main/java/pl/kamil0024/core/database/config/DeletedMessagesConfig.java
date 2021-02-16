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

package pl.kamil0024.core.database.config;

import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import pl.kamil0024.logs.logger.FakeMessage;

@Table("deletedmessage")
@GIndex({"userId", "channelId", "createdDate", "deletedDate"})
@Data
@AllArgsConstructor
public class DeletedMessagesConfig {
    public DeletedMessagesConfig() { }

    public DeletedMessagesConfig(String id) {
        this.id = id;
    }

    @PrimaryKey
    private String id = "";

    private long createdDate;
    private long deletedDate;
    private String userId;
    private String channelId;
    private String content;

    public static DeletedMessagesConfig convert(FakeMessage msg, long deletedDate) {
        return new DeletedMessagesConfig(msg.getId(), msg.getCreatedAt().toInstant().toEpochMilli(), deletedDate, msg.getAuthor(), msg.getChannel(), msg.getContent());
    }

}
