package io.github.lizhangqu.intellij.android.bundle;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AtlasAwbBundleFileType implements FileType {
    public static final AtlasAwbBundleFileType INSTANCE = new AtlasAwbBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "awb";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android atlas awb";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "awb";
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
