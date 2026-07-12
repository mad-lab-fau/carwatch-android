package de.fau.cs.mad.carwatch.debug;

import android.app.Activity;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;

import de.fau.cs.mad.carwatch.BuildConfig;

/** Entry point whose implementation is present only in debug builds. */
public final class DemoStudyLoader {
    private static final String TAG = DemoStudyLoader.class.getSimpleName();
    private static final String DEBUG_LOADER_CLASS =
            "de.fau.cs.mad.carwatch.debug.DebugDemoStudyLoader";

    private DemoStudyLoader() {}

    public static boolean isAvailable() {
        return BuildConfig.DEBUG;
    }

    public static void load(Activity activity, Runnable onLoaded) {
        if (!BuildConfig.DEBUG) {
            return;
        }

        try {
            Class.forName(DEBUG_LOADER_CLASS)
                    .getMethod("load", Activity.class, Runnable.class)
                    .invoke(null, activity, onLoaded);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException
                 | InvocationTargetException e) {
            Log.e(TAG, "Could not load debug demo study", e);
        }
    }
}
