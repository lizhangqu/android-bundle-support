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

import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.List;

@Beta
public class LintAnalyzer implements Analyzer {
    private final AppSizeIssueRegistry issueRegistry;
    private final LintClient client;
    private final LintRequest request;

    public LintAnalyzer(LintClient client, LintRequest request) {
        issueRegistry = new AppSizeIssueRegistry();
        this.client = client;
        this.request = request;
    }

    @Override
    @Nullable
    public List<Suggestion> analyze() {
        List<Issue> sizeIssues = issueRegistry.getIssues();
        List<Issue> enabledIssues = new ArrayList(sizeIssues.size());
        try {
            for (Issue issue : sizeIssues) {
                if (!issue.isEnabledByDefault()) {
                    issue.setEnabledByDefault(true);
                    enabledIssues.add(issue);
                }
            }
            LintDriver lint = new LintDriver(issueRegistry, client, request);
            lint.analyze();
        } finally {
            for (Issue issue : enabledIssues) {
                issue.setEnabledByDefault(false);
            }
        }
        // Issue:  https://issuetracker.google.com/issues/110100052
        // grab this from the client, the list of suggestions, and report it.
        return null;
    }
}
