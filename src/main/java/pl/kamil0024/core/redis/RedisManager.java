/*
 * Copyright (C) 2019-2020 FratikB0T Contributors
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.kamil0024.core.redis;

import com.google.common.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import pl.kamil0024.core.util.GsonUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class RedisManager {

    private final String PREFIX;
    @Getter(AccessLevel.PACKAGE)
    private final JedisPool jedisPool;
    private final ExecutorService executor = Executors.newFixedThreadPool(4, new RedisThreadFactory());

    public RedisManager(long id) {
        GenericObjectPoolConfig pc = new GenericObjectPoolConfig();
        pc.setMaxIdle(16);
        pc.setMaxIdle(32);
        pc.setMaxIdle(32);
        PREFIX = "P2WB0T-" + id;
        this.jedisPool = new JedisPool(pc, "localhost");
    }

    public <T> T get(String key, TypeToken<T> holds, Function<? super String, ? extends T> mappingFunction) {
        return get(key, holds, mappingFunction, 300);
    }

    public <T> T get(String key, TypeToken<T> holds) {
        return getRaw(getDbkey(key, holds), holds);
    }

    public <T> T getRaw(String dbkey, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dane = jedis.get(dbkey);
            if (dane == null) return null;
            return GsonUtil.fromJSON(dane, holds.getType());
        }
    }

    public <T> T get(String key, TypeToken<T> holds, Function<? super String, ? extends T> mappingFunction, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = getDbkey(key, holds);
            String dane = jedis.get(dbkey);
            if (dane == null) {
                T v = mappingFunction.apply(key);
                jedis.set(dbkey, GsonUtil.toJSON(v));
                if (expiry > 0) {
                    scheduleAsync(() -> {
                        try (Jedis j = jedisPool.getResource()) {
                            j.expire(dbkey, expiry);
                        }
                    });
                }
                return v;
            }
            return GsonUtil.fromJSON(dane, holds.getType());
        }
    }

    public <T> void putAll(TypeToken<T> holds, Map<? extends String, ? extends T> map) {
        putAll(holds, map, 300);
    }

    public <T> void putAll(TypeToken<T> holds, Map<? extends String, ? extends T> map, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (Map.Entry<? extends String, ? extends T> ent : map.entrySet()) {
                String dbkey = getDbkey(ent.getKey(), holds);
                jedis.set(dbkey, GsonUtil.toJSON(ent.getValue()));
                if (expiry > 0) {
                    scheduleAsync(() -> {
                        try (Jedis j = jedisPool.getResource()) {
                            j.expire(dbkey, expiry);
                        }
                    });
                }
            }
        }
    }

    public <T> void put(String key, TypeToken<T> holds, T value) {
        put(key, holds, value, 300);
    }

    public <T> void put(String key, TypeToken<T> holds, T value, int expiry) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = getDbkey(key, holds);
            jedis.set(dbkey, GsonUtil.toJSON(value));
            if (expiry > 0) {
                scheduleAsync(() -> {
                    try (Jedis j = jedisPool.getResource()) {
                        j.expire(dbkey, expiry);
                    }
                });
            }
        }
    }

    private void scheduleAsync(Runnable r) {
        executor.execute(r);
    }

    public <T> void invalidate(Object key, Class<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = getDbkey(key, holds);
            jedis.del(dbkey);
        }
    }

    public <T> void invalidate(Object key, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = getDbkey(key, holds);
            jedis.del(dbkey);
        }
    }

    public <T> void invalidateAll(Iterable<?> keys, TypeToken<T> holds) {
        List<String> str = new ArrayList<>();
        for (Object key : keys) {
            String dbkey = getDbkey(key, holds);
            str.add(dbkey);
        }
        invalidateAllRaw(str);
    }

    public void invalidateAllRaw(Iterable<?> dbKeys) {
        List<String> str = new ArrayList<>();
        for (Object dbkey : dbKeys)
            str.add(dbkey.toString());
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(str.toArray(new String[]{}));
        }
    }

    public <T> long ttl(Object key, TypeToken<T> holds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String dbkey = getDbkey(key, holds);
            return jedis.ttl(dbkey);
        }
    }

    @NotNull
    private <T> String getDbkey(Object key, Class<T> holds) {
        return PREFIX + "::" + holds.getSimpleName() + ":" + key;
    }

    @NotNull
    private <T> String getDbkey(Object key, TypeToken<T> holds) {
        StringBuilder tak = new StringBuilder(PREFIX + "::" + holds.getRawType().getSimpleName());
        if (holds.getType() instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) holds.getType()).getActualTypeArguments();
            if (args.length > 0) {
                for (Type t : args) tak.append(":").append(resolveTypeSimpleName(t));
            }
        }
        return tak.append(":").append(key).toString();
    }

    private String resolveTypeSimpleName(Type t) {
        if (t instanceof Class) return ((Class<?>) t).getSimpleName();
        else if (t instanceof ParameterizedType) {
            StringBuilder tak = new StringBuilder();
            Type[] args = ((ParameterizedType) t).getActualTypeArguments();
            if (args.length > 0) {
                for (Type t2 : args) {
                    tak.append(":").append(resolveTypeSimpleName(t2));
                }
            }
            return tak.toString();
        }
        return t.getTypeName();
    }

    public List<String> scanAll(TypeToken<?> holds) {
        return scanAll("*", holds);
    }

    public List<String> scanAll(String pattern, TypeToken<?> holds) {
        List<String> keys = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            String match = getDbkey(pattern, holds);
            String cursor = "0";
            do {
                ScanResult<String> xd = jedis.scan(cursor, new ScanParams().match(match));
                keys.addAll(xd.getResult());
                cursor = xd.getStringCursor();
            } while (!cursor.equals("0"));
        }
        return keys;
    }

    public abstract class CacheRetriever<T> {
        public RedisCache<T> getCache() {
            return getCache(300);
        }

        public RedisCache<T> getCache(int expiry) {
            return new RedisCache<>(RedisManager.this, new TypeToken<T>(getClass()) {
            }, expiry);
        }
    }

    private static class RedisThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public RedisThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "RedisCacheManager-AsyncThread-";
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
