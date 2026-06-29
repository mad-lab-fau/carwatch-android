/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fau.cs.mad.carwatch.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.fau.cs.mad.carwatch.Constants;
import de.fau.cs.mad.carwatch.barcodedetection.camera.CameraSizePair;

import static androidx.core.content.ContextCompat.checkSelfPermission;

/**
 * Utility class to provide helper methods.
 */
@SuppressWarnings("deprecation")
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    /**
     * If the absolute difference between aspect ratios is less than this tolerance, they are
     * considered to be the same aspect ratio.
     */
    public static final float ASPECT_RATIO_TOLERANCE = 0.01f;


    public static final int REQUEST_CODE_RUNTIME_PERMISSIONS = 0;

    public static boolean requestRuntimePermissions(Activity activity) {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRuntimePermissionsToRequest()) {
            if (checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity, allNeededPermissions.toArray(new String[0]), REQUEST_CODE_RUNTIME_PERMISSIONS);
            return true;
        }
        return false;
    }

    public static boolean allPermissionsGranted(Context context) {
        for (String permission : getRequiredRuntimePermissions()) {
            if (checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void openBatteryOptimizationSettings(Context context) {
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
        }

        context.startActivity(intent);
    }

    public static boolean batteryOptimizationIgnored(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null)
                return pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    private static String[] getRequiredRuntimePermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);

        return permissions.toArray(new String[0]);
    }

    private static String[] getRuntimePermissionsToRequest() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return permissions.toArray(new String[0]);
    }

    public static boolean isPortraitMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Generates a list of acceptable preview sizes. Preview sizes are not acceptable if there is not
     * a corresponding picture size of the same aspect ratio. If there is a corresponding picture size
     * of the same aspect ratio, the picture size is paired up with the preview size.
     *
     * <p>This is necessary because even if we don't use still pictures, the still picture size must
     * be set to a size that is the same aspect ratio as the preview size we choose. Otherwise, the
     * preview images may be distorted on some devices.
     */
    public static List<CameraSizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        List<CameraSizePair> validPreviewSizes = new ArrayList<>();
        for (Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            // By looping through the picture sizes in order, we favor the higher resolutions.
            // We choose the highest resolution in order to support taking the full resolution
            // picture later.
            for (Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new CameraSizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all of
        // the preview sizes and hope that the camera can handle it.  Probably unlikely, but we still
        // account for it.
        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size.");
            for (Camera.Size previewSize : supportedPreviewSizes) {
                // The null picture size will let us know that we shouldn't set a picture size.
                validPreviewSizes.add(new CameraSizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }

    /**
     * Convert shared preferences string encoding a list of integers back to an int[]
     */
    public static int[] decodeArrayFromString(String input) {
        if (input == null || input.isEmpty())
            return new int[0];

        String[] list = input.split(Constants.QR_PARSER_LIST_SEPARATOR);
        int[] output = new int[list.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = Integer.parseInt(list[i]);
        }
        return output;
    }

    public static String translateTimestamp(long timestamp) {
        return new SimpleDateFormat("EE MMM dd yyyy HH:mm:ss ZZZZ", Locale.ENGLISH).format(timestamp);
    }
}
