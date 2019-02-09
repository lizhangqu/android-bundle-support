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
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveContext;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveEntry;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveNode;
import io.github.lizhangqu.intellij.android.bundle.analyzer.ArchiveTreeStructure;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import javax.swing.tree.DefaultMutableTreeNode;

public class ApkDiffParser {
    @NonNull
    public static DefaultMutableTreeNode createTreeNode(
            @NonNull ArchiveContext oldFile, @NonNull ArchiveContext newFile) throws IOException {
        ArchiveNode oldRoot = ArchiveTreeStructure.create(oldFile);
        ArchiveNode newRoot = ArchiveTreeStructure.create(newFile);
        return createTreeNode(oldRoot, newRoot);
    }

    @NonNull
    private static DefaultMutableTreeNode createTreeNode(
            @Nullable ArchiveNode oldFile, @Nullable ArchiveNode newFile) throws IOException {
        if (oldFile == null && newFile == null) {
            throw new IllegalArgumentException("Both old and new files are null");
        }

        DefaultMutableTreeNode node = new DefaultMutableTreeNode();

        long oldSize = 0;
        long newSize = 0;

        HashSet<String> childrenInOldFile = new HashSet<>();
        final ArchiveEntry data = oldFile == null ? newFile.getData() : oldFile.getData();
        final String name =
                data.getPath().getFileName() != null
                        ? data.getPath().getFileName().toString()
                        : data.getArchive().getPath().getFileName().toString();
        if (oldFile != null) {
            if (!oldFile.getChildren().isEmpty()) {
                for (ArchiveNode oldChild : oldFile.getChildren()) {
                    ArchiveNode newChild = null;
                    if (newFile != null) {
                        for (ArchiveNode archiveNode : newFile.getChildren()) {
                            if (archiveNode
                                    .getData()
                                    .getPath()
                                    .getFileName()
                                    .toString()
                                    .equals(
                                            oldChild.getData()
                                                    .getPath()
                                                    .getFileName()
                                                    .toString())) {
                                newChild = archiveNode;
                                break;
                            }
                        }
                    }

                    childrenInOldFile.add(oldChild.getData().getPath().getFileName().toString());
                    DefaultMutableTreeNode childNode = createTreeNode(oldChild, newChild);
                    node.add(childNode);

                    ApkDiffEntry entry = (ApkDiffEntry) childNode.getUserObject();
                    oldSize += entry.getOldSize();
                    newSize += entry.getNewSize();
                }

                if (Files.size(oldFile.getData().getPath()) > 0) {
                    // This is probably a zip inside the apk, and we should use it's size
                    oldSize = Files.size(oldFile.getData().getPath());
                } else if (oldFile.getParent() == null) {
                    oldSize = Files.size(oldFile.getData().getArchive().getPath());
                }
            } else {
                oldSize += Files.size(oldFile.getData().getPath());
            }
        }
        if (newFile != null) {
            if (!newFile.getChildren().isEmpty()) {
                for (ArchiveNode newChild : newFile.getChildren()) {
                    if (childrenInOldFile.contains(
                            newChild.getData().getPath().getFileName().toString())) {
                        continue;
                    }

                    DefaultMutableTreeNode childNode = createTreeNode(null, newChild);
                    node.add(childNode);

                    ApkDiffEntry entry = (ApkDiffEntry) childNode.getUserObject();
                    oldSize += entry.getOldSize();
                    newSize += entry.getNewSize();
                }

                if (Files.size(newFile.getData().getPath()) > 0) {
                    // This is probably a zip inside the apk, and we should use it's size
                    newSize = Files.size(newFile.getData().getPath());
                } else if (newFile.getParent() == null) {
                    newSize = Files.size(newFile.getData().getArchive().getPath());
                }
            } else {
                newSize += Files.size(newFile.getData().getPath());
            }
        }

        node.setUserObject(new ApkDiffEntry(name, oldFile, newFile, oldSize, newSize));
        ApkEntry.sort(node);
        return node;
    }
}
