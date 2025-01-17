/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lineageos.deskclock;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.lineageos.deskclock.provider.Alarm;
import com.lineageos.deskclock.widget.selector.AlarmSelectionAdapter;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmSelectionActivity extends ListActivity {

    /** Used by default when an invalid action provided. */
    private static final int ACTION_INVALID = -1;

    /** Action used to signify alarm should be dismissed on selection. */
    public static final int ACTION_DISMISS = 0;

    public static final String EXTRA_ACTION = "com.lineageos.deskclock.EXTRA_ACTION";
    public static final String EXTRA_ALARMS = "com.lineageos.deskclock.EXTRA_ALARMS";

    private final List<Alarm> mSelections = new ArrayList<>();

    private int mAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // this activity is shown if:
        // a) no search mode was specified in which case we show all
        // enabled alarms
        // b) if search mode was next and there was multiple alarms firing next
        // (at the same time) then we only show those alarms firing at the same time
        // c) if search mode was time and there are multiple alarms with that time
        // then we only show those alarms with that time

        super.onCreate(savedInstanceState);
        setContentView(R.layout.selection_layout);

        final Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> finish());

        final Intent intent = getIntent();
        final Parcelable[] alarmsFromIntent = intent.getParcelableArrayExtra(EXTRA_ALARMS);
        mAction = intent.getIntExtra(EXTRA_ACTION, ACTION_INVALID);

        // reading alarms from intent
        // PickSelection is started only if there are more than 1 relevant alarm
        // so no need to check if alarmsFromIntent is empty
        for (Parcelable parcelable : alarmsFromIntent) {
            final Alarm alarm = (Alarm) parcelable;
            mSelections.add(alarm);
        }

        setListAdapter(new AlarmSelectionAdapter(this, R.layout.alarm_row, mSelections));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // id corresponds to mSelections id because the view adapter used mSelections
        final Alarm alarm = mSelections.get((int) id);
        if (alarm != null) {
            processAlarmActionAsync(alarm);
        }
        finish();
    }

    void processAlarmActionAsync(Alarm alarm) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            switch (mAction) {
                case ACTION_DISMISS:
                    HandleApiCalls.dismissAlarm(alarm, this);
                    break;
                case ACTION_INVALID:
                    LogUtils.i("Invalid action");
            }
        });
    }
}
