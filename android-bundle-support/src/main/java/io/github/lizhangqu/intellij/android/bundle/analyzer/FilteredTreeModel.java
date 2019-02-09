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
package io.github.lizhangqu.intellij.android.bundle.analyzer;

import com.android.annotations.NonNull;
import java.util.Locale;
import java.util.function.Predicate;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class FilteredTreeModel<T> extends DefaultTreeModel {
    private final Predicate<T> predicate;

    public FilteredTreeModel(@NonNull TreeNode treeNode, @NonNull Predicate<T> predicate) {
        super(treeNode);
        this.predicate = predicate;
    }

    @Override
    public Object getChild(Object parent, int index) {
        for (int i = 0, n = super.getChildCount(parent); i < n; i++) {
            T result = (T) super.getChild(parent, i);
            if (predicate.test(result)) {
                if (index == 0) {
                    return result;
                } else {
                    index--;
                }
            }
        }

        String msg =
                String.format(
                        Locale.US,
                        "Child index %1$d is higher than # of children %2$d",
                        index,
                        getChildCount(parent));
        throw new IllegalStateException(msg);
    }

    @Override
    public int getChildCount(Object parent) {
        int count = 0;
        for (int i = 0, n = super.getChildCount(parent); i < n; i++) {
            T result = (T) super.getChild(parent, i);
            if (predicate.test(result)) {
                count++;
            }
        }

        return count;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        int index = 0;
        for (int i = 0, n = super.getChildCount(parent); i < n; i++) {
            T result = (T) super.getChild(parent, i);
            if (predicate.test(result)) {
                if (result.equals(child)) {
                    return index;
                } else {
                    index++;
                }
            }
        }

        return -1;
    }
}
