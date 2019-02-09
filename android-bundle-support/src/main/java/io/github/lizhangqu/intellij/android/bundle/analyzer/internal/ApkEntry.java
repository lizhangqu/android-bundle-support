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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;

public interface ApkEntry {
    @Nullable
    static ApkEntry fromNode(@Nullable Object value) {
        if (!(value instanceof DefaultMutableTreeNode)) {
            return null;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object userObject = node.getUserObject();
        if (!(userObject instanceof ApkEntry)) {
            return null;
        }

        return (ApkEntry) userObject;
    }

    static void sort(@NonNull DefaultMutableTreeNode node) {
        if (node.getChildCount() == 0) {
            return;
        }

        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }

        Collections.sort(
                children,
                (o1, o2) -> {
                    ApkEntry entry1 = fromNode(o1);
                    ApkEntry entry2 = fromNode(o2);
                    if (entry1 == null || entry2 == null) {
                        return 0;
                    }
                    return Long.compare(entry2.getSize(), entry1.getSize());
                });

        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            node.add(child);
        }
    }

    @NonNull
    String getName();

    @NonNull
    Path getPath();

    long getSize();
}
