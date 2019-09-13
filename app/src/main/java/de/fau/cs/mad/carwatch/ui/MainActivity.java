package de.fau.cs.mad.carwatch.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.orhanobut.logger.DiskLogAdapter;
import com.orhanobut.logger.Logger;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.logger.LoggerUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    static DiskLogAdapter sAdapter;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (sharedPreferences.getBoolean(Constants.PREF_FIRST_RUN, true)) {
            showSubjectIdDialog();
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_alarm, R.id.navigation_scanner).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navController.navigate(R.id.navigation_alarm);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        if (sAdapter == null) {
            sAdapter = new DiskLogAdapter(LoggerUtil.getFormatStrategy(this)) {
                @Override
                public boolean isLoggable(int priority, @Nullable String tag) {
                    return priority == Logger.INFO;
                }
            };
            Logger.addLogAdapter(sAdapter);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void showSubjectIdDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        final View dialogView = getLayoutInflater().inflate(R.layout.widget_subject_id_dialog, null);
        EditText editText = dialogView.findViewById(R.id.edit_text_subject_id);
        editText.setText(sharedPreferences.getString(Constants.PREF_SUBJECT_ID, ""));

        dialogBuilder
                .setCancelable(false)
                .setTitle(getString(R.string.title_subject_id))
                .setMessage(getString(R.string.message_subject_id))
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EditText editText = dialogView.findViewById(R.id.edit_text_subject_id);
                        String subjectId = editText.getText().toString();

                        sharedPreferences.edit()
                                .putBoolean(Constants.PREF_FIRST_RUN, false)
                                .putString(Constants.PREF_SUBJECT_ID, subjectId)
                                .apply();
                    }
                });

        dialogBuilder.show();
    }
}
