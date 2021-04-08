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

import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public interface Cache<V> {
    V getIfPresent(@NotNull Object key);
    V getOrElse(@NotNull Object key, @NotNull V value);
    V get(@NotNull String key, @NotNull Function<? super String, ? extends V> mappingFunction);
    Map<String, V> getAllPresent(@NotNull Iterable<?> keys);
    void put(@NotNull String key, @NotNull V value);
    void putAll(@NotNull Map<? extends String,? extends V> map);
    void invalidate(@NotNull Object key);
    long getTTL(@NotNull Object key);
    void invalidateAll();
    void invalidateAll(@NotNull Iterable<?> keys);
    Map<String, V> asMap();
}
