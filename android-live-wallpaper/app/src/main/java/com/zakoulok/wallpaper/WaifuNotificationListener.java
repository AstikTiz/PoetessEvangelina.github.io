package com.zakoulok.wallpaper;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class WaifuNotificationListener extends NotificationListenerService {
    static final String PREFS = "waifu_wallpaper_state";
    static final String KEY_NOTIFICATION_COUNT = "notification_count";

    @Override
    public void onListenerConnected() {
        saveCount(getActiveNotifications().length);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        saveCount(getActiveNotifications().length);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        saveCount(getActiveNotifications().length);
    }

    private void saveCount(int count) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_NOTIFICATION_COUNT, Math.max(0, count)).apply();
    }
}
