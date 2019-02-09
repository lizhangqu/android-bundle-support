/*
 * Copyright (C) 2016 The Android Open Source Project
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
package io.github.lizhangqu.intellij.android.bundle.analyzer;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.ide.common.process.BaseProcessOutputHandler;
import com.android.ide.common.process.CachedProcessOutputHandler;
import com.android.ide.common.process.DefaultProcessExecutor;
import com.android.ide.common.process.ProcessException;
import com.android.ide.common.process.ProcessInfoBuilder;
import com.android.ide.common.process.ProcessResult;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.sdklib.repository.LoggerProgressIndicatorWrapper;
import com.android.utils.ILogger;
import com.android.utils.LineCollector;
import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class AaptInvoker {
    @NonNull private final Path aapt;
    @NonNull private final DefaultProcessExecutor processExecutor;

    public AaptInvoker(@NonNull Path aaptPath, @NonNull ILogger logger) {
        aapt = aaptPath;
        processExecutor = new DefaultProcessExecutor(logger);
    }

    public AaptInvoker(@NonNull AndroidSdkHandler sdkHandler, @NonNull ILogger logger) {
        this(getPathToAapt(sdkHandler, logger), logger);
    }

    @NonNull
    private List<String> invokeAaptWithParameters(
            @NonNull File apkFile, @NonNull String resource, @NonNull String... parameters)
            throws ProcessException {
        String[] params = Arrays.copyOf(parameters, parameters.length + 2);
        params[params.length - 2] = apkFile.getPath();
        params[params.length - 1] = resource;
        return invokeAaptWithParameters(params);
    }

    @NonNull
    private List<String> invokeAaptWithParameters(@NonNull String... parameters)
            throws ProcessException {
        ProcessInfoBuilder builder = new ProcessInfoBuilder();

        builder.setExecutable(aapt.toFile());
        builder.addArgs(parameters);

        CachedProcessOutputHandler processOutputHandler = new CachedProcessOutputHandler();

        ProcessResult result =
                processExecutor
                        .execute(builder.createProcess(), processOutputHandler)
                        .rethrowFailure()
                        .assertNormalExitValue();
        result.assertNormalExitValue();

        BaseProcessOutputHandler.BaseProcessOutput output = processOutputHandler.getProcessOutput();
        LineCollector lineCollector = new LineCollector();
        output.processStandardOutputLines(lineCollector);
        return lineCollector.getResult();
    }

    @NonNull
    public List<String> getXmlTree(@NonNull File apk, @NonNull String xmlResourcePath)
            throws ProcessException {
        return invokeAaptWithParameters(apk, xmlResourcePath, "dump", "xmltree");
    }

    @NonNull
    public List<String> dumpBadging(@NonNull File apk) throws ProcessException {
        return invokeAaptWithParameters("dump", "badging", apk.toString());
    }

    /**
     * @return the path to aapt from the latest version of build tools that is installed, null if
     *     there are no build tools
     * @param sdkHandler pass in a configured sdkHandler to locate aapt
     */
    @NonNull
    public static Path getPathToAapt(@NonNull AndroidSdkHandler sdkHandler, ILogger logger) {
        BuildToolInfo latestBuildTool =
                sdkHandler.getLatestBuildTool(new LoggerProgressIndicatorWrapper(logger), true);
        if (latestBuildTool == null) {
            throw new IllegalStateException("Cannot locate latest build tools");
        }
        return latestBuildTool.getLocation().toPath().resolve(SdkConstants.FN_AAPT);
    }
}
