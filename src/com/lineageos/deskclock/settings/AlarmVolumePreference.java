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

package com.lineageos.deskclock.settings;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;

import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.lineageos.deskclock.R;
import com.lineageos.deskclock.RingtonePreviewKlaxon;
import com.lineageos.deskclock.data.DataModel;

public class AlarmVolumePreference extends Preference {

    private static final long ALARM_PREVIEW_DURATION_MS = 2000;

    private SeekBar mSeekbar;
    private boolean mPreviewPlaying;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final Context context = getContext();
        final AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        // Minimum volume for alarm is not 0, calculate it.
        int maxVolume = audioManager.getStreamMaxVolume(STREAM_ALARM) -
                audioManager.getStreamMinVolume(STREAM_ALARM);
        mSeekbar = (SeekBar) holder.findViewById(R.id.seekbar);
        mSeekbar.setMax(maxVolume);
        mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) -
                audioManager.getStreamMinVolume(STREAM_ALARM));
        ((ImageView) holder.findViewById(android.R.id.icon))
                .setImageResource(R.drawable.ic_alarm_small);

        onSeekbarChanged();

        final ContentObserver volumeObserver = new ContentObserver(mSeekbar.getHandler()) {
            @Override
            public void onChange(boolean selfChange) {
                // Volume was changed elsewhere, update our slider.
                mSeekbar.setProgress(audioManager.getStreamVolume(STREAM_ALARM) -
                        audioManager.getStreamMinVolume(STREAM_ALARM));
            }
        };

        mSeekbar.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                context.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI,
                        true, volumeObserver);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                context.getContentResolver().unregisterContentObserver(volumeObserver);
            }
        });

        mSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int newVolume = progress + audioManager.getStreamMinVolume(STREAM_ALARM);
                    audioManager.setStreamVolume(STREAM_ALARM, newVolume, 0);
                }
                onSeekbarChanged();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!mPreviewPlaying) {
                    // If we are not currently playing, start.
                    RingtonePreviewKlaxon.start(
                            context, DataModel.getDataModel().getDefaultAlarmRingtoneUri());
                    mPreviewPlaying = true;
                    seekBar.postDelayed(() -> {
                        RingtonePreviewKlaxon.stop(context);
                        mPreviewPlaying = false;
                    }, ALARM_PREVIEW_DURATION_MS);
                }
            }
        });
    }

    private void onSeekbarChanged() {
        mSeekbar.setEnabled(doesDoNotDisturbAllowAlarmPlayback());
    }

    private boolean doesDoNotDisturbAllowAlarmPlayback() {
        final NotificationManager notificationManager = (NotificationManager)
                getContext().getSystemService(NOTIFICATION_SERVICE);
        return notificationManager.getCurrentInterruptionFilter() !=
                NotificationManager.INTERRUPTION_FILTER_NONE;
    }
}
