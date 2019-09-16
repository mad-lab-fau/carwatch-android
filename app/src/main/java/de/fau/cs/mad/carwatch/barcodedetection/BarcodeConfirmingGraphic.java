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

import android.graphics.Canvas;
import android.graphics.Path;

import de.fau.cs.mad.carwatch.barcodedetection.camera.GraphicOverlay;

/**
 * Guides user to move camera closer to confirm the detected barcode.
 */
class BarcodeConfirmingGraphic extends BarcodeGraphicBase {

    BarcodeConfirmingGraphic(GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Draws a highlighted path to indicate the current progress to meet size requirement.
        Path path = new Path();
        // To have a completed path with all corners rounded.
        path.moveTo(boxRect.left, boxRect.top);
        path.lineTo(boxRect.right, boxRect.top);
        path.lineTo(boxRect.right, boxRect.bottom);
        path.lineTo(boxRect.left, boxRect.bottom);
        path.close();

        canvas.drawPath(path, pathPaint);
    }


}
