package de.fau.cs.mad.carwatch.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;

import org.joda.time.DateTime;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.alarmmanager.AlarmHandler;
import de.fau.cs.mad.carwatch.barcodedetection.BarcodeResultFragment;
import de.fau.cs.mad.carwatch.logger.GenericFileProvider;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;
import de.fau.cs.mad.carwatch.util.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int[] NAV_IDS = {R.id.navigation_wakeup, R.id.navigation_alarm, R.id.navigation_bedtime};

    private static DiskLogAdapter sAdapter;

    private SharedPreferences sharedPreferences;

    private CoordinatorLayout coordinatorLayout;

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN, true)) {
            showSubjectIdDialog();
        }

        coordinatorLayout = findViewById(R.id.coordinator);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(NAV_IDS).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // TODO navigate based on current time
        navigate(R.id.navigation_alarm);

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        if (sAdapter == null) {
            sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(this)) {
                @Override
                public boolean isLoggable(int priority, @Nullable String tag) {
                    return true;
                }
            };
            Logger.addLogAdapter(sAdapter);
        }
    }

    public void navigate(int navId) {
        for (int id : NAV_IDS) {
            if (id == navId) {
                navController.navigate(navId);
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Utils.allPermissionsGranted(this)) {
            Utils.requestRuntimePermissions(this);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        BarcodeResultFragment.dismiss(getSupportFragmentManager());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_share:
                String subjectId = sharedPreferences.getString(Constants.PREF_SUBJECT_ID, null);

                try {
                    File zipFile = LoggerUtil.zipDirectory(this, subjectId);
                    createFileShareDialog(zipFile);
                } catch (FileNotFoundException e) {
                    Snackbar.make(coordinatorLayout, Objects.requireNonNull(e.getMessage()), Snackbar.LENGTH_SHORT).show();
                }
                break;
            // TODO REMOVE (OR HIDE BETTER)
            case R.id.menu_kill:
                AlarmHandler.killAll(getApplication());
                break;
            case R.id.menu_scan:
                startActivity(new Intent(this, ScannerActivity.class));
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void showSubjectIdDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View dialogView = getLayoutInflater().inflate(R.layout.widget_subject_id_dialog, null);
        final EditText editText = dialogView.findViewById(R.id.edit_text_subject_id);
        editText.setText(sharedPreferences.getString(Constants.PREF_SUBJECT_ID, ""));

        dialogBuilder
                .setCancelable(false)
                .setTitle(getString(R.string.title_subject_id))
                .setMessage(getString(R.string.message_subject_id))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    String subjectId = editText.getText().toString();

                    sharedPreferences.edit()
                            .putBoolean(Constants.PREF_FIRST_RUN, false)
                            .putString(Constants.PREF_SUBJECT_ID, subjectId)
                            .putInt(Constants.PREF_DAY_ID, 0)
                            .apply();
                });

        dialogBuilder.show();
    }

    private void createFileShareDialog(File zipFile) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        Uri uri = GenericFileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() +
                        ".logger.fileprovider",
                zipFile);
        sharingIntent.setType("application/octet-stream");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        // TODO change email address
        sharingIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"carwatch_logs@gmail.com"});
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, zipFile.getName());
        startActivity(Intent.createChooser(sharingIntent, "Share Logs via..."));
    }

}
