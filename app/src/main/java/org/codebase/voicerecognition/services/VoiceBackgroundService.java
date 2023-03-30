package org.codebase.voicerecognition.services;

import static androidx.core.app.NotificationCompat.PRIORITY_HIGH;

import static org.codebase.voicerecognition.MainActivity.LOG_TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import net.gotev.speech.GoogleVoiceTypingDisabledException;
import net.gotev.speech.Logger;
import net.gotev.speech.Speech;
import net.gotev.speech.SpeechDelegate;
import net.gotev.speech.SpeechRecognitionNotAvailable;

import org.codebase.voicerecognition.MainActivity;
import org.codebase.voicerecognition.R;
import org.codebase.voicerecognition.utils.App;

import java.util.ArrayList;
import java.util.List;

public class VoiceBackgroundService extends Service implements SpeechDelegate {

    MainActivity mainActivity = new MainActivity();

    private static final int ID_SERVICE = 101;

    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;


    // create audio manager instance to mute/unmute the beep of microphone
    AudioManager amanager=(AudioManager) App.getContext().getSystemService(Context.AUDIO_SERVICE);


    //unmute audio
//    amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
//    amanager.setStreamMute(AudioManager.STREAM_ALARM, false);
//    amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
//    amanager.setStreamMute(AudioManager.STREAM_RING, false);
//    amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);


    // text to speech initializer
    private TextToSpeech.OnInitListener mTttsInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(final int status) {
            switch (status) {
                case TextToSpeech.SUCCESS:
                    Logger.info(LOG_TAG, "TextToSpeech engine successfully started");
                    break;

                case TextToSpeech.ERROR:
                    Logger.error(LOG_TAG, "Error while initializing TextToSpeech engine!");
                    break;

                default:
                    Logger.error(LOG_TAG, "Unknown TextToSpeech status: " + status);
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(App.getContext(), 0,
                notificationIntent, PendingIntent.FLAG_MUTABLE);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(getString(R.string.voicerecognition))
                .setContentText("Recognizing Voice")
                .setSmallIcon(R.drawable.ic_mic)
                .setPriority(PRIORITY_HIGH)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        startForeground(ID_SERVICE, notification);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyWifiLock");
        wifiLock.acquire();
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();

        //mute audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            amanager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
            amanager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_MUTE, 0);
            amanager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
//            amanager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
//            amanager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
        } else {
            amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
            amanager.setStreamMute(AudioManager.STREAM_ALARM, true);
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, true);
            amanager.setStreamMute(AudioManager.STREAM_RING, true);
            amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true);
        }
    }

    // notification channel written to shew the notification on the top of app when service starts
    private String createNotificationChannel(NotificationManager notificationManager) {
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_MIN);
            channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(channel);
        }
        return channelId;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // this method will start the service and check service in every 1 second with the help of handle
        final Handler handler = new Handler();
        final int delay = 1000; // delay of 1 minutes
        handler.postDelayed(new Runnable() {
            public void run() {
                Log.e("running", "service");
                try {
                    Speech.init(App.getContext(), getPackageName(), mTttsInitListener);

                    Speech.getInstance().startListening((SpeechDelegate) VoiceBackgroundService.this);
                } catch (SpeechRecognitionNotAvailable | GoogleVoiceTypingDisabledException e) {
                    e.printStackTrace();
                }
                handler.postDelayed(this, delay);
            }
        }, delay);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(rootIntent);
                } else {
                    startService(rootIntent);
                }
            }
        }, 1000);
    }


    @Override
    public void onDestroy() {
        if (wakeLock != null) {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
        if (wifiLock != null) {
            if (wifiLock.isHeld()) {
                wifiLock.release();
            }
        }
        super.onDestroy();

        // stop the service and microphone
        Speech.getInstance().stopListening();
        stopForeground(true);
        this.stopSelf();
    }

    @Override
    public void onStartOfSpeech() {

    }

    @Override
    public void onSpeechRmsChanged(float value) {

    }

    @Override
    public void onSpeechPartialResults(List<String> results) {

        Log.e("partial results", results.toString());
    }

    // this shows the results of speaking voice in the form of text
    @Override
    public void onSpeechResult(String result) {
        Log.e("running", "speech");

        if (result.isEmpty()) {
//            Speech.getInstance().say(getString(R.string.repeat));

            Log.e("Empty", "Word Not Recognize");
        } else {
            if (App.getStringFromSharedPreferences("voice_text").contains(result)) {
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Word doesn't exists!", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
