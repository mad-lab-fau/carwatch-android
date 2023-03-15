package de.fau.cs.mad.carwatch.alarmmanager;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;

import de.fau.cs.mad.carwatch.Constants;

/**
 * Singleton class to control Alarm Ringing Sound
 */
public class AlarmSoundControl {

    private static final String TAG = AlarmSoundControl.class.getSimpleName();

    private static AlarmSoundControl sInstance;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

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
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void playAlarmSound(Context context) {
        Log.d(TAG, "Playing alarm sound");

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, getAlarmUri());
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            if (audioManager != null) {
                if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) <= audioManager.getStreamVolume(AudioManager.RINGER_MODE_SILENT)) {
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                }
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                mediaPlayer.prepare();
                mediaPlayer.start();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(Constants.VIBRATION_PATTERN, 0));
                } else {
                    vibrator.vibrate(Constants.VIBRATION_PATTERN, 0);
                }

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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
        }

        if (vibrator != null) {
            vibrator.cancel();
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