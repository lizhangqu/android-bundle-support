package io.github.lizhangqu.intellij.android.bundle.filetype;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ApksBundleFileType implements FileType {
    public static final ApksBundleFileType INSTANCE = new ApksBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "apks";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android App Bundle Apks";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "apks";
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
