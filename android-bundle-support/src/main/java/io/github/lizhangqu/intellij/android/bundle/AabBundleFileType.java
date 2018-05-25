package io.github.lizhangqu.intellij.android.bundle;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AabBundleFileType implements FileType {
    public static final AabBundleFileType INSTANCE = new AabBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "aab";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android App Bundle";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "aab";
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
