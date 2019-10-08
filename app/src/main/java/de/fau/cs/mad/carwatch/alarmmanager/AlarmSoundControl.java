package de.fau.cs.mad.carwatch.alarmmanager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

/**
 * Singleton class to control Alarm Ringing Sound
 */
public class AlarmSoundControl {

    private static final String TAG = AlarmSoundControl.class.getSimpleName();

    private static AlarmSoundControl sInstance;

    private MediaPlayer mediaPlayer;

    private AlarmSoundControl() {
    }

    public static AlarmSoundControl getInstance() {
        if (sInstance == null) {
            sInstance = new AlarmSoundControl();
        }
        return sInstance;
    }

    /**
     * Play Alarm Sound
     */
    public void playAlarmSound(Context context) {
        Log.d(TAG, "Playing alarm sound");

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, getAlarmUri());
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

            if (audioManager != null) {
                if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                }
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.prepare();
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(MediaPlayer::start);
            }

        } catch (IOException e) {
            Log.e(TAG, "Can't read Alarm uri: " + getAlarmUri());
        }
    }

    /**
     * Stop Alarm Sound currently playing
     */
    public void stopAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    /**
     * Get alarm sound: try to get default alarm, then notification, then ringtone
     *
     * @return URI for alarm sound
     */
    private Uri getAlarmUri() {
        Uri alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarm == null) {
            alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alarm == null) {
                alarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        return alarm;
    }
}