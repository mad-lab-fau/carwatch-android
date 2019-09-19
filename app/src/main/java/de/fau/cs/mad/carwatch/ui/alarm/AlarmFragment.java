package de.fau.cs.mad.carwatch.ui.alarm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.databinding.FragmentAlarmBinding;
import de.fau.cs.mad.carwatch.db.Alarm;
import de.fau.cs.mad.carwatch.ui.AddAlarmActivity;

public class AlarmFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = AlarmFragment.class.getSimpleName();

    private AlarmRecyclerAdapter adapter;
    private CoordinatorLayout coordinatorLayout;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        AlarmViewModel alarmViewModel = ViewModelProviders.of(this).get(AlarmViewModel.class);

        FragmentAlarmBinding dataBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_alarm, container, false);
        View root = dataBinding.getRoot();
        dataBinding.setViewmodel(alarmViewModel);

        if (getActivity() != null) {
            coordinatorLayout = getActivity().findViewById(R.id.coordinator);
        }

        adapter = new AlarmRecyclerAdapter(this, coordinatorLayout);

        RecyclerView recyclerView = root.findViewById(R.id.alarm_recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        final TextView noAlarmsTextView = root.findViewById(R.id.tv_no_alarms);

        // Add an observer on the LiveData returned by getAllAlarms.
        alarmViewModel.getAllAlarms().observe(this, alarms -> {
            // Update the cached copy of the words in the adapter.
            adapter.setAlarms(alarms);
            noAlarmsTextView.setVisibility(alarms.size() == 0 ? View.VISIBLE : View.GONE);
        });

        FloatingActionButton fab = root.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        return root;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                Intent intent = new Intent(getContext(), AddAlarmActivity.class);
                startActivityForResult(intent, Constants.REQUEST_CODE_NEW_ALARM);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (data.hasExtra(Constants.EXTRA_ALARM)) {
                Alarm alarm = data.getParcelableExtra(Constants.EXTRA_ALARM);
                adapter.updateAlarm(alarm);
            }
        } else {
            Snackbar.make(coordinatorLayout, R.string.alarm_not_saved, Snackbar.LENGTH_SHORT).show();
        }
    }

}