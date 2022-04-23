/*
 * Copyright (C) 2020 Shift GmbH
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
package com.android.launcher3.icons;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public final class IconPackStore {

    private static final String LAUNCHER_PREFERENCES = "com.android.launcher3.prefs";
    public static final String LAUNCHER_ICON_PACK = "launcher_icon_pack";
    public static final String DEFAULT_ICON_PACK = "android";

    private Context mContext;
    private SharedPreferences mSharedPreferences;

    public IconPackStore(Context context) {
        mContext = context;
        mSharedPreferences = context.getSharedPreferences(LAUNCHER_PREFERENCES, Context.MODE_PRIVATE);
    }

    public String getCurrent() {
        return mSharedPreferences.getString(LAUNCHER_ICON_PACK, DEFAULT_ICON_PACK);
    }

    public void setCurrent(String pkgName) {
        mSharedPreferences.edit()
            .putString(LAUNCHER_ICON_PACK, pkgName)
            .apply();
    }

    public boolean isUsingSystemIcons() {
        return DEFAULT_ICON_PACK.equals(getCurrent());
    }

    public String getCurrentLabel(String defaultLabel) {
        final String pkgName = getCurrent();
        if (DEFAULT_ICON_PACK.equals(pkgName)) {
            return defaultLabel;
        }

        final PackageManager pm = mContext.getPackageManager();
        try {
            final ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
            return (String) pm.getApplicationLabel(ai);
        } catch (PackageManager.NameNotFoundException e) {
            return defaultLabel;
        }
    }
}
