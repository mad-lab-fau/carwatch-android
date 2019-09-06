package de.fau.cs.mad.carwatch.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.LocalTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.alarm.AlarmViewModel;

/**
 * Used to create and edit alarms, depending on REQUEST_CODE
 */
public class AddAlarmActivity extends AppCompatActivity {//implements SetRepeatDaysDialogFragment.OnDialogCompleteListener {

    private final String TAG = AddAlarmActivity.class.getSimpleName();

    private AlarmViewModel alarmViewModel;
    private int currRequestCode; // current request code - static values in Constants

    private Alarm alarm;
    // View objects
    private TextView timeTextView;
    private TextView repeatTextView;
    private FloatingActionButton deleteButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        deleteButton = findViewById(R.id.fab_add_alarm_delete);

        timeTextView = findViewById(R.id.add_alarm_time_text);
        repeatTextView = findViewById(R.id.add_alarm_repeat_text);

        // Get a new or existing ViewModel from the ViewModelProvider.
        alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        Intent intent = getIntent();

        if (intent.hasExtra(Constants.EXTRA_BUNDLE)) { // Activity called to edit an alarm.
            Bundle args = intent.getBundleExtra(Constants.EXTRA_BUNDLE);
            alarm = args.getParcelable(Constants.EXTRA_ALARM);
            currRequestCode = Constants.REQUEST_CODE_EDIT_ALARM;
        } else {
            currRequestCode = Constants.REQUEST_CODE_NEW_ALARM;
        }

        if (alarm != null) {
            timeTextView.setText(alarm.getStringTime());
            repeatTextView.setText(alarm.getStringOfActiveDays());
            setTitle(R.string.edit_alarm);
            addDeleteButtonListener();
        } else {
            alarm = new Alarm();
            alarm.setActiveDays(new boolean[7]);
            deleteButton.hide();
            setInitialAlarmTime();
            repeatTextView.setText(R.string.never);
        }

        addSetTimeLayoutListener();
        addSetRepeatLayoutListener();
    }


    /**
     * Initialize TimeTextView and Alarm's time with current time
     */
    private void setInitialAlarmTime() {
        // Get time and set it to alarm time TextView
        LocalTime time = LocalTime.now();

        String currentTime = time.toString("HH:mm");
        timeTextView.setText(currentTime);
        alarm.setTime(time.toDateTimeToday());
    }

    private void addDeleteButtonListener() {
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent replyIntent = new Intent();

                alarmViewModel.delete(alarm);
                alarm.setActive(false);
                replyIntent.putExtra(Constants.EXTRA_DELETE, true);
                replyIntent.putExtra(Constants.EXTRA_ALARM, alarm);

                setResult(RESULT_OK, replyIntent);
                finish();
            }
        });
    }

    /**
     * When timeChangeLayout is selected, open TimePickerDialog
     */
    private void addSetTimeLayoutListener() {
        // Get layout view
        RelativeLayout setTimeButton = findViewById(R.id.add_alarm_time_layout);

        setTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LocalTime time;
                if (alarm.getTime() == null) {
                    time = LocalTime.now();
                } else {
                    time = alarm.getTime().toLocalTime();
                }

                TimePickerDialog timePicker;
                timePicker = new TimePickerDialog(AddAlarmActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        LocalTime selectedTime = new LocalTime(selectedHour, selectedMinute);
                        alarm.setTime(selectedTime.toDateTimeToday());
                        timeTextView.setText(selectedTime.toString("HH:mm"));
                    }
                }, time.getHourOfDay(), time.getMinuteOfHour(), true);
                timePicker.setTitle("Select Time");
                timePicker.show();
            }
        });
    }

    /**
     * When repeatChangeLayout is selected, open SetRepeatDaysDialogFragment
     */
    private void addSetRepeatLayoutListener() {
        // Get layout view
        RelativeLayout setRepeatButton = findViewById(R.id.add_alarm_repeat_layout);

        setRepeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*SetRepeatDaysDialogFragment setRepeatDaysDialogFragment = new SetRepeatDaysDialogFragment();

                Bundle args = getBundle();
                setRepeatDaysDialogFragment.setArguments(args);
                // Display dialog
                setRepeatDaysDialogFragment.show(getSupportFragmentManager(), "A");*/
            }
        });
    }

    /**
     * Get Bundle of arguments for SetRepeatDaysDialogFragment, arguments include alarm's active days.
     */
    @NonNull
    private Bundle getBundle() {
        Bundle args = new Bundle();

        args.putBooleanArray(Constants.KEY_ACTIVE_DAYS, alarm.getActiveDays());
        return args;
    }

    /**
     * This method is called when SetRepeatDaysDialogFragment completes, we get the selectedDays
     * and apply that to alarm
     *
     * @param selectedDaysBools boolean array of selected days of the week
     */
    public void onDialogComplete(boolean[] selectedDaysBools) {
        alarm.setActiveDays(selectedDaysBools);
        String formattedActiveDays = Alarm.getStringOfActiveDays(alarm.getActiveDays());
        repeatTextView.setText(formattedActiveDays);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_alarm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_alarm_save) {
            Intent replyIntent = new Intent();
            if (currRequestCode == Constants.REQUEST_CODE_EDIT_ALARM) {
                alarmViewModel.update(alarm);
            } else {
                alarmViewModel.insert(alarm);
            }

            replyIntent.putExtra(Constants.EXTRA_ALARM, alarm);
            replyIntent.putExtra(Constants.EXTRA_EDIT, true);

            setResult(RESULT_OK, replyIntent);
            finish();

        }

        return super.onOptionsItemSelected(item);
    }
}
