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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import io.github.lizhangqu.intellij.android.bundle.analyzer.Archive;
import com.google.common.primitives.Shorts;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

/**
 * Implementation of {@link Archive} for an &quot;apk&quot; file.
 *
 * <p>The archive is opened as a {@code zip} {@link FileSystem} until the {@link #close()} method is
 * called.
 */
public class ApkArchive extends ZipArchive {
    public ApkArchive(@NonNull Path artifact) throws IOException {
        super(artifact);
    }

    @Override
    public boolean isBinaryXml(@NonNull Path p, @NonNull byte[] content) {
        if (!p.toString().endsWith(SdkConstants.DOT_XML)) {
            return false;
        }

        Path name = p.getFileName();
        if (name == null) {
            return false;
        }

        Path contents = this.getContentRoot();
        boolean manifest = p.equals(contents.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML));
        boolean insideResFolder = p.startsWith(contents.resolve(SdkConstants.FD_RES));
        boolean insideResRaw =
                p.startsWith(
                        contents.resolve(SdkConstants.FD_RES).resolve(SdkConstants.FD_RES_RAW));
        boolean xmlResource = insideResFolder && !insideResRaw;
        if (!manifest && !xmlResource) {
            return false;
        }

        short code = Shorts.fromBytes(content[1], content[0]);
        return code == 0x0003; // Chunk.Type.XML
    }
}
