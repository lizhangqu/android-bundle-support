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

package io.github.lizhangqu.intellij.android.bundle.analyzer;

import com.android.annotations.NonNull;
import java.nio.file.Path;

public class InnerArchiveEntry extends ArchiveEntry {
    @NonNull private final Archive innerArchive;

    public InnerArchiveEntry(
            @NonNull Archive archive,
            @NonNull Path path,
            @NonNull String pathPrefix,
            @NonNull Archive innerArchive) {
        super(archive, path, pathPrefix);
        this.innerArchive = innerArchive;
    }

    @NonNull
    public ArchiveEntry asArchiveEntry() {
        return new ArchiveEntry(innerArchive, innerArchive.getContentRoot(), getPathPrefix());
    }
}
