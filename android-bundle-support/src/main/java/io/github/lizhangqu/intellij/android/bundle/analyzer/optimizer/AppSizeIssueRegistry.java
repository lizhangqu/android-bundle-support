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
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.annotations.Beta;
import java.util.ArrayList;
import java.util.List;

@Beta
public class AppSizeIssueRegistry extends BuiltinIssueRegistry {

    private List<Issue> ourIssues;

    @NonNull
    @Override
    public List<Issue> getIssues() {
        if (ourIssues == null) {
            List<Issue> sIssues = super.getIssues();
            ourIssues = new ArrayList<Issue>(sIssues.size());
            for (Issue issue : sIssues) {
                if (issue.getCategory() == Category.PERFORMANCE) {
                    ourIssues.add(issue);
                }
            }
        }
        return ourIssues;
    }
}
