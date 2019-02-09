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
package io.github.lizhangqu.intellij.android.bundle.analyzer.internal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveNode;
import java.nio.file.Path;

public class ApkDiffEntry implements io.github.lizhangqu.intellij.android.bundle.analyzer.internal.ApkEntry {
    @NonNull private final String name;
    @Nullable private final ArchiveNode oldFile;
    @Nullable private final ArchiveNode newFile;
    private final long oldSize;
    private final long newSize;

    ApkDiffEntry(
            @NonNull String name,
            @Nullable ArchiveNode oldFile,
            @Nullable ArchiveNode newFile,
            long oldSize,
            long newSize) {
        if (oldFile == null && newFile == null) {
            throw new IllegalArgumentException("Both files can't be null");
        }
        this.name = name;
        this.oldFile = oldFile;
        this.newFile = newFile;
        this.oldSize = oldSize;
        this.newSize = newSize;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public Path getPath() {
        assert oldFile != null || newFile != null;
        return oldFile != null ? oldFile.getData().getPath() : newFile.getData().getPath();
    }

    @Override
    public long getSize() {
        return newSize - oldSize;
    }

    public long getOldSize() {
        return oldSize;
    }

    public long getNewSize() {
        return newSize;
    }

    public static long getOldSize(@NonNull io.github.lizhangqu.intellij.android.bundle.analyzer.internal.ApkEntry apkEntry) {
        if (apkEntry instanceof ApkDiffEntry) {
            return ((ApkDiffEntry) apkEntry).getOldSize();
        }
        return apkEntry.getSize();
    }

    public static long getNewSize(@NonNull ApkEntry apkEntry) {
        if (apkEntry instanceof ApkDiffEntry) {
            return ((ApkDiffEntry) apkEntry).getNewSize();
        }
        return apkEntry.getSize();
    }

    @Override
    public String toString() {
        return getName();
    }
}
