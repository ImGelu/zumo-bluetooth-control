package edu.utcluj.robotcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;

import edu.utcluj.robotcontroller.room.User;

public abstract class GlobalData {
    public final static String USERS_DB_NAME = "users.db";
    public final static String LOGGED_IN_USER = "LOGGED_IN_USER";
    public final static String ENDPOINT = "ENDPOINT";
    public final static String DARK_MODE = "DARK_MODE";
    public final static String BT_NAME = "BT_NAME";
    public final static String BT_ADDRESS = "BT_ADDRESS";

    private static User loggedInUser;
    public static boolean isDarkMode = false;
    private static String endpoint;
    public static String defaultEndpoint = "10.0.2.2:8082";

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    public static String getEndpoint() {
        return endpoint;
    }

    public static void setDarkMode(boolean isDarkMode, Context context){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(GlobalData.DARK_MODE, isDarkMode);
        editor.apply();

        if(isDarkMode){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    public static void setLoggedInUser(User loggedInUser, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();

        if (loggedInUser != null) {
            String userJson = gson.toJson(loggedInUser);
            editor.putString(GlobalData.LOGGED_IN_USER, userJson);
        } else {
            editor.putString(GlobalData.LOGGED_IN_USER, null);
        }

        editor.apply();

        if (loggedInUser != null) {
            GlobalData.loggedInUser = gson.fromJson(preferences.getString(GlobalData.LOGGED_IN_USER, ""), User.class);
        } else {
            GlobalData.loggedInUser = null;
        }

    }

    public static void setEndpoint(String ip, int port, Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        if (preferences.getString(GlobalData.ENDPOINT, null) != null) {
            editor.putString(GlobalData.ENDPOINT, ip + ":" + port);
        } else {
            editor.putString(GlobalData.ENDPOINT, GlobalData.defaultEndpoint);
        }

        editor.apply();

        GlobalData.endpoint = preferences.getString(GlobalData.ENDPOINT, GlobalData.defaultEndpoint);
    }
}