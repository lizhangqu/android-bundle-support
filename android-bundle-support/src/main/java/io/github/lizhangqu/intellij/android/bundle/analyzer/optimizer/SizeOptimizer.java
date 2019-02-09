/*
 * Copyright (C) 2018 The Android Open Source Project
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

package io.github.lizhangqu.intellij.android.bundle.analyzer.optimizer;

import com.android.annotations.NonNull;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintRequest;
import com.google.common.annotations.Beta;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Beta
public class SizeOptimizer {

    private File appArchive;
    private List<Analyzer> analyzers;

    public SizeOptimizer(
            @NonNull File appArchive, @NonNull LintClient client, @NonNull LintRequest request) {
        this.appArchive = appArchive;
        analyzers = new ArrayList<>(1);
        analyzers.add(new LintAnalyzer(client, request));
    }

    @NonNull
    public List<Suggestion> analyze() {

        List<Suggestion> suggestions = new ArrayList<>(analyzers.size());
        for (Analyzer analyzer : analyzers) {
            List<Suggestion> analyzerSuggestions = analyzer.analyze();
            if (analyzerSuggestions != null && !analyzerSuggestions.isEmpty()) {
                suggestions.addAll(analyzerSuggestions);
            }
        }
        return suggestions;
    }

    @VisibleForTesting
    void setAnalyzers(@NonNull List<Analyzer> analyzers) {
        this.analyzers = analyzers;
    }
}
