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

import android.os.Parcel;
import android.os.Parcelable;

import de.fau.cs.mad.carwatch.Constants;

/**
 * Information about a barcode field.
 */
public class BarcodeField implements Parcelable {

    public static final Creator<BarcodeField> CREATOR =
            new Creator<BarcodeField>() {
                @Override
                public BarcodeField createFromParcel(Parcel in) {
                    return new BarcodeField(in);
                }

                @Override
                public BarcodeField[] newArray(int size) {
                    return new BarcodeField[size];
                }
            };

    private final String label;
    private final String rawValue;
    private final String value;

    public BarcodeField(String label, String rawValue) {
        this.label = label;
        this.rawValue = rawValue;

        if (label.equals(Constants.BARCODE_TYPE_EAN8) && rawValue != null && rawValue.length() > 1) {
            // remove last digits since it's a check number
            this.value = rawValue.substring(0, rawValue.length() - 1);
        } else {
            this.value = rawValue;
        }
    }

    private BarcodeField(Parcel in) {
        label = in.readString();
        rawValue = in.readString();
        value = in.readString();
    }

    public String getValue() {
        return value;
    }

    public String getRawValue() {
        return rawValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(label);
        dest.writeString(rawValue);
        dest.writeString(value);
    }
}
