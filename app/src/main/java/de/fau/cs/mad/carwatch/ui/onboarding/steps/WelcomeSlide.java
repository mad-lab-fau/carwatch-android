package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import androidx.databinding.ObservableBoolean;
import androidx.fragment.app.Fragment;

public interface WelcomeSlide {
    Fragment getFragment();

    ObservableBoolean getSkipButtonIsVisible();

    ObservableBoolean getNextButtonIsEnabled();

    void onSlideFinished();
}
