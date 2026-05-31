package com.zakoulok.wallpaper;

import android.Manifest;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 42;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button setWallpaperButton = findViewById(R.id.setWallpaperButton);
        Button locationButton = findViewById(R.id.locationButton);
        Button notificationsButton = findViewById(R.id.notificationsButton);

        setWallpaperButton.setOnClickListener(view -> openLiveWallpaperPicker());
        locationButton.setOnClickListener(view -> requestLocationPermission());
        notificationsButton.setOnClickListener(view -> openNotificationAccessSettings());
    }

    private void openLiveWallpaperPicker() {
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                new ComponentName(this, WaifuWallpaperService.class)
        );
        startActivity(intent);
    }

    private void requestLocationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_REQUEST);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void openNotificationAccessSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }
}
