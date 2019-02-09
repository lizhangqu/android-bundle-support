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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import io.github.lizhangqu.intellij.android.bundle.analyzer.Archive;
import com.android.utils.FileUtils;
import com.android.utils.XmlUtils;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * Implementation of {@link Archive} for an &quot;Android App Bundle&quot; zip file.
 *
 * <p>The archive is opened as a <code>zip</code> {@link FileSystem} until the {@link #close()}
 * method is called.
 */
public class AppBundleArchive extends AbstractArchive {
    @NonNull private final FileSystem zipFileSystem;

    private AppBundleArchive(@NonNull Path path) throws IOException {
        super(path);
        this.zipFileSystem = FileUtils.createZipFilesystem(path);
    }

    @NonNull
    public static AppBundleArchive fromBundleFile(@NonNull Path artifact) throws IOException {
        return new AppBundleArchive(artifact);
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

    @Override
    public boolean isProtoXml(@NonNull Path p, @NonNull byte[] content) {
        if (!p.toString().endsWith(SdkConstants.DOT_XML)) {
            return false;
        }

        Path name = p.getFileName();
        if (name == null) {
            return false;
        }

        boolean manifest = isManifestFile(p);
        boolean insideResFolder = isInsideResFolder(p);
        boolean insideResRaw = isInsiderResRawFolder(p);
        boolean xmlResource = insideResFolder && !insideResRaw;
        if (!manifest && !xmlResource) {
            return false;
        }

        return XmlUtils.isProtoXml(content);
    }

    private static boolean isManifestFile(@NonNull Path p) {
        return matchPathPrefix(
                p, PathEntry.any(), PathEntry.FD_MANIFEST, PathEntry.FN_ANDROID_MANIFEST_XML);
    }

    private static boolean isInsideResFolder(@NonNull Path p) {
        return matchPathPrefix(p, PathEntry.any(), PathEntry.name(SdkConstants.FD_RES));
    }

    private static boolean isInsiderResRawFolder(@NonNull Path p) {
        return matchPathPrefix(p, PathEntry.any(), PathEntry.FD_RES, PathEntry.FD_RES_RAW);
    }

    private static boolean matchPathPrefix(
            @NonNull Path path, @NonNull PathEntry... prefixEntries) {
        int index = 0;
        for (PathEntry entry : prefixEntries) {
            if (!entry.matches(path.getName(index))) {
                return false;
            }
            index++;
        }
        return true;
    }

    public abstract static class PathEntry {
        public abstract boolean matches(@NonNull Path name);

        @NonNull
        public static PathEntry any() {
            return AnyPathEntry.instance;
        }

        @NonNull
        public static PathEntry name(@NonNull String name) {
            return new NamePathEntry(name);
        }

        @NonNull public static final PathEntry FD_RES = name(SdkConstants.FD_RES);
        @NonNull public static final PathEntry FD_RES_RAW = name(SdkConstants.FD_RES_RAW);

        @NonNull
        public static final PathEntry FN_ANDROID_MANIFEST_XML =
                name(SdkConstants.FN_ANDROID_MANIFEST_XML);

        @NonNull public static final PathEntry FD_MANIFEST = name("manifest");
    }

    public static class AnyPathEntry extends PathEntry {
        @NonNull public static AnyPathEntry instance = new AnyPathEntry();

        @Override
        public boolean matches(@NonNull Path name) {
            return true;
        }
    }

    public static class NamePathEntry extends PathEntry {
        private final String name;

        public NamePathEntry(@NonNull String name) {
            this.name = name;
        }

        @Override
        public boolean matches(@NonNull Path name) {
            // For ZIP paths, we use strict string equality
            return name.toString().equals(this.name);
        }
    }
}
