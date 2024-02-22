package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.fau.cs.mad.carwatch.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WelcomeText#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WelcomeText extends Fragment {

    public WelcomeText() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment WelcomeText.
     */
    public static WelcomeText newInstance() {
        return new WelcomeText();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_welcome_text, container, false);
    }
}