package io.github.lizhangqu.intellij.android.bundle;

import com.android.tools.idea.apk.viewer.ApkEditor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class BundleEditorProvider implements FileEditorProvider, DumbAware {
    private static final String ID = "bundle-viewer";

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
        return BundleFileSystem.EXTENSIONS.contains(file.getExtension()) &&
                BundleFileSystem.getInstance().getRootByLocal(file) != null;
    }

    @NotNull
    @Override
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFile root = BundleFileSystem.getInstance().getRootByLocal(file);
        assert root != null; // see accept above
        return new ApkEditor(project, file, root);
    }

    @NotNull
    @Override
    public String getEditorTypeId() {
        return ID;
    }

    @NotNull
    @Override
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;
    }
}