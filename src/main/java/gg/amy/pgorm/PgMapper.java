package gg.amy.pgorm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gg.amy.pgorm.annotations.BtreeIndex;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.Getter;
import pl.kamil0024.core.logger.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NOTE: The JSONB data column is always named <code>data</code>.
 *
 * @author amy
 * @since 4/10/18.
 */
@SuppressWarnings({"WeakerAccess", "unused", "squid:S1192", "DuplicatedCode"})
public class PgMapper<T> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Getter
    private final Class<T> type;
    @Getter
    private final PgStore store;

    private Field pkField;
    private Table table;
    private PrimaryKey primaryKey;

    public PgMapper(final PgStore store, final Class<T> type) {
        this.store = store;
        this.type = type;
        init();
    }

    private void init() {
        // Scan to ensure required annotations
        if(!type.isAnnotationPresent(Table.class)) {
            throw new IllegalStateException("Got class " + type.getName() + " to map, but it has no @Table!?");
        }
        table = type.getDeclaredAnnotation(Table.class);
        // Scan the class for a primary key
        boolean havePk = false;
        pkField = null;
        for(final Field field : type.getDeclaredFields()) {
            field.setAccessible(true);
            if(field.isAnnotationPresent(PrimaryKey.class)) {
                havePk = true;
                pkField = field;
            }
        }
        if(!havePk) {
            throw new IllegalStateException("Class " + type.getName() + " has no @PrimaryKey!?");
        }
        // Ensure that it's a valid type
        final String sqlType = typeToSqlType(pkField.getType());
        primaryKey = pkField.getDeclaredAnnotation(PrimaryKey.class);
        // Create the table
        store.sql("CREATE TABLE IF NOT EXISTS " + table.value() + " (" +
                primaryKey.value() + ' ' + sqlType + " PRIMARY KEY NOT NULL UNIQUE," +
                "data JSONB" +
                ");");
        Log.info("Created table %s for entity class %s.", table.value(), type.getName());
        // Create the indexes
        if(type.isAnnotationPresent(BtreeIndex.class)) {
            final BtreeIndex btreeIndex = type.getDeclaredAnnotation(BtreeIndex.class);
            for(final String s : btreeIndex.value()) {
                final String idx = "idx_btree_" + table.value() + '_' + s;
                store.sql("CREATE INDEX IF NOT EXISTS " + idx + " ON " + table.value() + " USING BTREE ((data->'" + s + "'));");
                Log.info("Created index %s on %s for entity class %s.", idx, table.value(), type.getName());
            }
        }
        // Make base GIN index
        final String dataGinIdx = "idx_gin_" + table.value() + "_data";
        store.sql("CREATE INDEX IF NOT EXISTS " + dataGinIdx + " ON " + table.value() + " USING GIN (data);");
        Log.info("Created index idx_gin_data on %s for entity class %s.", table.value(), type.getName());
        if(type.isAnnotationPresent(GIndex.class)) {
            final GIndex gin = type.getDeclaredAnnotation(GIndex.class);
            for(final String s : gin.value()) {
                final String idx = "idx_gin_" + table.value() + '_' + s;
                store.sql("CREATE INDEX IF NOT EXISTS " + idx + " ON " + table.value() + " USING GIN ((data->'" + s + "'));");
                Log.info("Created index %s on %s for entity class %s.", idx, table.value(), type.getName());
            }
        }
    }

    public void save(final T entity) {
        pkField.setAccessible(true);
        try {
            final Object pk = pkField.get(entity);
            // Map the object to JSON
            final String json = MAPPER.writeValueAsString(entity);
            // Oh god this is so ugly
            store.sql("INSERT INTO " + table.value() + " (" + primaryKey.value() + ", data) values (?, to_jsonb(?::jsonb)) " +
                    "ON CONFLICT (" + primaryKey.value() + ") DO UPDATE SET " + primaryKey.value() + " = ?, data = to_jsonb(?::jsonb);", c -> {
                c.setObject(1, pk);
                c.setString(2, json);
                c.setObject(3, pk);
                c.setString(4, json);
                c.execute();
            });
        } catch(final IllegalAccessException e) {
            Log.error("Couldn't access primary key for entity %s (value: %s): %s", type.getName(), entity, e);
        } catch(final JsonProcessingException e) {
            Log.error("Couldn't map entity %s (value: %s) to JSON: %s", type.getName(), entity, e);
        }
    }

    public Optional<T> load(final Object pk) {
        final OptionalHolder result = new OptionalHolder();
        store.sql("SELECT * FROM " + table.value() + " WHERE " + primaryKey.value() + " = ?;", c -> {
            c.setObject(1, pk);
            final ResultSet resultSet = c.executeQuery();
            if(resultSet.isBeforeFirst()) {
                resultSet.next();
                try {
                    result.setValue(loadFromResultSet(resultSet));
                } catch(final IllegalStateException e) {
                    Log.error("Load error: %s", e);
                    // Optional API says this will return Optional.empty()
                    result.setValue(null);
                }
            }
        });
        return result.value;
    }

    /**
     * This is a slightly-weird thing, but it makes sense given the kind of
     * use-case I have. <p/>
     * Suppose you have many documents like this:
     * <pre>
     * {
     *     "id": 123456789,
     *     "data": "henlo world",
     *     "type": "type.whatever"
     * }
     * </pre>
     * and you want to select all documents with type {@code type.whatever}.
     * This can't be done nicely without writing SQL statements directly in
     * your application, which is ugly in my opinion. To solve this, there has
     * to be a method that allows loading many objects by a key's value in the
     * JSONB data that gets stored, hence why this method exists.
     *
     * @param subKey     The subkey to query on. Ex. {@code data->'type'}.
     * @param subKeyData The subkey data to search for. Ex.
     *                   {@code type.whatever}.
     *
     * @return A list of {@code <T>}s that has the given value for the given
     * subkey.
     */
    public List<T> loadManyBySubkey(final String subKey, final String subKeyData) {
        final List<T> data = new ArrayList<>();
        store.sql("SELECT * FROM " + table.value() + " WHERE " + subKey + " = ?;", c -> {
            c.setObject(1, subKeyData);
            final ResultSet resultSet = c.executeQuery();
            if(resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> loadManyBySubkey(final String subKey, final String subKeyData, final int limit) {
        final List<T> data = new ArrayList<>();
        store.sql("SELECT * FROM " + table.value() + " WHERE " + subKey + " = ? LIMIT " + limit + ";", c -> {
            c.setObject(1, subKeyData);
            final ResultSet resultSet = c.executeQuery();
            if(resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> loadAll() {
        final List<T> data = new ArrayList<>();
        store.sql("SELECT * FROM " + table.value() + ";", c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> loadWhere(String where) {
        final List<T> data = new ArrayList<>();
        store.sql("SELECT * FROM " + table.value() + ";", c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> sum(final String columnName, final String where) {
        final List<T> data = new ArrayList<>();
        store.sql("SELECT SUM(" + columnName + ") FROM " + table.value() + " WHERE " + where + " ;", c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public Optional<Boolean> delete(final Object pk) {
        AtomicReference<Optional<Boolean>> result = new AtomicReference<>(Optional.empty());
        store.sql("DELETE FROM " + table.value() + " WHERE " + primaryKey.value() + " = ?;", c -> {
            c.setObject(1, pk);
            result.set(Optional.of(c.execute()));
        });
        return result.get();
    }

    public Optional<Boolean> delete(final int id) {
        AtomicReference<Optional<Boolean>> result = new AtomicReference<>(Optional.empty());
        store.sql("DELETE FROM " + table.value() + " WHERE " + primaryKey.value() + " = '"+ id +"';");
        return result.get();
    }

    public List<T> loadRaw(String sqlBase, String... arguments) {
        final List<T> data = new ArrayList<>();
        store.sql(String.format(sqlBase, table.value()), c -> {
            int i = 0;
            for (String arg : arguments) {
                i++;
                c.setObject(i, arg);
            }
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public T loadFromResultSet(final ResultSet resultSet) {
        try {
            final String json = resultSet.getString("data");
            try { //NOSONAR
                return MAPPER.readValue(json, type);
            } catch(final IOException e) {
                Log.error("Couldn't load entity %s from JSON %s: %s", type.getName(), json, e);
                throw new IllegalStateException("Couldn't load entity " + type.getName() + " from JSON " + json, e);
            }
        } catch(final SQLException e) {
            Log.error("Couldn't load entity %s from JSON: %s", type.getName(), e);
            throw new IllegalStateException("Couldn't load entity " + type.getName(), e);
        }
    }

    private String typeToSqlType(final Class<?> type) {
        if(type.equals(String.class)) {
            return "TEXT";
        } else if(type.equals(Integer.class) || type.equals(int.class)) {
            return "INT";
        } else if(type.equals(Long.class) || type.equals(long.class)) {
            return "BIGINT";
        } else if(type.equals(java.sql.Date.class)) {
            return "DATE";
        } else {
            throw new IllegalArgumentException("No SQL type mapping known for class of type: " + type.getName());
        }
    }

    public List<T> getAktywne(String karanyId) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"aktywna\": true, \"karanyId\": \"%s\"} }';", table.value(), karanyId);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getMcAktywne(String nick) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"aktywna\": true, \"mcNick\": \"%s\"} }';", table.value(), nick);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllAktywne() {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"aktywna\": true}}' AND (data::jsonb @> '{\"kara\": {\"typKary\": \"TEMPBAN\"}}' OR data::jsonb @> '{\"kara\": {\"typKary\": \"TEMPMUTE\"}}');", table.value());
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllPunAktywne(String karanyId) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"punAktywna\": true, \"karanyId\": \"%s\"} }';", table.value(), karanyId);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllNick(String nick) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"mcNick\": \"%s\"} }';", table.value(), nick);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAll(String userId) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"karanyId\": \"%s\"} }';", table.value(), userId);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getDescKary(String userId, int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"kara\": {\"karanyId\": \"%s\"}}' ORDER BY cast(data->>'id' as integer) DESC LIMIT 5 OFFSET %d;", table.value(), userId, offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public String getTableName() {
        return table.value();
    }

    public String getPrimaryKeyName() {
        return primaryKey.value();
    }

    public List<T> getTicketById(String userId, int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"userId\": \"%s\"}' LIMIT 10 OFFSET %d;", table.value(), userId, offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getTicketByNick(String nick, int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"userNick\": \"%s\"}' ORDER BY data->>'createdTime' DESC LIMIT 10 OFFSET %d;", table.value(), nick, offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public T getTicketBySpam(String userId) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"userId\": \"%s\", \"spam\": true}';", table.value(), userId);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        if (data.isEmpty()) {
            return null;
        }
        return data.get(0);
    }

    public List<T> getTicketsSpam(int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"spam\": true}' ORDER BY data->>'createdTime' DESC LIMIT 10 OFFSET %d;", table.value(), offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        if (data.isEmpty()) {
            return null;
        }
        return data;
    }

    public List<T> getAllTickets(int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE NOT(data::jsonb @> '{\"ocena\": -1}') AND data::jsonb @> '{\"spam\": false}' ORDER BY data->>'createdTime' DESC LIMIT 10 OFFSET %d;", table.value(), offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }
    
    public List<T> getAllTicketsByFiltr(int offset, String admId, boolean read) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE NOT(data::jsonb @> '{\"ocena\": -1}') AND data::jsonb @> '{\"spam\": false}' AND (data->>'readBy')::jsonb ?? '%s' ORDER BY data->>'createdTime';", table.value(), admId);
        if (!read) {
            msg = String.format("SELECT * FROM %s WHERE NOT(data::jsonb @> '{\"ocena\": -1}') AND data::jsonb @> '{\"spam\": false}' AND NOT((data->>'readBy')::jsonb ?? '%s') ORDER BY data->>'createdTime';", table.value(), admId);
        }
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllApelacje(int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s ORDER BY data->>'createdTime' DESC LIMIT 10 OFFSET %d;", table.value(), offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllAcBan(int offset, boolean seeReaded) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"readed\": " + seeReaded + "}' ORDER BY data->>'createdTime' ASC LIMIT 10 OFFSET %d;", table.value(), offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllApelacjeByNick(String nick, int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s WHERE data::jsonb @> '{\"apelacjeNick\": \"%s\"}' ORDER BY data->>'createdTime' DESC LIMIT 10 OFFSET %d;", table.value(), nick, offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    public List<T> getAllLogs(int offset) {
        final List<T> data = new ArrayList<>();
        String msg = String.format("SELECT * FROM %s ORDER BY data->>'deletedDate' DESC LIMIT 10 OFFSET %d;", table.value(), offset);
        store.sql(msg, c -> {
            final ResultSet resultSet = c.executeQuery();
            if (resultSet.isBeforeFirst()) {
                while(resultSet.next()) {
                    try {
                        data.add(loadFromResultSet(resultSet));
                    } catch(final IllegalStateException e) {
                        Log.error("Load error: %s", e);
                    }
                }
            }
        });
        return data;
    }

    // Ugly hack to allow bringing an optional out of a lambda
    private final class OptionalHolder {
        // This is intentionally done. . _.
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private Optional<T> value = Optional.empty();

        private void setValue(final T data) {
            value = Optional.ofNullable(data);
        }
    }

}
