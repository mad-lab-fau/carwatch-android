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

package de.fau.cs.mad.carwatch.barcodedetection;

import static java.lang.Math.min;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.barcode.common.Barcode;

import de.fau.cs.mad.carwatch.R;
import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;

abstract class BarcodeGraphicBase extends GraphicOverlay.Graphic {

    private final Paint boxPaint;
    private final Paint scrimPaint;
    private final Paint eraserPaint;

    final int boxCornerRadius;
    final Paint pathPaint;
    final RectF boxRect;

    private static final float REL_BOX_HEIGHT_DEFAULT = 0.8f;
    private static final float REL_BOX_WIDTH_DEFAULT = 0.8f;
    private static final float REL_BOX_HEIGHT_EAN8 = 0.35f;

    BarcodeGraphicBase(GraphicOverlay overlay, int barcodeFormat) {
        super(overlay);

        boxPaint = new Paint();
        boxPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_stroke));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width));

        scrimPaint = new Paint();
        scrimPaint.setColor(ContextCompat.getColor(context, R.color.barcode_reticle_background));
        eraserPaint = new Paint();
        eraserPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        boxCornerRadius = context.getResources().getDimensionPixelOffset(R.dimen.barcode_reticle_corner_radius);

        pathPaint = new Paint();
        pathPaint.setColor(Color.WHITE);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(boxPaint.getStrokeWidth());
        pathPaint.setPathEffect(new CornerPathEffect(boxCornerRadius));

        boxRect = getBarcodeReticleBox(overlay, barcodeFormat);
    }

    @Override
    protected void draw(Canvas canvas) {
        // Draws the dark background scrim and leaves the box area clear.
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), scrimPaint);
        // As the stroke is always centered, so erase twice with FILL and STROKE respectively to clear
        // all area that the box rect would occupy.
        eraserPaint.setStyle(Style.FILL);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);
        eraserPaint.setStyle(Style.STROKE);
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, eraserPaint);

        // Draws the box.
        canvas.drawRoundRect(boxRect, boxCornerRadius, boxCornerRadius, boxPaint);
    }

    private static RectF getBarcodeReticleBox(GraphicOverlay overlay, int barcodeFormat) {
        float relBoxHeight;
        if (barcodeFormat == Barcode.FORMAT_EAN_8) {
            relBoxHeight = REL_BOX_HEIGHT_EAN8;
        } else {
            relBoxHeight = REL_BOX_HEIGHT_DEFAULT;
        }
        float relBoxWidth = REL_BOX_WIDTH_DEFAULT;

        float overlayWidth = overlay.getWidth();
        float overlayHeight = overlay.getHeight();
        float overlayMin = min(overlayHeight, overlayWidth);
        float boxWidth = overlayMin * relBoxWidth;
        float boxHeight = overlayMin * relBoxHeight;
        float cx = overlayWidth / 2;
        float cy = overlayHeight / 2;
        return new RectF(cx - boxWidth / 2, cy - boxHeight / 2, cx + boxWidth / 2, cy + boxHeight / 2);
    }
}
