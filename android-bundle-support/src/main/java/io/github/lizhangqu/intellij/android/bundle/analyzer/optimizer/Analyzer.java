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

import com.google.common.annotations.Beta;
import java.util.List;

/**
 * Interface implemented by apk/bundle and source code analyzers. This is used to give a list of
 * suggestions for optimizing the final binary size.
 *
 * <p>**NOTE: This is not a public or final API; if you rely on this be prepared to adjust your code
 * for the next tools release.**
 */
@Beta
public interface Analyzer {

    /** @return a list of suggestions for optimizing the apk for this analyzer. */
    List<Suggestion> analyze();
}
