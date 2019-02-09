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
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveEntry;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveNode;
import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class ArchiveTreeNode extends DefaultMutableTreeNode implements ArchiveNode {
    public ArchiveTreeNode(@NonNull ArchiveEntry data) {
        setUserObject(data);
    }

    @NonNull
    @Override
    public List<ArchiveNode> getChildren() {
        //noinspection unchecked
        return children == null ? ImmutableList.of() : ImmutableList.copyOf(children);
    }

    @Override
    public void add(MutableTreeNode newChild) {
        if (!(newChild instanceof ArchiveTreeNode)) {
            throw new IllegalArgumentException("Only instances of ArchiveTreeNode can be added.");
        }
        super.add(newChild);
    }

    @Nullable
    @Override
    public ArchiveTreeNode getParent() {
        return (ArchiveTreeNode) super.getParent();
    }

    @NonNull
    @Override
    public ArchiveEntry getData() {
        return (ArchiveEntry) getUserObject();
    }
}
