package com.example.olla.smartassistant;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.widget.Toast;

public class AlarmAlertReceiver extends BroadcastReceiver {
    public static Vibrator alarmVibrator;
    public static Ringtone alarmRingtone;
    public static MediaPlayer alarmMediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "ALARM ACTIVE!", Toast.LENGTH_SHORT).show();

        // creating vibrator instance for phone to vibrate when alarm is fired!
        alarmVibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        alarmVibrator.vibrate(10000);

        // creating ringtone instance for phone to ring when alarm is fired
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        alarmRingtone = (Ringtone) RingtoneManager.getRingtone(context, uri);

        // start ringtone
        alarmRingtone.play();

        alarmMediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_RINGTONE_URI);


    }
}
