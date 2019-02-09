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

import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;

public class DexPackageNode extends DexElementNode {
    @Nullable private final String packageName;

    public DexPackageNode(@NonNull String name, @Nullable String packageName) {
        super(name, true);
        this.packageName = packageName;
    }

    @Override
    public long getSize() {
        long size = 0;
        for (int i = 0, n = getChildCount(); i < n; i++) {
            DexElementNode node = getChildAt(i);
            size += node.getSize();
        }
        return size;
    }

    @NonNull
    public io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexClassNode getOrCreateClass(
            @NonNull String parentPackage,
            @NonNull String qualifiedClassName,
            @Nullable TypeReference typeReference) {
        int i = qualifiedClassName.indexOf('.');
        if (i < 0) {
            io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexClassNode node = getChildByType(qualifiedClassName, io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexClassNode.class);
            if (node == null) {
                node =
                        new DexClassNode(
                                qualifiedClassName,
                                typeReference != null
                                        ? ImmutableTypeReference.of(typeReference)
                                        : null);
                add(node);
            }
            return node;
        } else {
            String segment = qualifiedClassName.substring(0, i);
            String nextSegment = qualifiedClassName.substring(i + 1);
            DexPackageNode packageNode = getChildByType(segment, DexPackageNode.class);
            if (packageNode == null) {
                packageNode = new DexPackageNode(segment, combine(parentPackage, segment));
                add(packageNode);
            }
            return packageNode.getOrCreateClass(
                    combine(parentPackage, segment), nextSegment, typeReference);
        }
    }

    @Override
    public void update() {
        super.update();

        int methodDefinitions = 0;
        int methodReferences = 0;
        boolean isRemoved = true;
        boolean isDefined = false;

        for (int i = 0, n = getChildCount(); i < n; i++) {
            DexElementNode node = getChildAt(i);
            methodDefinitions += node.getMethodDefinitionsCount();
            methodReferences += node.getMethodReferencesCount();
            if (!node.isRemoved()) isRemoved = false;
            if (node.isDefined()) isDefined = true;
        }
        setDefined(isDefined);
        setRemoved(isRemoved);
        setMethodDefinitionsCount(methodDefinitions);
        setMethodReferencesCount(methodReferences);
    }

    @Nullable
    public String getPackageName() {
        return packageName;
    }
}
