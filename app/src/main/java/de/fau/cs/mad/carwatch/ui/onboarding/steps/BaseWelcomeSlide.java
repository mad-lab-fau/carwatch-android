package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.Fragment;

public abstract class BaseWelcomeSlide extends Fragment implements WelcomeSlide {

    protected final ObservableBoolean isSkipButtonVisible = new ObservableBoolean(false);
    protected final ObservableBoolean canShowNextSlide = new ObservableBoolean(false);
    protected final ObservableBoolean canShowPreviousSlide = new ObservableBoolean(false);

    public BaseWelcomeSlide() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(getResourceId(), container, false);
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    @Override
    public ObservableBoolean getSkipButtonIsVisible() {
        return isSkipButtonVisible;
    }

    @Override
    public ObservableBoolean getCanShowNextSlide() {
        return canShowNextSlide;
    }

    @Override
    public ObservableBoolean getCanShowPreviousSlide() {
        return canShowPreviousSlide;
    }

    @Override
    public void onSlideFinished() {
        // Default: do nothing
    }

    protected abstract int getResourceId();
}
