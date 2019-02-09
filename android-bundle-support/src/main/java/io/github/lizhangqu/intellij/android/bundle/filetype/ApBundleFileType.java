package io.github.lizhangqu.intellij.android.bundle.filetype;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ApBundleFileType implements FileType {
    public static final ApBundleFileType INSTANCE = new ApBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "ap_";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android ap_";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "ap_";
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
