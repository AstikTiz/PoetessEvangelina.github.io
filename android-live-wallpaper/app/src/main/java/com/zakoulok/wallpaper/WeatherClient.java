package com.zakoulok.wallpaper;

import android.location.Location;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

final class WeatherClient {
    private WeatherClient() {}

    static WeatherSnapshot load(Location location) throws Exception {
        double latitude = location == null ? 55.7558 : location.getLatitude();
        double longitude = location == null ? 37.6173 : location.getLongitude();
        String endpoint = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&timezone=auto",
                latitude,
                longitude);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
            JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
            double temperature = current.getDouble("temperature_2m");
            int code = current.getInt("weather_code");
            return new WeatherSnapshot(Math.round(temperature) + "°C", descriptionFor(code));
        } finally {
            connection.disconnect();
        }
    }

    private static String descriptionFor(int code) {
        if (code == 0) return "ясно";
        if (code <= 3) return "облачно";
        if (code == 45 || code == 48) return "туман";
        if (code >= 51 && code <= 67) return "дождь";
        if (code >= 71 && code <= 77) return "снег";
        if (code >= 80 && code <= 82) return "ливень";
        if (code >= 95) return "гроза";
        return "погода";
    }

    static final class WeatherSnapshot {
        final String temperature;
        final String description;

        WeatherSnapshot(String temperature, String description) {
            this.temperature = temperature;
            this.description = description;
        }
    }
}
