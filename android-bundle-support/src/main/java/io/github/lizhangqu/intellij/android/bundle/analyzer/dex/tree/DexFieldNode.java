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
package io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import io.github.lizhangqu.intellij.android.bundle.analyzer.dex.PackageTreeCreator;
import com.android.tools.proguard.ProguardMap;
import com.android.tools.proguard.ProguardSeedsMap;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;

public class DexFieldNode extends DexElementNode {
    private long size;

    public DexFieldNode(@NonNull String displayName, @Nullable ImmutableFieldReference reference) {
        super(displayName, false, reference);
    }

    @Nullable
    @Override
    public FieldReference getReference() {
        return (FieldReference) super.getReference();
    }

    @Override
    public boolean isSeed(
            @Nullable ProguardSeedsMap seedsMap, @Nullable ProguardMap map, boolean checkChildren) {
        if (seedsMap != null) {
            FieldReference reference = getReference();
            if (reference != null) {
                String fieldName = PackageTreeCreator.decodeFieldName(reference, map);
                String className =
                        PackageTreeCreator.decodeClassName(reference.getDefiningClass(), map);
                return seedsMap.hasField(className, fieldName);
            }
        }
        return false;
    }

    @Override
    public void update() {}

    @Override
    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
