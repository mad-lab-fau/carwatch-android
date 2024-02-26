package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public abstract class Slide extends Fragment {

    public Slide() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(getResourceId(), container, false);
    }

    protected abstract int getResourceId();
}
