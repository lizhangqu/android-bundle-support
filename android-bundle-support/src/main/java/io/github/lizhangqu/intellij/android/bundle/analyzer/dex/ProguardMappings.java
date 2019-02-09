/*
 * Copyright (C) 2017 The Android Open Source Project
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
package io.github.lizhangqu.intellij.android.bundle.analyzer.dex;

import com.android.annotations.Nullable;
import com.android.tools.proguard.ProguardMap;
import com.android.tools.proguard.ProguardSeedsMap;
import com.android.tools.proguard.ProguardUsagesMap;

public class ProguardMappings {
    @Nullable public final ProguardMap map;
    @Nullable public final ProguardSeedsMap seeds;
    @Nullable public final ProguardUsagesMap usage;

    public ProguardMappings(
            @Nullable ProguardMap map,
            @Nullable ProguardSeedsMap seeds,
            @Nullable ProguardUsagesMap usage) {
        this.map = map;
        this.seeds = seeds;
        this.usage = usage;
    }
}
