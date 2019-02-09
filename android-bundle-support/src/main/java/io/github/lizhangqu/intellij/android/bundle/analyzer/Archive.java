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
import java.io.IOException;
import java.nio.file.Path;

public interface Archive extends AutoCloseable {
    /** Returns the {@link Path} of the archive in the local file system */
    @NonNull
    Path getPath();

    /**
     * Returns the {@link Path} of the root entry in the <bold>archive</bold> file system, e.g. the
     * local file system or the a zip archive file system
     */
    @NonNull
    Path getContentRoot();

    /**
     * Returns {@code true} if the entry at the given path in the archive file system is a Chunk
     * encoded XML file.
     */
    boolean isBinaryXml(@NonNull Path p, @NonNull byte[] content);

    /**
     * Returns {@code true} if the entry at the given path in the archive file system is an XML file
     * represented as a resource protobuf.
     */
    boolean isProtoXml(@NonNull Path p, @NonNull byte[] content);

    /** Closes the archive file */
    @Override
    void close() throws IOException;
}
