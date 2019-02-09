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
import io.github.lizhangqu.intellij.android.bundle.analyzer.Archive;
import com.android.utils.FileUtils;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Implementation of {@link Archive} for an Instant App bundle zip file.
 *
 * <p>The contents of the <code>zip</code> file (i.e. <code>APK</code> files) are extracted into a
 * temporary directory. The {@link #close()} method deletes this temporary directory.
 */
public class InstantAppBundleArchive extends AbstractArchive {
    @NonNull private final Path extractedFilesPath;

    private InstantAppBundleArchive(@NonNull Path path) throws IOException {
        super(path);
        this.extractedFilesPath = Files.createTempDirectory(path.getFileName().toString());

        // For zip archives (which are AIA bundles), we unzip the outer zip contents to a temp folder
        // so that we show accurate file sizes for the top-level APKs in the ZIP file.
        extractArchiveContents(path);
    }

    private void extractArchiveContents(@NonNull Path artifact) throws IOException {
        try (FileSystem fileSystem = FileUtils.createZipFilesystem(artifact)) {
            Files.walkFileTree(
                    fileSystem.getPath("/"),
                    new CopyPathFileVisitor(fileSystem, extractedFilesPath));
        }
    }

    @NonNull
    public static InstantAppBundleArchive fromZippedBundle(@NonNull Path path) throws IOException {
        return new InstantAppBundleArchive(path);
    }

    @Override
    @NonNull
    public Path getContentRoot() {
        return extractedFilesPath;
    }

    @Override
    public void close() throws IOException {
        FileUtils.deletePath(extractedFilesPath.toFile());
    }

    private static class CopyPathFileVisitor implements FileVisitor<Path> {
        private final Path source;
        private final Path destination;

        public CopyPathFileVisitor(@NonNull FileSystem source, @NonNull Path destination) {
            this.source = source.getPath("/");
            this.destination = destination;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                throws IOException {
            Path resolved = destination.resolve(source.relativize(dir).toString()).normalize();
            if (resolved.startsWith(destination)) {
                Files.createDirectories(resolved);
                return FileVisitResult.CONTINUE;
            } else {
                return FileVisitResult.SKIP_SUBTREE;
            }
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, destination.resolve(source.relativize(file).toString()));
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
