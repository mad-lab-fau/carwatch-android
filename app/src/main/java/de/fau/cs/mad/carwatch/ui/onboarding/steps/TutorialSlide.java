package de.fau.cs.mad.carwatch.ui.onboarding.steps;

import android.os.Bundle;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import de.fau.cs.mad.carwatch.R;

public class TutorialSlide extends BaseWelcomeSlide {

    private static final String ARG_HEADLINE = "headline";
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_IMAGE_ID = "imageId";
    private static final String ARG_CAN_SHOW_PREVIOUS_SLIDE = "canShowPreviousSlide";
    private String headline;
    private String description;
    private int imageId;

    public static TutorialSlide newInstance(String headline, String description, int imageId, boolean canShowPreviousSlide) {
        TutorialSlide fragment = new TutorialSlide();
        Bundle args = new Bundle();
        args.putString(ARG_HEADLINE, headline);
        args.putString(ARG_DESCRIPTION, description);
        args.putInt(ARG_IMAGE_ID, imageId);
        args.putBoolean(ARG_CAN_SHOW_PREVIOUS_SLIDE, canShowPreviousSlide);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        canShowNextSlide.set(true);
        isSkipButtonVisible.set(true);
        if (getArguments() != null) {
            headline = getArguments().getString(ARG_HEADLINE);
            description = getArguments().getString(ARG_DESCRIPTION);
            imageId = getArguments().getInt(ARG_IMAGE_ID);
            canShowPreviousSlide.set(getArguments().getBoolean(ARG_CAN_SHOW_PREVIOUS_SLIDE));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        if (root == null) {
            return null;
        }

        TextView titleText = root.findViewById(R.id.tv_tutorial_headline);
        TextView descriptionText = root.findViewById(R.id.tv_tutorial_description);
        ImageView screenView = root.findViewById(R.id.iv_screen_view);

        titleText.setText(headline);
        descriptionText.setText(createDescriptionWithInlineIcons(description));
        screenView.setImageResource(imageId);

        DisplayMetrics displayMetrics = requireContext().getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;

        if (dpHeight < 900) {
            titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            descriptionText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            screenView.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, displayMetrics);
        }
        return root;
    }

    private CharSequence createDescriptionWithInlineIcons(String text) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        replaceIconPlaceholder(builder, "{sample_taken_icon}", R.drawable.ic_check);
        replaceIconPlaceholder(builder, "{sample_due_icon}", R.drawable.ic_pending);
        replaceIconPlaceholder(builder, "{sample_later_icon}", R.drawable.ic_hourglass);
        return builder;
    }

    private void replaceIconPlaceholder(SpannableStringBuilder builder, String placeholder, int drawableId) {
        int start = builder.toString().indexOf(placeholder);
        while (start >= 0) {
            int end = start + placeholder.length();
            builder.replace(start, end, " ");

            Drawable drawable = ContextCompat.getDrawable(requireContext(), drawableId);
            if (drawable != null) {
                int iconSize = Math.round(TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        18,
                        getResources().getDisplayMetrics()
                ));
                drawable.setBounds(0, 0, iconSize, iconSize);
                builder.setSpan(
                        new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
                        start,
                        start + 1,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }

            start = builder.toString().indexOf(placeholder, start + 1);
        }
    }

    @Override
    protected int getResourceId() {
        return R.layout.fragment_tutorial_slide;
    }
}
