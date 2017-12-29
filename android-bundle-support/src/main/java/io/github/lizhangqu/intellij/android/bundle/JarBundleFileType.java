package io.github.lizhangqu.intellij.android.bundle;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class JarBundleFileType implements FileType {
    public static final JarBundleFileType INSTANCE = new JarBundleFileType();

    @NotNull
    @Override
    public String getName() {
        return "jar";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Android jar";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "jar";
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
