package org.codebase.voicerecognition.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class VoiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent intentService = new Intent(context, VoiceBackgroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intentService);
                } else {
                    context.startService(intentService);
                }
            }
        }

    }
}
