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
import com.android.utils.FileUtils;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * Implementation of {@link Archive} for any kind of &quot;zip&quot; file.
 *
 * <p>The archive is opened as a <code>zip</code> {@link FileSystem} until the {@link #close()}
 * method is called.
 */
public class ZipArchive extends AbstractArchive {
    @NonNull private final FileSystem zipFileSystem;

    public ZipArchive(@NonNull Path path) throws IOException {
        super(path);
        this.zipFileSystem = FileUtils.createZipFilesystem(path);
    }

    @Override
    @NonNull
    public Path getContentRoot() {
        return zipFileSystem.getPath("/");
    }

    @Override
    public void close() throws IOException {
        zipFileSystem.close();
    }
}
