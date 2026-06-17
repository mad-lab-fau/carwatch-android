package de.fau.cs.mad.carwatch.ui;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import de.fau.cs.mad.carwatch.ui.CarwatchSnackbar;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmSoundControl;
import de.fau.cs.mad.carwatch.logger.GenericFileProvider;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.ui.onboarding.SlideShowActivity;
import de.fau.cs.mad.carwatch.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int[] NAV_IDS = {R.id.navigation_wakeup, R.id.navigation_alarm, R.id.navigation_bedtime};

    private static DiskLogAdapter sAdapter;

    private SharedPreferences sharedPreferences;

    private CoordinatorLayout coordinatorLayout;
    private FloatingActionButton fabMenuToggle;
    private View fabMenuScrim;
    private View fabMenuContainer;
    private MaterialButton finishStudyDayButton;
    private TextView headerTitle;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // disable night mode per default
        AppCompatDelegate delegate = getDelegate();
        AppCompatDelegate.setDefaultNightMode(sharedPreferences.getBoolean(Constants.PREF_NIGHT_MODE_ENABLED, false) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        delegate.applyDayNight();
        HeaderUiHelper.configureTransparentStatusBar(this, sharedPreferences);

        initializeLoggingUtil(this);

        if (sharedPreferences.getInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE) != Constants.SLIDESHOW_FINISHED_SLIDE_ID) {
            Intent intent = new Intent(this, SlideShowActivity.class);
            startActivity(intent);
            finish();
        }

        coordinatorLayout = findViewById(R.id.coordinator);
        headerTitle = findViewById(R.id.tv_header_title);
        setupFabMenu();

        BottomNavigationView navView = findViewById(R.id.nav_view);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            throw new IllegalStateException("Missing navigation host fragment");
        }
        navController = navHostFragment.getNavController();

        int currentNavElement = getIntent().getIntExtra(
                Constants.EXTRA_TARGET_NAV_ELEMENT,
                sharedPreferences.getInt(Constants.PREF_CURRENT_NAV_ELEMENT, NAV_IDS[0])
        );
        navigate(currentNavElement);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            CharSequence label = destination.getLabel();
            headerTitle.setText(label == null ? getString(R.string.app_name) : label);
        });
        NavigationUI.setupWithNavController(navView, navController);


        if (getIntent() != null && getIntent().getBooleanExtra(Constants.EXTRA_SHOW_BARCODE_SCANNED_MSG, false)) {
            CarwatchSnackbar.show(coordinatorLayout, getString(R.string.message_barcode_scanned_successfully), CarwatchSnackbar.LENGTH_SHORT);
        }
        showPendingWakeupAlert();
        showEndOfDayAlert();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (navController != null && navController.getCurrentDestination() != null)
            sharedPreferences.edit().putInt(Constants.PREF_CURRENT_NAV_ELEMENT, navController.getCurrentDestination().getId()).apply();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferences.edit().putInt(Constants.PREF_CURRENT_NAV_ELEMENT, NAV_IDS[0]).apply();
    }

    public void navigate(int navId) {
        for (int id : NAV_IDS) {
            if (id == navId) {
                if (navController.getCurrentDestination() != null
                        && navController.getCurrentDestination().getId() == navId) {
                    return;
                }
                NavOptions navOptions = new NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                        .build();
                navController.navigate(navId, null, navOptions);
                return;
            }
        }
    }

    private void showPendingWakeupAlert() {
        String wakeupAlertType = sharedPreferences.getString(Constants.PREF_WAKEUP_ALERT_TYPE, null);
        if (wakeupAlertType == null) {
            return;
        }

        int delayedSampleMinutes = sharedPreferences.getInt(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES, 0);
        sharedPreferences.edit()
                .remove(Constants.PREF_WAKEUP_ALERT_TYPE)
                .remove(Constants.PREF_WAKEUP_DELAYED_SAMPLE_MINUTES)
                .apply();

        int titleId = wakeupAlertType.equals(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE)
                ? R.string.title_overdue_sample_pending
                : R.string.title_delayed_sample_planned;
        String message = wakeupAlertType.equals(Constants.WAKEUP_ALERT_OVERDUE_SAMPLE)
                ? getString(R.string.message_overdue_sample_pending)
                : getString(R.string.message_delayed_sample_planned, delayedSampleMinutes);

        new CarwatchDialogBuilder(this)
                .setIcon(R.drawable.ic_info_24dp)
                .setTitle(titleId)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> navigate(R.id.navigation_alarm))
                .show();
    }

    private void showEndOfDayAlert() {
        if (getIntent() == null) {
            return;
        }

        String alertType = getIntent().getStringExtra(Constants.EXTRA_END_OF_DAY_ALERT_TYPE);
        if (alertType == null) {
            return;
        }

        int titleId;
        int messageId = switch (alertType) {
            case Constants.END_OF_DAY_ALERT_EVENING_REQUIRED -> {
                titleId = R.string.title_samples_recorded;
                yield R.string.message_samples_recorded_evening_required;
            }
            case Constants.END_OF_DAY_ALERT_STUDY_FINISHED -> {
                titleId = R.string.title_study_finished;
                yield R.string.message_study_finished;
            }
            default -> {
                titleId = R.string.title_samples_recorded;
                yield R.string.message_samples_recorded_day_finished;
            }
        };

        Drawable icon = ContextCompat.getDrawable(this, R.drawable.ic_check_circle_24dp);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(this, R.color.colorGreen500));
        }

        new CarwatchDialogBuilder(this)
                .setIcon(icon)
                .setTitle(titleId)
                .setMessage(messageId)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    public static void initializeLoggingUtil(Context context) {
        if (sAdapter != null)
            return;
        sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(context)) {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return true;
            }
        };
        Logger.addLogAdapter(sAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }
    }

    private void setupFabMenu() {
        fabMenuToggle = findViewById(R.id.fab_menu_toggle);
        fabMenuScrim = findViewById(R.id.fab_menu_scrim);
        fabMenuContainer = findViewById(R.id.fab_menu_container);
        finishStudyDayButton = findViewById(R.id.fab_menu_finish_study_day);

        fabMenuToggle.setOnClickListener(view -> openFabMenu());
        findViewById(R.id.fab_menu_close).setOnClickListener(view -> closeFabMenu());
        fabMenuScrim.setOnClickListener(view -> closeFabMenu());
        fabMenuContainer.setOnClickListener(view -> {
            // Keep taps on menu controls from bubbling to the outside scrim.
        });

        setFabMenuAction(R.id.fab_menu_share, R.id.menu_share);
        setFabMenuAction(R.id.fab_menu_delete_logs, R.id.menu_delete_log_files);
        setFabMenuAction(R.id.fab_menu_kill, R.id.menu_kill);
        setFabMenuAction(R.id.fab_menu_reregister, R.id.menu_reregister);
        setFabMenuAction(R.id.fab_menu_show_tutorial, R.id.menu_show_tutorial);
        setFabMenuAction(R.id.fab_menu_study_information, R.id.menu_study_information);
        setFabMenuAction(R.id.fab_menu_finish_study_day, R.id.menu_finish_study_day);
        setFabMenuAction(R.id.fab_menu_app_info, R.id.menu_app_info);
    }

    private void setFabMenuAction(int buttonId, int menuItemId) {
        findViewById(buttonId).setOnClickListener(view -> {
            closeFabMenu();
            handleMenuAction(menuItemId);
        });
    }

    private void openFabMenu() {
        updateFinishStudyDayFabState();
        fabMenuToggle.setVisibility(View.GONE);
        fabMenuScrim.setAlpha(0f);
        fabMenuContainer.setAlpha(0f);
        fabMenuContainer.setTranslationY(-12f);
        fabMenuScrim.setVisibility(View.VISIBLE);
        fabMenuScrim.animate().alpha(1f).setDuration(120).start();
        fabMenuContainer.animate().alpha(1f).translationY(0f).setDuration(160).start();
    }

    private void closeFabMenu() {
        if (fabMenuScrim.getVisibility() != View.VISIBLE) {
            return;
        }

        fabMenuContainer.animate().alpha(0f).translationY(-12f).setDuration(100).start();
        fabMenuScrim.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    fabMenuScrim.setVisibility(View.GONE);
                    fabMenuToggle.setVisibility(View.VISIBLE);
                })
                .start();
    }

    private void updateFinishStudyDayFabState() {
        boolean canFinishStudyDay = AlarmHandler.canFinishCurrentStudyDay(this);
        finishStudyDayButton.setEnabled(canFinishStudyDay);
        finishStudyDayButton.setAlpha(canFinishStudyDay ? 1f : 0.56f);
    }

    private void handleMenuAction(int itemId) {
        if (itemId == R.id.menu_share) {
            String studyName = sharedPreferences.getString(Constants.PREF_STUDY_NAME, null);
            String participantId = sharedPreferences.getString(Constants.PREF_PARTICIPANT_ID, null);

            try {
                File zipFile = LoggerUtil.zipDirectory(this, studyName, participantId);
                createFileShareDialog(zipFile);
            } catch (FileNotFoundException e) {
                CarwatchSnackbar.show(coordinatorLayout, Objects.requireNonNull(e.getMessage()), CarwatchSnackbar.LENGTH_SHORT);
            } catch (ActivityNotFoundException | IllegalArgumentException | IOException e) {
                Log.e(TAG, "Unable to share log files", e);
                CarwatchSnackbar.show(coordinatorLayout, R.string.message_share_logs_failed, CarwatchSnackbar.LENGTH_SHORT);
            }
        } else if (itemId == R.id.menu_delete_log_files) {
            showDeleteLogFilesWarningDialog();
        } else if (itemId == R.id.menu_kill) {
            showKillWarningDialog();
        } else if (itemId == R.id.menu_reregister) {
            requestReregistration();
        } else if (itemId == R.id.menu_show_tutorial) {
            sharedPreferences.edit().putInt(Constants.PREF_CURRENT_SLIDE_SHOW_SLIDE, Constants.INITIAL_SLIDE_SHOW_SLIDE).apply();
            Intent tutorialIntent = new Intent(this, SlideShowActivity.class);
            tutorialIntent.putExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SlideShowActivity.SHOW_TUTORIAL_SLIDES);
            startActivity(tutorialIntent);
        } else if (itemId == R.id.menu_study_information) {
            showStudyInformationDialog();
        } else if (itemId == R.id.menu_finish_study_day) {
            requestFinishStudyDay();
        } else if (itemId == R.id.menu_app_info) {
            showAppInfoDialog();
        }
    }

    private void requestReregistration() {
        if (!AlarmHandler.isStudyOngoing(this)) {
            performReregistration();
            return;
        }

        new CarwatchDialogBuilder(this)
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(R.string.title_reregister_ongoing_study)
                .setMessage(R.string.message_reregister_ongoing_study)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.menu_reregister, (dialog, which) -> performReregistration())
                .show();
    }

    private void performReregistration() {
        AlarmHandler.resetStudyConfiguration(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        Intent intent = new Intent(this, SlideShowActivity.class);
        intent.putExtra(Constants.EXTRA_SLIDE_SHOW_TYPE, SlideShowActivity.SHOW_ALL_SLIDES);
        startActivity(intent);
        finish();
    }

    private void createFileShareDialog(File zipFile) throws IOException {
        File shareFile = copyToShareCache(zipFile);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri uri = GenericFileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() +
                        ".logger.fileprovider",
                shareFile);
        String extra_email = sharedPreferences.getString(Constants.PREF_SHARE_EMAIL_ADDRESS, "");
        sharingIntent.setType("application/zip");
        sharingIntent.setClipData(ClipData.newUri(getContentResolver(), shareFile.getName(), uri));
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{extra_email});
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareFile.getName());
        Intent chooser = Intent.createChooser(sharingIntent, getString(R.string.title_share_dialog));
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        chooser.setClipData(ClipData.newUri(getContentResolver(), shareFile.getName(), uri));
        startActivity(chooser);
    }

    private File copyToShareCache(File source) throws IOException {
        File shareDirectory = new File(getCacheDir(), "shared_logs");
        if (!shareDirectory.exists() && !shareDirectory.mkdirs()) {
            throw new IOException("Could not create share cache directory.");
        }

        File target = new File(shareDirectory, source.getName());
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target, false)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        }
        return target;
    }

    private void showDeleteLogFilesWarningDialog() {
        AlertDialog dialog = new CarwatchDialogBuilder(this)
                .setCancelable(false)
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(R.string.title_delete_log_files)
                .setMessage(R.string.message_delete_log_files_confirm_dialog)
                .setPositiveButton(R.string.menu_delete_logs, (dialogInterface, which) -> deleteLogFiles())
                .setNegativeButton(R.string.cancel, ((dialogInterface, which) -> { }))
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.md_theme_error));
    }

    private void deleteLogFiles() {
        boolean fileWereDeleted = LoggerUtil.deleteLogFiles(this);
        String msg = fileWereDeleted ? getString(R.string.message_all_log_files_deleted) : getString(R.string.message_not_all_log_files_deleted);
        CarwatchSnackbar.show(coordinatorLayout, msg, CarwatchSnackbar.LENGTH_SHORT);
    }

    private void requestFinishStudyDay() {
        if (!AlarmHandler.canFinishCurrentStudyDay(this)) {
            invalidateOptionsMenu();
            return;
        }

        AlertDialog dialog = new CarwatchDialogBuilder(this)
                .setIcon(R.drawable.ic_check_circle_24dp)
                .setTitle(R.string.title_finish_study_day)
                .setMessage(R.string.message_finish_study_day)
                .setNegativeButton(R.string.button_keep_samples, null)
                .setPositiveButton(R.string.button_finish_day, (dialogInterface, which) -> finishStudyDay())
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.md_theme_error));
    }

    private void finishStudyDay() {
        if (AlarmHandler.finishCurrentStudyDay(this)) {
            CarwatchSnackbar.show(coordinatorLayout, R.string.message_study_day_finished, CarwatchSnackbar.LENGTH_SHORT);
            navigate(R.id.navigation_alarm);
        }
        updateFinishStudyDayFabState();
    }

    private void showStudyInformationDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.widget_study_information_dialog, null);
        Drawable icon = Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.ic_school_24dp));
        icon.setTint(ContextCompat.getColor(this, R.color.colorPrimary));

        setDetailRow(dialogView, R.id.row_study_name, R.string.label_study_name, getPreferenceString(Constants.PREF_STUDY_NAME));
        setDetailRow(dialogView, R.id.row_participant_id, R.string.label_participant_id, getPreferenceString(Constants.PREF_PARTICIPANT_ID));
        setDetailRow(dialogView, R.id.row_study_days, R.string.label_study_days, String.valueOf(sharedPreferences.getInt(Constants.PREF_NUM_DAYS, 0)));
        setDetailRow(dialogView, R.id.row_contact_email, R.string.label_contact_email, getPreferenceString(Constants.PREF_SHARE_EMAIL_ADDRESS));
        setDetailRow(dialogView, R.id.row_interval_samples, R.string.label_interval_samples, formatIntervalSamples(sharedPreferences.getString(Constants.PREF_SALIVA_DISTANCES, "")));
        setDetailRow(dialogView, R.id.row_fixed_sample_times, R.string.label_fixed_sample_times, formatFixedSampleTimes(sharedPreferences.getString(Constants.PREF_SALIVA_TIMES, "")));
        setDetailRow(dialogView, R.id.row_evening_sample, R.string.label_evening_sample, getString(sharedPreferences.getBoolean(Constants.PREF_HAS_EVENING, false) ? R.string.yes : R.string.no));

        AlertDialog dialog = new CarwatchDialogBuilder(this)
                .setIcon(icon)
                .setTitle(R.string.title_study_information)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, null)
                .show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.bg_study_information_dialog));
        }
    }

    private void setDetailRow(View root, int rowId, int labelId, String value) {
        View row = root.findViewById(rowId);
        TextView labelView = row.findViewById(R.id.tv_detail_label);
        TextView valueView = row.findViewById(R.id.tv_detail_value);
        labelView.setText(labelId);
        valueView.setText(value);
    }

    private String getPreferenceString(String key) {
        String value = sharedPreferences.getString(key, "");
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }

    private String formatIntervalSamples(String intervalSamples) {
        List<String> values = splitPreferenceList(intervalSamples);
        if (values.isEmpty()) {
            return "-";
        }

        List<String> formattedValues = new ArrayList<>();
        for (String value : values) {
            formattedValues.add(value + " min");
        }
        return String.join(", ", formattedValues);
    }

    private String formatFixedSampleTimes(String fixedSampleTimes) {
        List<String> values = splitPreferenceList(fixedSampleTimes);
        if (values.isEmpty()) {
            return getString(R.string.message_no_fixed_sample_times);
        }

        List<String> formattedTimes = new ArrayList<>();
        for (String value : values) {
            formattedTimes.add(formatFixedSampleTime(value));
        }
        return String.join(", ", formattedTimes);
    }

    private List<String> splitPreferenceList(String value) {
        List<String> values = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return values;
        }

        String[] parts = value.split(Constants.QR_PARSER_LIST_SEPARATOR);
        for (String part : parts) {
            String trimmedValue = part.trim();
            if (!trimmedValue.isEmpty()) {
                values.add(trimmedValue);
            }
        }
        return values;
    }

    private String formatFixedSampleTime(String value) {
        if (value.length() != 4) {
            return value;
        }

        try {
            int hour = Integer.parseInt(value.substring(0, 2));
            int minute = Integer.parseInt(value.substring(2, 4));
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.getTime());
        } catch (NumberFormatException e) {
            return value;
        }
    }

    public void showKillWarningDialog() {
        AlertDialog dialog = new CarwatchDialogBuilder(this)
                .setCancelable(false)
                .setIcon(R.drawable.ic_warning_24dp)
                .setTitle(getString(R.string.title_kill_alarms))
                .setMessage(getString(R.string.message_kill_alarms))
                .setPositiveButton(R.string.menu_kill, (dialogInterface, which) -> {
                    AlarmHandler.killAll(getApplication());
                    AlarmSoundControl.getInstance().stopAlarmSound();
                    NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null)
                        notificationManager.cancelAll();
                })
                .setNegativeButton(R.string.cancel, ((dialogInterface, which) -> {
                }))
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.md_theme_error));
    }

    private void showAppInfoDialog() {
        AppInfoDialog dialog = new AppInfoDialog();
        dialog.show(getSupportFragmentManager(), "app_info");
    }
}
