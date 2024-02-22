package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.fau.cs.mad.carwatch.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PermissionRequest#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PermissionRequest extends Fragment {

    public PermissionRequest() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PermissionRequest.
     */
    public static PermissionRequest newInstance() {
        return new PermissionRequest();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_permission_request, container, false);
    }
}