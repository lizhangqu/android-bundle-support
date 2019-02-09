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

import com.android.annotations.NonNull;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidApplicationInfo {
    public static final AndroidApplicationInfo UNKNOWN =
            new AndroidApplicationInfo("unknown", "unknown", 0);

    @NonNull public final String packageId;
    @NonNull public final String versionName;
    public final long versionCode;
    private final Map<String, String> usesFeature;
    private final Set<String> usesFeatureNotRequired;

    private static final Pattern impliedFeaturePattern =
            Pattern.compile("uses-implied-feature: name='(.+)' reason='(.+)'");
    private static final Pattern packagePattern =
            Pattern.compile(
                    "package: name='(.*)' versionCode='(.*)' versionName='(.*)' platformBuildVersionName='(.*)'");
    private final Set<String> permissions;

    public AndroidApplicationInfo(
            @NonNull String packageId, @NonNull String versionName, long versionCode) {
        this.packageId = packageId;
        this.versionName = versionName;
        this.versionCode = versionCode;
        usesFeature = ImmutableMap.of();
        usesFeatureNotRequired = ImmutableSet.of();
        permissions = ImmutableSet.of();
    }

    public AndroidApplicationInfo(
            @NonNull String packageId,
            @NonNull String versionName,
            long versionCode,
            Map<String, String> usesFeature,
            Set<String> usesFeatureNotRequired,
            Set<String> permissions) {
        this.packageId = packageId;
        this.versionName = versionName;
        this.versionCode = versionCode;
        this.usesFeature = usesFeature;
        this.usesFeatureNotRequired = usesFeatureNotRequired;
        this.permissions = permissions;
    }

    @NonNull
    public static AndroidApplicationInfo parse(@NonNull List<String> output) {
        String packageId = null;
        long versionCode = 0;
        String versionName = null;

        for (String line : output) {
            line = line.trim();
            if (line.startsWith("A: android:versionCode")) {
                // e.g: A: android:versionCode(0x0101021b)=(type 0x10)0x2079
                int eqIndex = line.indexOf("=(type 0x10)");
                if (eqIndex > 0) {
                    int endParenthesis = line.indexOf(')', eqIndex + 2);
                    if (endParenthesis > 0) {
                        String versionCodeStr = line.substring(endParenthesis + 1);
                        try {
                            versionCode = Long.decode(versionCodeStr);
                        } catch (NumberFormatException e) {
                            versionCode = 0;
                        }
                    }
                }
            } else if (line.startsWith("A: android:versionName")) {
                // e.g: A: android:versionName(0x0101021c)="51.0.2704.10" (Raw: "51.0.2704.10")
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    int endQuote = line.indexOf('\"', eqIndex + 2);
                    if (endQuote > 0) {
                        versionName = line.substring(eqIndex + 2, endQuote);
                    }
                }
            } else if (line.startsWith("A: package=")) {
                // e.g: A: package="com.android.chrome" (Raw: "com.android.chrome")
                int eqIndex = line.indexOf('=');
                if (eqIndex > 0) {
                    int endQuote = line.indexOf('\"', eqIndex + 2);
                    if (endQuote > 0) {
                        packageId = line.substring(eqIndex + 2, endQuote);
                    }
                }
            }

            if (packageId != null && versionName != null && versionCode != 0) {
                break;
            }
        }

        return new AndroidApplicationInfo(
                packageId == null ? "unknown" : packageId,
                versionName == null ? "?" : versionName,
                versionCode);
    }


    public static AndroidApplicationInfo parseBadging(@NonNull List<String> output) {
        Builder builder = new Builder();
        for (String line : output) {
            line = line.trim();
            if (line.startsWith("package:")) {
                // e.g: package: name='com.example' versionCode='1' versionName='1.0' platformBuildVersionName=''
                Matcher matcher = packagePattern.matcher(line);
                if (matcher.matches()) {
                    builder.setPackageId(matcher.group(1));
                    try {
                        builder.setVersionCode(Long.decode(matcher.group(2)));
                    } catch (NumberFormatException e) {
                        builder.setVersionCode(0);
                    }
                    builder.setVersionName(matcher.group(3));
                }
            } else if (line.startsWith("uses-feature:")) {
                String name = line.substring("uses-feature: name='".length(), line.length() - 1);
                builder.addFeature(name);
            } else if (line.startsWith("uses-implied-feature:")) {
                Matcher matcher = impliedFeaturePattern.matcher(line);
                if (matcher.matches()) {
                    builder.addImpliedFeature(matcher.group(1), matcher.group(2));
                }
            } else if (line.startsWith("uses-feature-not-required:")) {
                String name =
                        line.substring(
                                "uses-feature-not-required: name='".length(), line.length() - 1);
                builder.addFeatureNotRequired(name);
            } else if (line.startsWith("uses-permission:")) {
                String name = line.substring("uses-permission: name='".length(), line.length() - 1);
                builder.addPermission(name);
            }
        }

        return builder.build();
    }

    public Map<String, String> getUsesFeature() {
        return usesFeature;
    }

    public Set<String> getUsesFeatureNotRequired() {
        return usesFeatureNotRequired;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    private static class Builder {
        public String packageId;
        public String versionName;
        public long versionCode;
        private final Map<String, String> usesFeature = new HashMap<>();
        private final Set<String> usesFeatureNotRequired = new HashSet<>();
        private final Set<String> permissions = new HashSet<>();

        public void setPackageId(@NonNull String packageId) {
            this.packageId = packageId;
        }

        public void setVersionName(@NonNull String versionName) {
            this.versionName = versionName;
        }

        public void setVersionCode(long versionCode) {
            this.versionCode = versionCode;
        }

        public void addFeature(@NonNull String name) {
            if (!usesFeature.containsKey(name)) {
                usesFeature.put(name, null);
            }
        }

        public void addImpliedFeature(@NonNull String name, @NonNull String reason) {
            usesFeature.put(name, reason);
        }

        public void addFeatureNotRequired(@NonNull String name) {
            usesFeatureNotRequired.add(name);
        }

        public AndroidApplicationInfo build() {
            return new AndroidApplicationInfo(
                    packageId == null ? "unknown" : packageId,
                    versionName == null ? "?" : versionName,
                    versionCode,
                    Collections.unmodifiableMap(usesFeature),
                    Collections.unmodifiableSet(usesFeatureNotRequired),
                    Collections.unmodifiableSet(permissions));
        }

        public void addPermission(String name) {
            permissions.add(name);
        }
    }
}
