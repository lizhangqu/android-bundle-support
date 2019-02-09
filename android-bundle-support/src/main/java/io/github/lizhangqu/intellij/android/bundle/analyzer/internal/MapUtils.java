/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lizhangqu.intellij.android.bundle.analyzer.internal;

import com.android.annotations.NonNull;
import java.util.Map;
import java.util.function.Function;

public class MapUtils {
    /**
     * Similar to {@link Map#computeIfAbsent(Object, Function)}, except that {@code Function} can
     * throw an {@link Exception}.
     */
    public static <K, V, E extends Throwable> V computeIfAbsent(
            @NonNull Map<K, V> map, @NonNull K key, ThrowableFunction<K, V, E> supplier) throws E {
        V value = map.get(key);
        if (value == null) {
            value = supplier.apply(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return value;
    }

    public interface ThrowableFunction<T, R, E extends Throwable> {
        R apply(T t) throws E;
    }
}
