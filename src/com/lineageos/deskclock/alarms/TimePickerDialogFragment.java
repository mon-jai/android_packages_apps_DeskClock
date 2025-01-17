/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lineageos.deskclock.alarms;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;

/**
 * DialogFragment used to show TimePicker.
 */
public class TimePickerDialogFragment extends DialogFragment {

    /**
     * Tag for timer picker fragment in FragmentManager.
     */
    private static final String TAG = "TimePickerDialogFragment";

    private static final String ARG_HOUR = TAG + "_hour";
    private static final String ARG_MINUTE = TAG + "_minute";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final OnTimeSetListener listener = ((OnTimeSetListener) getParentFragment());

        final Calendar now = Calendar.getInstance();
        final Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        final int hour = args.getInt(ARG_HOUR, now.get(Calendar.HOUR_OF_DAY));
        final int minute = args.getInt(ARG_MINUTE, now.get(Calendar.MINUTE));

        final Context context = getActivity();
        return new TimePickerDialog(context, (view, hourOfDay, minute1) ->
                listener.onTimeSet(hourOfDay, minute1),
                hour, minute, DateFormat.is24HourFormat(context));
    }

    public static void show(Fragment fragment) {
        show(fragment, -1 /* hour */, -1 /* minute */);
    }

    public static void show(Fragment parentFragment, int hourOfDay, int minute) {
        if (!(parentFragment instanceof OnTimeSetListener)) {
            throw new IllegalArgumentException("Fragment must implement OnTimeSetListener");
        }

        final FragmentManager manager = parentFragment.getChildFragmentManager();
        if (manager.isDestroyed()) {
            return;
        }

        // Make sure the dialog isn't already added.
        removeTimeEditDialog(manager);

        final TimePickerDialogFragment fragment = new TimePickerDialogFragment();

        final Bundle args = new Bundle();
        if (hourOfDay >= 0 && hourOfDay < 24) {
            args.putInt(ARG_HOUR, hourOfDay);
        }
        if (minute >= 0 && minute < 60) {
            args.putInt(ARG_MINUTE, minute);
        }

        fragment.setArguments(args);
        fragment.show(manager, TAG);
    }

    public static void removeTimeEditDialog(FragmentManager manager) {
        if (manager != null) {
            final Fragment prev = manager.findFragmentByTag(TAG);
            if (prev != null) {
                manager.beginTransaction().remove(prev).commit();
            }
        }
    }

    /**
     * The callback interface used to indicate the user is done filling in the time (e.g. they
     * clicked on the 'OK' button).
     */
    public interface OnTimeSetListener {
        /**
         * Called when the user is done setting a new time and the dialog has closed.
         * @param hourOfDay the hour that was set
         * @param minute    the minute that was set
         */
        void onTimeSet(int hourOfDay, int minute);
    }
}
