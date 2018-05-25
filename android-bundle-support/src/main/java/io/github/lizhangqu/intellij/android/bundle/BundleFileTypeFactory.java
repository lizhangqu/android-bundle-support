package io.github.lizhangqu.intellij.android.bundle;

import com.intellij.openapi.fileTypes.FileTypeConsumer;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import org.jetbrains.annotations.NotNull;

public class BundleFileTypeFactory extends FileTypeFactory {

    @Override
    public void createFileTypes(@NotNull FileTypeConsumer consumer) {
        consumer.consume(SoBundleFileType.INSTANCE, SoBundleFileType.INSTANCE.getDefaultExtension());
        consumer.consume(AarBundleFileType.INSTANCE, AarBundleFileType.INSTANCE.getDefaultExtension());
        consumer.consume(AabBundleFileType.INSTANCE, AabBundleFileType.INSTANCE.getDefaultExtension());
        consumer.consume(ApBundleFileType.INSTANCE, ApBundleFileType.INSTANCE.getDefaultExtension());
        consumer.consume(AtlasApBundleFileType.INSTANCE, AtlasApBundleFileType.INSTANCE.getDefaultExtension());
        consumer.consume(AtlasAwbBundleFileType.INSTANCE, AtlasAwbBundleFileType.INSTANCE.getDefaultExtension());
    }
}