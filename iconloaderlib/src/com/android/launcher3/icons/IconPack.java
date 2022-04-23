/*
 * Copyright (C) 2019 Paranoid Android
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

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IconPack {
    /*
     * Useful Links:
     * https://github.com/teslacoil/Example_NovaTheme
     * http://stackoverflow.com/questions/7205415/getting-resources-of-another-application
     * http://stackoverflow.com/questions/3890012/how-to-access-string-resource-from-another-application
    */

    private final Context mContext;
    private final String mPackageName;
    private Resources mPackageRes;

    private Map<String, String> mIconPackRes;
    private List<String> mIconBackStrings;
    private List<Drawable> mIconBackList;
    private Drawable mIconUpon;
    private Drawable mIconMask;
    private float mIconScale;

    public IconPack(Context context, String packageName) {
        mContext = context;
        mPackageName = packageName;
    }

    public void setIcons(Map<String, String> iconPackResources, List<String> iconBackStrings) {
        mIconPackRes = iconPackResources;
        mIconBackStrings = iconBackStrings;
        mIconBackList = new ArrayList<Drawable>();
        try {
            mPackageRes = mContext.getPackageManager()
                    .getResourcesForApplication(mPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            // must never happen cause it's checked already in the provider
            return;
        }

        mIconMask = getDrawableForName(IconPackProvider.ICON_MASK_TAG);
        mIconUpon = getDrawableForName(IconPackProvider.ICON_UPON_TAG);
        for (int i = 0; i < mIconBackStrings.size(); i++) {
            final String backIconString = mIconBackStrings.get(i);
            final Drawable backIcon = getDrawableWithName(backIconString);
            if (backIcon != null) {
                mIconBackList.add(backIcon);
            }
        }

        final String scale = mIconPackRes.get(IconPackProvider.ICON_SCALE_TAG);
        if (scale != null) {
            try {
                mIconScale = Float.valueOf(scale);
            } catch (NumberFormatException e) {
            }
        }
    }

    public Drawable getIcon(LauncherActivityInfo info, Drawable appIcon, CharSequence appLabel) {
        return getIcon(info.getComponentName(), appIcon, appLabel);
    }

    public Drawable getIcon(ActivityInfo info, Drawable appIcon, CharSequence appLabel) {
        return getIcon(new ComponentName(info.packageName, info.name), appIcon, appLabel);
    }

    public Drawable getIcon(ComponentName name, Drawable appIcon, CharSequence appLabel) {
        return getDrawable(name.flattenToString(), appIcon, appLabel);
    }

    public Drawable getIcon(String packageName, Drawable appIcon, CharSequence appLabel) {
        return getDrawable(packageName, appIcon, appLabel);
    }

    private Drawable getDrawable(String name, Drawable appIcon, CharSequence appLabel) {
        Drawable d = getDrawableForName(name);
        if (d == null && appIcon != null) {
            d = compose(name, appIcon, appLabel);
        }
        return d;
    }

    private Drawable getIconBackFor(CharSequence tag) {
        if (mIconBackList == null || mIconBackList.size() == 0) {
            return null;
        }

        if (mIconBackList.size() == 1) {
            return mIconBackList.get(0);
        }

        try {
            final Drawable back = mIconBackList.get(
                    (tag.hashCode() & 0x7fffffff) % mIconBackList.size());
            return back;
        } catch (ArrayIndexOutOfBoundsException e) {
            return mIconBackList.get(0);
        }
    }

    private int getResourceIdForDrawable(String resource) {
        return mPackageRes.getIdentifier(resource, "drawable", mPackageName);
    }

    private Drawable getDrawableForName(String name) {
        final String item = mIconPackRes.get(name);
        if (TextUtils.isEmpty(item)) {
            return null;
        }

        final int id = getResourceIdForDrawable(item);
        return id == 0 ? null : mPackageRes.getDrawable(id);
    }

    private Drawable getDrawableWithName(String name) {
        final int id = getResourceIdForDrawable(name);
        return id == 0 ? null : mPackageRes.getDrawable(id);
    }

    private BitmapDrawable getBitmapDrawable(Drawable image) {
        if (image instanceof BitmapDrawable) {
            return (BitmapDrawable) image;
        }

        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final Bitmap bmResult = Bitmap.createBitmap(image.getIntrinsicWidth(),
                image.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmResult);
        image.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        image.draw(canvas);
        return new BitmapDrawable(mPackageRes, bmResult);
    }

    private Drawable compose(String name, Drawable appIcon, CharSequence appLabel) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final BitmapDrawable appIconBitmap = getBitmapDrawable(appIcon);
        final int width = appIconBitmap.getBitmap().getWidth();
        final int height = appIconBitmap.getBitmap().getHeight();
        float scale = mIconScale;
        final Drawable iconBack = getIconBackFor(appLabel);
        if (iconBack == null && mIconMask == null && mIconUpon == null){
            scale = 1.0f;
        }

        final Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        final int scaledWidth = (int) (width * scale);
        final int scaledHeight = (int) (height * scale);
        if (scaledWidth != width || scaledHeight != height) {
            final Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    appIconBitmap.getBitmap(), scaledWidth, scaledHeight, true);
            canvas.drawBitmap(scaledBitmap, (width - scaledWidth) / 2,
                    (height - scaledHeight) / 2, null);
        } else {
            canvas.drawBitmap(appIconBitmap.getBitmap(), 0, 0, null);
        }

        if (mIconMask != null) {
            mIconMask.setBounds(0, 0, width, height);
            BitmapDrawable  b = getBitmapDrawable(mIconMask);
            b.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            b.draw(canvas);
        }
        if (iconBack != null) {
            iconBack.setBounds(0, 0, width, height);
            BitmapDrawable  b = getBitmapDrawable(iconBack);
            b.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            b.draw(canvas);
        }
        if (mIconUpon != null) {
            mIconUpon.setBounds(0, 0, width, height);
            mIconUpon.draw(canvas);
        }

        return new BitmapDrawable(mPackageRes, bitmap);
    }
}
