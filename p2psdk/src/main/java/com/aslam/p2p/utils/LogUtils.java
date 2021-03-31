package com.aslam.p2p.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogUtils {

    private static final int TONE_INTERVAL = 20;
    private static int logLine = 1;
    private static StringBuilder logs = new StringBuilder();
    private static ToneGenerator toneGenerator;
    private static TextToSpeech textToSpeech;

    public static void consoleLog(String tag, String message) {
        message = (logLine++) + " " + (new SimpleDateFormat("H:mm:ss").format(new Date(System.currentTimeMillis()))) + " => " + message;
        logs.insert(0, message + "\n");
        if (Const.DEBUG_MODE) {
            Log.e(tag, message);
            // speech(this, message);
            // buzz(100);
        }
    }

    public static void printLogs() {
        Log.e("P2PService", "------------------------<LOGS>------------------------");
        Log.e("P2PService", logs.toString());
        Log.e("P2PService", "------------------------</LOGS>------------------------");
    }

    public static String getLogs() {
        StringBuilder output = new StringBuilder();
        String[] lines = logs.toString().split(System.getProperty("line.separator"));
        for (int i = 0; i < lines.length; i++) {
            if (i > 500) break;
            output.append(lines[i] + "\n");
        }
        return output.toString();
    }

    @SuppressLint("NewApi")
    public static void speech(final Context context, String words) {
        try {
            if (words.contains("Offline voices for English")) return;
            final String wordsToSay = words.replace("_", " ");
            if (textToSpeech != null) {
                textToSpeech.speak(wordsToSay, TextToSpeech.QUEUE_ADD, null, null);
                return;
            }
            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status != TextToSpeech.ERROR) {
                        textToSpeech.setLanguage(Locale.UK);
                        speech(context, wordsToSay);
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void buzz(int duration) {
        buzz(ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL, duration);
    }

    public static void buzz(final int tone, final int duration) {
        final Handler toneHandler = new Handler(Looper.getMainLooper());
        toneHandler.post(new Runnable() {
            @Override
            public void run() {
                if (toneGenerator == null) {
                    toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
                    toneGenerator.startTone(tone, duration);
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            toneGenerator.stopTone();
                            toneGenerator.release();
                            toneGenerator = null;
                        }
                    }, duration + TONE_INTERVAL);
                } else {
                    toneHandler.postDelayed(this, TONE_INTERVAL);
                }
            }
        });
    }
}
