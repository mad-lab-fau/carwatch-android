package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;

/**
 * Fragment used to display Alarm active days to user
 */
public class RepeatDaysDialogFragment extends DialogFragment {

    /**
     * Listener for Dialog Completion
     * <p>
     * Implemented in AddAlarmActivity
     */
    public interface OnDialogCompleteListener {
        void onDialogComplete(boolean[] selectedDays);
    }

    private OnDialogCompleteListener completeListener;

    private boolean[] activeDays;

    public RepeatDaysDialogFragment(OnDialogCompleteListener listener) {
        completeListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activeDays = getArguments().getBooleanArray(Constants.KEY_ACTIVE_DAYS);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        // Set the dialog title
        builder.setTitle(R.string.set_repeat);
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
        builder.setMultiChoiceItems(R.array.days_of_week, activeDays,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedDay,
                                        boolean isChecked) {
                        activeDays[selectedDay] = isChecked;
                    }
                });
        // Set the action buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // Return selectedDays to activity
                saveSelectedDays(activeDays);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        return builder.create();
    }

    /**
     * Saved selectedDays by calling completeListener's complete function
     *
     * @param selectedDays ArrayList of selected days ints
     */
    private void saveSelectedDays(boolean[] selectedDays) {
        completeListener.onDialogComplete(selectedDays);
    }
}
