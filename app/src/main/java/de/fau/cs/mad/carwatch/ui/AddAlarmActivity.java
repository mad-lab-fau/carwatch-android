package de.fau.cs.mad.carwatch.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.subject.Condition;
import de.fau.cs.mad.carwatch.subject.SubjectMap;
import de.fau.cs.mad.carwatch.ui.alarm.AlarmViewModel;
import de.fau.cs.mad.carwatch.ui.alarm.RepeatDaysDialogFragment;

/**
 * Used to create and edit alarms, depending on REQUEST_CODE
 */
public class AddAlarmActivity extends AppCompatActivity implements RepeatDaysDialogFragment.OnDialogCompleteListener {

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
            if (args != null) {
                alarm = args.getParcelable(Constants.EXTRA_ALARM);
                currRequestCode = Constants.REQUEST_CODE_EDIT_ALARM;
            }
        } else {
            currRequestCode = Constants.REQUEST_CODE_NEW_ALARM;
        }

        if (alarm != null) {
            timeTextView.setText(alarm.getStringTime());
            repeatTextView.setText(alarm.getStringOfActiveDays(this));
            setTitle(R.string.edit_alarm);
            addDeleteButtonListener();
        } else {
            alarm = new Alarm();
            alarm.setActiveDays(new boolean[7]);
            deleteButton.hide();
            setInitialAlarmTime();
            repeatTextView.setText(R.string.never);
        }

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String subjectId = sp.getString(Constants.PREF_SUBJECT_ID, null);
        int dayId = sp.getInt(Constants.PREF_DAY_COUNTER, 0);

        if (subjectId != null && SubjectMap.getConditionForSubject(subjectId) == Condition.UNKNOWN_ALARM) {
            int hiddenDelta;
            if (dayId >= 0 && dayId < Constants.DELTA_HIDDEN_ALARMS.length) {
                hiddenDelta = Constants.DELTA_HIDDEN_ALARMS[dayId];
            } else {
                hiddenDelta = Constants.DELTA_HIDDEN_ALARMS[Constants.DELTA_HIDDEN_ALARMS.length - 1];
            }

            alarm.setHiddenDelta(hiddenDelta);
        }

        addSetTimeLayoutListener();
        addSetRepeatLayoutListener();
    }


    /**
     * Initialize TimeTextView and Alarm's time with current time
     */
    private void setInitialAlarmTime() {
        // Get time and set it to alarm time TextView
        DateTime time = DateTime.now();

        String currentTime = time.toString("HH:mm");
        timeTextView.setText(currentTime);
        alarm.setTime(time);
    }

    private void addDeleteButtonListener() {
        deleteButton.setOnClickListener(view -> {
            Intent replyIntent = new Intent();

            alarmViewModel.delete(alarm);
            alarm.setActive(false);
            replyIntent.putExtra(Constants.EXTRA_DELETE, true);
            replyIntent.putExtra(Constants.EXTRA_ALARM, alarm);

            setResult(RESULT_OK, replyIntent);
            finish();
        });
    }

    /**
     * When timeChangeLayout is selected, open TimePickerDialog
     */
    private void addSetTimeLayoutListener() {
        // Get layout view
        RelativeLayout setTimeButton = findViewById(R.id.add_alarm_time_layout);

        setTimeButton.setOnClickListener(view -> {
            DateTime time;
            if (alarm.getTime() == null) {
                time = DateTime.now();
            } else {
                time = alarm.getTime();
            }

            TimePickerDialog timePicker = new TimePickerDialog(AddAlarmActivity.this, (timePicker1, selectedHour, selectedMinute) -> {
                LocalTime selectedTime = new LocalTime(selectedHour, selectedMinute);
                alarm.setTime(selectedTime.toDateTimeToday());
                timeTextView.setText(selectedTime.toString("HH:mm"));
            }, time.getHourOfDay(), time.getMinuteOfHour(), true);
            timePicker.show();
        });
    }

    /**
     * When repeatChangeLayout is selected, open SetRepeatDaysDialogFragment
     */
    private void addSetRepeatLayoutListener() {
        // Get layout view
        RelativeLayout setRepeatButton = findViewById(R.id.add_alarm_repeat_layout);

        setRepeatButton.setOnClickListener(view -> {
            RepeatDaysDialogFragment setRepeatDaysDialogFragment = new RepeatDaysDialogFragment(AddAlarmActivity.this);

            Bundle args = getBundle();
            setRepeatDaysDialogFragment.setArguments(args);
            // Display dialog
            setRepeatDaysDialogFragment.show(getSupportFragmentManager(), RepeatDaysDialogFragment.class.getSimpleName());
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
     * @param selectedDays boolean array of selected days of the week
     */
    public void onDialogComplete(boolean[] selectedDays) {
        alarm.setActiveDays(selectedDays);
        String formattedActiveDays = Alarm.getStringOfActiveDays(this, alarm.getActiveDays());
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

        if (id == R.id.action_alarm_save) {
            Intent replyIntent = new Intent();

            alarm.setActive(true);

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
