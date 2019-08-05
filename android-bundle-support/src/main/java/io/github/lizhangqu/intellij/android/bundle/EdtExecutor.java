package io.github.lizhangqu.intellij.android.bundle;


import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class EdtExecutor implements Executor {
    public static EdtExecutor INSTANCE = new EdtExecutor();

    private EdtExecutor() {
    }

    public void execute(@NotNull Runnable runnable) {
        UIUtil.invokeLaterIfNeeded(runnable);
    }
}
