package com.zakoulok.wallpaper;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WaifuWallpaperService extends WallpaperService {
    private static final int LOW_POWER_THRESHOLD_PERCENT = 5;
    private static final long ACTIVE_FRAME_DELAY_MS = 33L;
    private static final long LOW_POWER_FRAME_DELAY_MS = 60_000L;
    private static final long WEATHER_REFRESH_MS = 15 * 60 * 1000L;

    @Override
    public Engine onCreateEngine() {
        return new WaifuEngine();
    }

    private final class WaifuEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        private final RectF rect = new RectF();
        private final Random random = new Random();
        private boolean visible;
        private float phase;
        private String weatherText = "погода…";
        private long lastWeatherLoad;
        private String phrase = "3 касания → чат | тряска → фраза";

        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                drawFrame();
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(drawRunner);
            weatherExecutor.shutdownNow();
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            if (isLowPowerMode(batteryPercent())) {
                return;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                String[] phrases = {
                        "Я рядом, хозяин ✦",
                        "Сегодня отличный день для маленького чуда.",
                        "Не забудь проверить уведомления.",
                        "Погода меняется, а я остаюсь с тобой.",
                        "Хочешь, я напомню тебе о важном?"
                };
                phrase = phrases[random.nextInt(phrases.length)];
                drawFrame();
            }
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            int battery = batteryPercent();
            boolean lowPowerMode = isLowPowerMode(battery);
            try {
                canvas = holder.lockCanvas();
                if (canvas == null) return;
                if (!lowPowerMode) {
                    phase += 0.045f;
                    maybeLoadWeather();
                } else {
                    phrase = "Батарея почти на нуле… я тихо подожду 💔";
                }
                drawScene(canvas, battery, lowPowerMode);
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }
            handler.removeCallbacks(drawRunner);
            if (visible) {
                handler.postDelayed(drawRunner, lowPowerMode ? LOW_POWER_FRAME_DELAY_MS : ACTIVE_FRAME_DELAY_MS);
            }
        }

        private boolean isLowPowerMode(int battery) {
            return battery >= 0 && battery <= LOW_POWER_THRESHOLD_PERCENT;
        }

        private void maybeLoadWeather() {
            long now = System.currentTimeMillis();
            if (now - lastWeatherLoad < WEATHER_REFRESH_MS) return;
            lastWeatherLoad = now;
            weatherExecutor.execute(() -> {
                try {
                    WeatherClient.WeatherSnapshot snapshot = WeatherClient.load(lastKnownLocation());
                    weatherText = snapshot.temperature + " · " + snapshot.description;
                } catch (Exception ignored) {
                    weatherText = "погода недоступна";
                }
            });
        }

        private Location lastKnownLocation() {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return gps != null ? gps : network;
        }

        private void drawScene(Canvas canvas, int battery, boolean lowPowerMode) {
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            float uiScale = uiScale(width);
            float characterScale = characterScale(width, height);
            drawBackground(canvas, width, height, lowPowerMode);
            drawStatusChips(canvas, width, battery, uiScale, lowPowerMode);
            float baseY = height * (height >= width ? 0.64f : 0.78f);
            drawCharacter(canvas, width / 2f, baseY, characterScale, lowPowerMode);
            drawSpeechBubble(canvas, width, height, uiScale, lowPowerMode);
        }

        private float uiScale(int width) {
            return clamp(width / 1080f, 0.72f, 1.25f);
        }

        private float characterScale(int width, int height) {
            return clamp(Math.min(width / 430f, height / 920f), 0.86f, 2.75f);
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private void drawBackground(Canvas canvas, int width, int height, boolean lowPowerMode) {
            int top = lowPowerMode ? 0xFF16111A : 0xFF120B1D;
            int bottom = lowPowerMode ? 0xFF2B2633 : 0xFF51306B;
            paint.setShader(new LinearGradient(0, 0, width, height, top, bottom, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, paint);
            paint.setShader(null);
            paint.setColor(lowPowerMode ? 0x10FFFFFF : 0x22FFFFFF);
            int stars = lowPowerMode ? 14 : 42;
            float drift = lowPowerMode ? 0f : phase * 60f;
            for (int i = 0; i < stars; i++) {
                float x = (i * 83) % Math.max(1, width);
                float y = (i * 137 + drift) % Math.max(1, height);
                canvas.drawCircle(x, y, 1.5f + (i % 4), paint);
            }
        }

        private void drawStatusChips(Canvas canvas, int width, int battery, float uiScale, boolean lowPowerMode) {
            float margin = 28f * uiScale;
            float gap = 10f * uiScale;
            float chipHeight = 48f * uiScale;
            float y = 52f * uiScale;
            String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            if (lowPowerMode) {
                drawChip(canvas, margin, y, width - margin * 2f, "🔋 " + batteryLabel(battery) + " · статичный режим", uiScale);
                drawChip(canvas, margin, y + chipHeight + gap, width - margin * 2f, "😢 Девушка очень грустит и экономит заряд", uiScale);
                return;
            }

            boolean twoColumns = width >= 760f * uiScale;
            if (twoColumns) {
                float columnWidth = (width - margin * 2f - gap) / 2f;
                drawChip(canvas, margin, y, columnWidth, "🕒 " + time, uiScale);
                drawChip(canvas, margin + columnWidth + gap, y, columnWidth, "🔋 " + batteryLabel(battery), uiScale);
                drawChip(canvas, margin, y + chipHeight + gap, columnWidth, "🌦 " + weatherText, uiScale);
                drawChip(canvas, margin + columnWidth + gap, y + chipHeight + gap, columnWidth, "🔔 " + notificationCount(), uiScale);
            } else {
                float fullWidth = width - margin * 2f;
                drawChip(canvas, margin, y, fullWidth, "🕒 " + time, uiScale);
                drawChip(canvas, margin, y + (chipHeight + gap), fullWidth, "🌦 " + weatherText, uiScale);
                drawChip(canvas, margin, y + (chipHeight + gap) * 2f, fullWidth, "🔋 " + batteryLabel(battery) + " · 🔔 " + notificationCount(), uiScale);
            }
        }

        private void drawChip(Canvas canvas, float x, float y, float width, String text, float uiScale) {
            float height = 48f * uiScale;
            paint.setTextSize(28f * uiScale);
            paint.setFakeBoldText(true);
            rect.set(x, y, x + width, y + height);
            paint.setColor(0xAA24142F);
            canvas.drawRoundRect(rect, height / 2f, height / 2f, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText(ellipsize(text, width - 34f * uiScale), x + 17f * uiScale, y + 33f * uiScale, paint);
            paint.setFakeBoldText(false);
        }

        private String ellipsize(String text, float maxWidth) {
            if (paint.measureText(text) <= maxWidth) return text;
            String suffix = "…";
            int end = text.length();
            while (end > 0 && paint.measureText(text, 0, end) + paint.measureText(suffix) > maxWidth) {
                end--;
            }
            return text.substring(0, Math.max(0, end)) + suffix;
        }

        private String batteryLabel(int battery) {
            return battery < 0 ? "?%" : battery + "%";
        }

        private int batteryPercent() {
            BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            if (manager == null) return -1;
            int percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            return percent == Integer.MIN_VALUE ? -1 : percent;
        }

        private int notificationCount() {
            SharedPreferences prefs = getSharedPreferences(WaifuNotificationListener.PREFS, MODE_PRIVATE);
            return prefs.getInt(WaifuNotificationListener.KEY_NOTIFICATION_COUNT, 0);
        }

        private void drawCharacter(Canvas canvas, float centerX, float baseY, float scale, boolean sad) {
            float bob = sad ? 0f : (float) Math.sin(phase) * 10f * scale;
            float headY = baseY - 170f * scale + bob;

            int aura = sad ? 0xFF4F5968 : 0xFFB58CFF;
            int hair = sad ? 0xFF566074 : 0xFF6F42C8;
            int hairBack = sad ? 0xFF313744 : 0xFF3B214D;
            int outfit = sad ? 0xFF3B4250 : 0xFF9367E8;
            int skin = sad ? 0xFFE7BAC6 : 0xFFF6CDD8;

            paint.setShader(new RadialGradient(centerX, headY - 20f * scale, 250f * scale, aura, 0x00351A4D, Shader.TileMode.CLAMP));
            canvas.drawCircle(centerX, headY, 260f * scale, paint);
            paint.setShader(null);

            paint.setColor(hairBack);
            canvas.drawOval(centerX - 155f * scale, headY + 150f * scale, centerX + 155f * scale, headY + 480f * scale, paint);
            paint.setColor(outfit);
            canvas.drawRoundRect(centerX - 125f * scale, headY + 120f * scale, centerX + 125f * scale, headY + 420f * scale, 70f * scale, 70f * scale, paint);
            paint.setColor(0xFFF3C5D2);
            canvas.drawCircle(centerX - 92f * scale, headY + 205f * scale, 34f * scale, paint);
            canvas.drawCircle(centerX + 92f * scale, headY + 205f * scale, 34f * scale, paint);

            paint.setColor(hair);
            canvas.drawOval(centerX - 138f * scale, headY - 110f * scale, centerX + 138f * scale, headY + 165f * scale, paint);
            paint.setColor(skin);
            canvas.drawOval(centerX - 95f * scale, headY - 78f * scale, centerX + 95f * scale, headY + 115f * scale, paint);

            paint.setColor(0xFF2C183D);
            if (sad) {
                paint.setStrokeWidth(5f * scale);
                paint.setStrokeCap(Paint.Cap.ROUND);
                canvas.drawLine(centerX - 48f * scale, headY - 3f * scale, centerX - 23f * scale, headY + 8f * scale, paint);
                canvas.drawLine(centerX + 23f * scale, headY + 8f * scale, centerX + 48f * scale, headY - 3f * scale, paint);
                paint.setColor(0xFF89C9FF);
                canvas.drawOval(centerX - 42f * scale, headY + 16f * scale, centerX - 30f * scale, headY + 48f * scale, paint);
            } else {
                canvas.drawCircle(centerX - 35f * scale, headY + 2f * scale, 10f * scale, paint);
                canvas.drawCircle(centerX + 35f * scale, headY + 2f * scale, 10f * scale, paint);
                paint.setColor(0xFFFFF1F7);
                canvas.drawCircle(centerX - 31f * scale, headY - 3f * scale, 3f * scale, paint);
                canvas.drawCircle(centerX + 39f * scale, headY - 3f * scale, 3f * scale, paint);
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f * scale);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(sad ? 0xFF8D4B65 : 0xFFD6458C);
            Path mouth = new Path();
            mouth.moveTo(centerX - 26f * scale, headY + 58f * scale);
            if (sad) {
                mouth.quadTo(centerX, headY + 36f * scale, centerX + 26f * scale, headY + 58f * scale);
            } else {
                mouth.quadTo(centerX, headY + 70f * scale, centerX + 26f * scale, headY + 48f * scale);
            }
            canvas.drawPath(mouth, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setColor(sad ? 0x88FFFFFF : 0xCCFFFFFF);
            canvas.drawCircle(centerX - 78f * scale, headY + 34f * scale, 10f * scale, paint);
            canvas.drawCircle(centerX + 78f * scale, headY + 34f * scale, 10f * scale, paint);
        }

        private void drawSpeechBubble(Canvas canvas, int width, int height, float uiScale, boolean lowPowerMode) {
            float margin = 30f * uiScale;
            float bottom = height - 62f * uiScale;
            float bubbleHeight = lowPowerMode ? 118f * uiScale : 96f * uiScale;
            rect.set(margin, bottom - bubbleHeight, width - margin, bottom);
            paint.setColor(lowPowerMode ? 0xEE1D1820 : 0xDD24142F);
            canvas.drawRoundRect(rect, 34f * uiScale, 34f * uiScale, paint);
            paint.setColor(0xFFFFF6FF);
            paint.setTextSize(27f * uiScale);
            paint.setFakeBoldText(false);
            drawWrappedText(canvas, phrase, rect.left + 24f * uiScale, rect.top + 42f * uiScale, rect.width() - 48f * uiScale, 34f * uiScale);
        }

        private void drawWrappedText(Canvas canvas, String text, float x, float y, float maxWidth, float lineHeight) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            int lines = 0;
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    canvas.drawText(line.toString(), x, y + lineHeight * lines, paint);
                    lines++;
                    line = new StringBuilder(word);
                    if (lines == 2) break;
                }
            }
            if (line.length() > 0 && lines < 3) {
                canvas.drawText(ellipsize(line.toString(), maxWidth), x, y + lineHeight * lines, paint);
            }
        }
    }
}
