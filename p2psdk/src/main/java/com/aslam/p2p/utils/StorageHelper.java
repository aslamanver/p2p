package com.aslam.p2p.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.aslam.p2p.models.DataCenter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StorageHelper {

    private static final String PREF = "com.aslam.p2p.PREF";
    private static DataCenter dataCenter;

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static Gson getGson() {
        return new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    public static String toJSON(Object object) {
        return getGson().toJson(object);
    }

    public static <T> T fromJSON(String json, Class<T> classOfT) {
        return getGson().fromJson(json, classOfT);
    }

    public static <T> T clone(Object object, Class<T> classOfT) {
        return fromJSON(toJSON(object), classOfT);
    }

    public static boolean storeData(Context context, Object object) {
        if (object == null) return clearData(context, object.getClass());
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        String json = toJSON(object);
        editor.putString(object.getClass().getSimpleName(), json);
        return editor.commit();
    }

    public static <T> T getData(Context context, Class<T> classOfT) {
        String json = getSharedPreferences(context).getString(classOfT.getSimpleName(), null);
        return fromJSON(json, classOfT);
    }

    public static boolean clearData(Context context, Class classOfT) {
        return getSharedPreferences(context).edit().remove(classOfT.getSimpleName()).commit();
    }

    public static DataCenter getDataCenter(Context context) {
        return dataCenter != null ? dataCenter : syncDataCenter(context);
    }

    public static DataCenter syncDataCenter(Context context) {
        dataCenter = getData(context, DataCenter.class);
        return dataCenter == null ? new DataCenter() : dataCenter;
    }

    public static boolean storeDataCenter(Context context, DataCenter dataCenter) {
        boolean stored = storeData(context, dataCenter);
        if (stored) syncDataCenter(context);
        return stored;
    }
}

