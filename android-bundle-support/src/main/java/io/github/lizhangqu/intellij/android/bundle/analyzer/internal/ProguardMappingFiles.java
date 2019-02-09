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
import com.google.common.collect.Iterators;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProguardMappingFiles {
    private static final String EXT = ".txt";
    private static final String MAPPING_BASE = "mapping";
    private static final String USAGE_BASE = "usage";
    private static final String SEEDS_BASE = "seeds";
    private static final String MAPPING_FILENAME = MAPPING_BASE + EXT;
    private static final String USAGE_FILENAME = USAGE_BASE + EXT;
    private static final String SEEDS_FILENAME = SEEDS_BASE + EXT;

    @Nullable public final Path mappingFile;
    @Nullable public final Path seedsFile;
    @Nullable public final Path usageFile;

    public ProguardMappingFiles(
            @Nullable Path mappingFile, @Nullable Path seedsFile, @Nullable Path usageFile) {
        this.mappingFile = mappingFile;
        this.seedsFile = seedsFile;
        this.usageFile = usageFile;
    }

    /**
     * Given a folder or a set of files, looks through the folder contents or supplied files for
     * filenames matching mapping.txt, seeds.txt, usage.txt, then *mapping*.txt, *seeds*.txt,
     * *usage*.txt.
     *
     * @param paths - it can be a folder or 1-3 files, no mixing of folders and files is allowed
     * @return
     */
    @NonNull
    public static ProguardMappingFiles from(@NonNull Path[] paths) throws IOException {
        if (paths.length == 0) { // user canceled
            return new ProguardMappingFiles(null, null, null);
        }

        if (paths.length > 1) {
            for (Path path : paths) {
                if (Files.isDirectory(path)) {
                    throw new IllegalArgumentException(
                            "Please select a folder or 1 to 3 files for loading Proguard mappings.");
                }
            }
        }

        Path[] filesToCheck = null;

        if (Files.isDirectory(paths[0])) {
            try (DirectoryStream<Path> stream =
                    Files.newDirectoryStream(
                            paths[0],
                            "*{"
                                    + MAPPING_BASE
                                    + ","
                                    + USAGE_BASE
                                    + ","
                                    + SEEDS_BASE
                                    + "}*"
                                    + EXT)) {
                filesToCheck = Iterators.toArray(stream.iterator(), Path.class);
            }
        } else {
            filesToCheck = paths;
        }

        Path mappingFile = null;
        Path seedsFile = null;
        Path usageFile = null;

        for (Path file : filesToCheck) {
            //first try if the exact filenames are in the folder...
            if (mappingFile == null && file.getFileName().toString().equals(MAPPING_FILENAME)) {
                mappingFile = file;
            }
            if (seedsFile == null && file.getFileName().toString().equals(SEEDS_FILENAME)) {
                seedsFile = file;
            }
            if (usageFile == null && file.getFileName().toString().equals(USAGE_FILENAME)) {
                usageFile = file;
            }
        }

        //if we don't have all 3 already, go through the files list and search for partial names
        if (mappingFile == null || usageFile == null || seedsFile == null) {
            for (Path file : filesToCheck) {
                if (mappingFile == null && file.getFileName().toString().contains(MAPPING_BASE)) {
                    mappingFile = file;
                } else if (usageFile == null
                        && file.getFileName().toString().contains(SEEDS_BASE)) {
                    usageFile = file;
                } else if (seedsFile == null
                        && file.getFileName().toString().contains(USAGE_BASE)) {
                    seedsFile = file;
                }

                if (mappingFile != null && usageFile != null && seedsFile != null) {
                    break;
                }
            }
        }

        return new ProguardMappingFiles(mappingFile, seedsFile, usageFile);
    }
}
