package com.will.calculator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * 用户信息
 */

public class NumberInfo {

// 功能:获得用户的信息
    private static String getUserInfo(String key, Context context,
                                      String defaultValue) {
        SharedPreferences settings = context.getSharedPreferences("UserInfo",
                Activity.MODE_PRIVATE);
        return settings.getString(key, defaultValue);
    }
    // 功能:设置用户信息
    private static void setUserInfo(String Key, String Value, Context context) {
        SharedPreferences settings = context.getSharedPreferences("UserInfo",
                Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Key, Value);
        editor.commit();
    }

    /** 功能：M */
    public static String getUserM(Context context) {
        return getUserInfo("session", context, "");
    }

    /** 功能：M*/
    public static void setUserM(String session, Context context) {
        setUserInfo("session", session, context);
    }

}
