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
package io.github.lizhangqu.intellij.android.bundle.analyzer.dex;

import com.android.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.util.DexUtil;

public final class DexFiles {

    public static final int MAX_SUPPORTED_DEX_VERSION = 38;
    private static final Opcodes DEFAULT_OPCODES = Opcodes.getDefault();

    private DexFiles() {}

    @NonNull
    public static DexBackedDexFile getDexFile(@NonNull Path p) throws IOException {
        return getDexFile(Files.readAllBytes(p));
    }

    @NonNull
    public static DexBackedDexFile getDexFile(@NonNull byte[] contents) {
        try {
            return new DexBackedDexFile(DEFAULT_OPCODES, contents);
        } catch (DexUtil.UnsupportedFile e) {
            // b/65186612: if dex version is too new,
            // fallback by trying the latest dex version we do support.
            if (HeaderItem.getVersion(contents, 0) > MAX_SUPPORTED_DEX_VERSION) {
                contents[4] = '0';
                contents[5] = '0' + MAX_SUPPORTED_DEX_VERSION / 10 % 10;
                contents[6] = '0' + MAX_SUPPORTED_DEX_VERSION % 10;
                return new DexBackedDexFile(DEFAULT_OPCODES, contents);
            } else {
                throw e;
            }
        }
    }
}
