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

import io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexElementNode;
import io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexFieldNode;
import io.github.lizhangqu.intellij.android.bundle.analyzer.dex.tree.DexMethodNode;
import java.util.function.Predicate;

public class DexViewFilters implements Predicate<DexElementNode> {
    private boolean showMethods = true;
    private boolean showFields = true;
    private boolean showReferencedNodes = true;
    private boolean showRemovedNodes = false;

    public void setShowMethods(boolean showMethods) {
        this.showMethods = showMethods;
    }

    public void setShowFields(boolean showFields) {
        this.showFields = showFields;
    }

    public void setShowReferencedNodes(boolean showReferencedNodes) {
        this.showReferencedNodes = showReferencedNodes;
    }

    public void setShowRemovedNodes(boolean showRemovedNodes) {
        this.showRemovedNodes = showRemovedNodes;
    }

    public boolean isShowMethods() {
        return showMethods;
    }

    public boolean isShowFields() {
        return showFields;
    }

    public boolean isShowReferencedNodes() {
        return showReferencedNodes;
    }

    public boolean isShowRemovedNodes() {
        return showRemovedNodes;
    }

    @Override
    public boolean test(DexElementNode node) {
        return ((showFields || !(node instanceof DexFieldNode))
                && (showMethods || !(node instanceof DexMethodNode))
                && (showReferencedNodes || node.isDefined())
                && (showRemovedNodes || !node.isRemoved()));
    }
}
