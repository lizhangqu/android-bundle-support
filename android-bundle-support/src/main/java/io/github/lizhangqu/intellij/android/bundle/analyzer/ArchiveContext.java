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

package io.github.lizhangqu.intellij.android.bundle.analyzer;

import com.android.annotations.NonNull;
import java.io.IOException;

/**
 * Represents a top level {@link Archive} and its associated {@link ArchiveManager} used to keep
 * track of temporary files and directories required for extraction. Use the {@link #close()} method
 * to ensure immediate release of file handles and deletion of temporary files.
 */
public interface ArchiveContext extends AutoCloseable {
    /** The main (or "outer") archive */
    @NonNull
    Archive getArchive();

    /** The {@lin ArchiveManager} responsible for keeping track of inner archive resources */
    @NonNull
    ArchiveManager getArchiveManager();

    /**
     * Closes the main archive and all inner archives, deleting temporary files and directories used
     * for extraction
     */
    @Override
    void close() throws IOException;
}
