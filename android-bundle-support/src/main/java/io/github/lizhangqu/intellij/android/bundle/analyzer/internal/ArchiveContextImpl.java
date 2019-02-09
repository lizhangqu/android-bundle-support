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
import io.github.lizhangqu.intellij.android.bundle.analyzer.Archive;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveContext;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveManager;
import java.io.IOException;

public class ArchiveContextImpl implements ArchiveContext {
    @NonNull private final ArchiveManager archiveManager;
    @NonNull private final Archive archive;

    public ArchiveContextImpl(@NonNull ArchiveManager archiveManager, @NonNull Archive archive) {
        this.archiveManager = archiveManager;
        this.archive = archive;
    }

    @Override
    @NonNull
    public ArchiveManager getArchiveManager() {
        return archiveManager;
    }

    @NonNull
    @Override
    public Archive getArchive() {
        return archive;
    }

    @Override
    public void close() throws IOException {
        archiveManager.close();
    }
}
