package io.github.lizhangqu.intellij.android.bundle.filetype;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AtlasApBundleFileType implements FileType {
    public static final AtlasApBundleFileType INSTANCE = new AtlasApBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "ap";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android atlas ap";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "ap";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return AndroidIcons.AndroidFile;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Nullable
    @Override
    public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
        return null;
    }
}
