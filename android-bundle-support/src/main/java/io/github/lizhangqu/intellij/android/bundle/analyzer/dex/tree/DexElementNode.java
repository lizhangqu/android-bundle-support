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
import com.android.tools.proguard.ProguardMap;
import com.android.tools.proguard.ProguardSeedsMap;
import java.util.Collections;
import java.util.Comparator;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.reference.ImmutableReference;

public abstract class DexElementNode extends DefaultMutableTreeNode {

    @NonNull private final String name;
    @Nullable private final ImmutableReference reference;
    private boolean defined;
    private boolean removed;
    private int methodReferencesCount;
    private int methodDefinitionsCount;

    DexElementNode(@NonNull String name, boolean allowsChildren) {
        this(name, allowsChildren, null);
    }

    DexElementNode(
            @NonNull String name, boolean allowsChildren, @Nullable ImmutableReference reference) {
        super(null, allowsChildren);
        this.name = name;
        this.reference = reference;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public Reference getReference() {
        return reference;
    }

    @Override
    public DexElementNode getChildAt(int i) {
        return (DexElementNode) super.getChildAt(i);
    }

    public void sort(Comparator<DexElementNode> comparator) {
        for (int i = 0; i < getChildCount(); i++) {
            DexElementNode node = getChildAt(i);
            node.sort(comparator);
        }
        if (children != null) {
            Collections.sort(children, comparator);
        }
    }

    @Nullable
    public <T extends DexElementNode> T getChildByType(@NonNull String name, Class<T> type) {
        for (int i = 0; i < getChildCount(); i++) {
            DexElementNode node = getChildAt(i);
            if (name.equals(node.getName()) && type.equals(node.getClass())) {
                return (T) node;
            }
        }

        return null;
    }

    public boolean isSeed(
            @Nullable ProguardSeedsMap seedsMap, @Nullable ProguardMap map, boolean checkChildren) {
        if (seedsMap != null && checkChildren) {
            for (int i = 0, n = getChildCount(); i < n; i++) {
                DexElementNode node = getChildAt(i);
                if (node.isSeed(seedsMap, map, checkChildren)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public DexElementNode getParent() {
        return (DexElementNode) super.getParent();
    }

    public void update() {
        for (int i = 0, n = getChildCount(); i < n; i++) {
            DexElementNode node = getChildAt(i);
            node.update();
        }
    }

    protected static String combine(@NonNull String parentPackage, @NonNull String childName) {
        return parentPackage.isEmpty() ? childName : parentPackage + "." + childName;
    }

    public boolean isDefined() {
        return defined;
    }

    public void setDefined(boolean defined) {
        this.defined = defined;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public int getMethodReferencesCount() {
        return methodReferencesCount;
    }

    protected void setMethodReferencesCount(int methodReferencesCount) {
        this.methodReferencesCount = methodReferencesCount;
    }

    public int getMethodDefinitionsCount() {
        return methodDefinitionsCount;
    }

    protected void setMethodDefinitionsCount(int methodDefinitionsCount) {
        this.methodDefinitionsCount = methodDefinitionsCount;
    }

    /**
     * Returns the private size of this dex node, i.e. size that can not shared with other nodes.
     * Example of shared size that is not included in this value: strings in the string pool,
     * annotation sets.
     *
     * @return private size of node in bytes
     */
    public abstract long getSize();
}
