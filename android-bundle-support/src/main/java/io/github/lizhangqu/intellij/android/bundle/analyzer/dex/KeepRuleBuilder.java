/*
 * Copyright (C) 2017 The Android Open Source Project
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
package io.github.lizhangqu.intellij.android.bundle.analyzer.dex;

public class KeepRuleBuilder {

    public static final String KEEP_PREAMBLE =
            "# Add *one* of the following rules to your Proguard configuration file.\n"
                    + "# Alternatively, you can annotate classes "
                    + "and class members with @android.support.annotation.Keep\n\n";
    public static final String KEEP_RULE =
            "# keep the class and specified members from being removed or renamed\n";
    public static final String KEEPCLASSMEMBERS_RULE =
            "# keep the specified class members from being removed or renamed \n"
                    + "# only if the class is preserved\n";
    public static final String KEEPNAMES_RULE =
            "# keep the class and specified members from being renamed only\n";
    public static final String KEEPCLASSMEMBERNAMES_RULE =
            "# keep the specified class members from being renamed only\n";

    public enum KeepType {
        KEEP,
        KEEPCLASSMEMBERS,
        KEEPNAMES,
        KEEPCLASSMEMBERNAMES
    }

    public static String ANY_CLASS = "**";
    public static String ANY_MEMBER = "*";

    private String packageName;
    private String className;
    private String memberName;

    public KeepRuleBuilder setPackage(String aPackage) {
        packageName = aPackage;
        return this;
    }

    public KeepRuleBuilder setClass(String aClass) {
        className = aClass;
        return this;
    }

    public KeepRuleBuilder setMember(String member) {
        memberName = member;
        return this;
    }

    public String build(KeepType keepType) {
        if (packageName == null) {
            throw new IllegalStateException("You must set a package.");
        }
        if (className == null) {
            className = ANY_CLASS;
        }
        if (memberName == null) {
            memberName = ANY_MEMBER;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("-");
        sb.append(keepType.toString().toLowerCase());
        sb.append(" class ");

        if (!packageName.isEmpty()) {
            sb.append(packageName);
            sb.append(".");
        }
        sb.append(className);
        sb.append(" { ");
        sb.append(memberName);
        sb.append("; }");
        return sb.toString();
    }
}
