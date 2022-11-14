package de.fau.cs.mad.carwatch.ui.alarm;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.ui.widgets.SwipeButton;

public class ShowAlarmFragment extends Fragment implements SwipeButton.OnSwipeListener {

    // TODO why is this here and not in AlarmSoundControl?
    private Vibrator vibrator;

    private SwipeButton.OnSwipeListener onSwipeListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_show_alarm, container, false);

        SwipeButton swipeButton = root.findViewById(R.id.button_swipe);
        swipeButton.setOnSwipeListener(this);

        // TODO removed for testing
        /*if (getContext() != null) {
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(Constants.VIBRATION_PATTERN, 0));
                } else {
                    vibrator.vibrate(Constants.VIBRATION_PATTERN, 0);
                }
            }
        }*/


        return root;
    }


    @Override
    public void onSwipe() {
        onSwipeListener.onSwipe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public void setOnSwipeListener(SwipeButton.OnSwipeListener onSwipeListener) {
        this.onSwipeListener = onSwipeListener;
    }

}
