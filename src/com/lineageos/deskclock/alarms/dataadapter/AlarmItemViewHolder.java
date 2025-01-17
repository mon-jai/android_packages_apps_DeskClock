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

package com.lineageos.deskclock.alarms.dataadapter;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.lineageos.deskclock.AlarmUtils;
import com.lineageos.deskclock.AnimatorUtils;
import com.lineageos.deskclock.ItemAdapter;
import com.lineageos.deskclock.ItemAnimator;
import com.lineageos.deskclock.R;
import com.lineageos.deskclock.data.DataModel;
import com.lineageos.deskclock.data.Weekdays;
import com.lineageos.deskclock.provider.Alarm;
import com.lineageos.deskclock.provider.AlarmInstance;
import com.lineageos.deskclock.widget.EllipsizeLayout;
import com.lineageos.deskclock.widget.TextTime;

import java.util.Calendar;

/**
 * Abstract ViewHolder for alarm time items.
 */
public abstract class AlarmItemViewHolder extends ItemAdapter.ItemViewHolder<AlarmItemHolder>
        implements ItemAnimator.OnAnimateChangeListener {

    public static final float CLOCK_ENABLED_ALPHA = 1f;
    public static final float CLOCK_DISABLED_ALPHA = 0.69f;

    public static final float ANIM_STANDARD_DELAY_MULTIPLIER = 1f / 6f;
    public static final float ANIM_LONG_DURATION_MULTIPLIER = 2f / 3f;
    public static final float ANIM_SHORT_DURATION_MULTIPLIER = 1f / 4f;
    public static final float ANIM_SHORT_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_LONG_DURATION_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;
    public static final float ANIM_LONG_DELAY_INCREMENT_MULTIPLIER =
            1f - ANIM_STANDARD_DELAY_MULTIPLIER - ANIM_SHORT_DURATION_MULTIPLIER;

    public final TextTime clock;
    public final CompoundButton onOff;
    public final ImageView arrow;
    public final TextView preemptiveDismissButton;
    public final TextView daysOfWeek;
    public final EllipsizeLayout ellipsizeLayout;

    public float annotationsAlpha = CLOCK_ENABLED_ALPHA;

    public AlarmItemViewHolder(View itemView) {
        super(itemView);

        clock = itemView.findViewById(R.id.digital_clock);
        onOff = itemView.findViewById(R.id.onoff);
        arrow = itemView.findViewById(R.id.arrow);
        daysOfWeek = itemView.findViewById(R.id.days_of_week);
        preemptiveDismissButton = itemView.findViewById(R.id.preemptive_dismiss_button);
        ellipsizeLayout  = itemView.findViewById(R.id.ellipse_layout);
        preemptiveDismissButton.setOnClickListener(v -> {
            final AlarmInstance alarmInstance = getItemHolder().getAlarmInstance();
            if (alarmInstance != null) {
                getItemHolder().getAlarmTimeClickHandler().dismissAlarmInstance(alarmInstance);
            }
        });
        onOff.setOnCheckedChangeListener((compoundButton, checked) ->
                getItemHolder().getAlarmTimeClickHandler().setAlarmEnabled(
                        getItemHolder().item, checked));
    }

    @Override
    protected void onBindItemView(final AlarmItemHolder itemHolder) {
        final Alarm alarm = itemHolder.item;
        bindOnOffSwitch(alarm);
        bindClock(alarm);
        final Context context = itemView.getContext();
        itemView.setContentDescription(clock.getText() + " " + alarm.getLabelOrDefault(context));
    }

    protected void bindOnOffSwitch(Alarm alarm) {
        if (onOff.isChecked() != alarm.enabled) {
            onOff.setChecked(alarm.enabled);
        }
    }

    protected void bindClock(Alarm alarm) {
        clock.setTime(alarm.hour, alarm.minutes);
        clock.setAlpha(alarm.enabled ? CLOCK_ENABLED_ALPHA : CLOCK_DISABLED_ALPHA);
        Typeface oldTypeface = alarm.enabled ? clock.getTypeface() : null;
        clock.setTypeface(oldTypeface, alarm.enabled ? Typeface.BOLD : Typeface.NORMAL);
    }

    protected void bindPreemptiveDismissButton(Context context, Alarm alarm,
            AlarmInstance alarmInstance) {
        final boolean canBind = alarm.canPreemptivelyDismiss() && alarmInstance != null;
        if (canBind) {
            preemptiveDismissButton.setVisibility(View.VISIBLE);
            final String dismissText = alarm.instanceState == AlarmInstance.SNOOZE_STATE
                    ? context.getString(R.string.alarm_alert_snooze_until,
                            AlarmUtils.getAlarmText(context, alarmInstance, false))
                    : context.getString(R.string.alarm_alert_dismiss_text);
            preemptiveDismissButton.setText(dismissText);
            preemptiveDismissButton.setClickable(true);
        } else {
            preemptiveDismissButton.setVisibility(View.GONE);
            preemptiveDismissButton.setClickable(false);
        }
    }

    protected void bindRepeatText(Context context, Alarm alarm) {
        if (alarm.daysOfWeek.isRepeating()) {
            final Weekdays.Order weekdayOrder = DataModel.getDataModel().getWeekdayOrder();
            final String daysOfWeekText = alarm.daysOfWeek.toString(context, weekdayOrder);
            daysOfWeek.setText(daysOfWeekText);

            final String string = alarm.daysOfWeek.toAccessibilityString(context, weekdayOrder);
            daysOfWeek.setContentDescription(string);
        } else {
            final String labelText = Alarm.isTomorrow(alarm, Calendar.getInstance()) ?
                    context.getString(R.string.alarm_tomorrow) :
                    context.getString(R.string.alarm_today);
            daysOfWeek.setText(labelText);
        }
    }

    protected Animator getBoundsAnimator(View from, View to, long duration) {
        final Animator animator = AnimatorUtils
                .getBoundsAnimator(from, from, to)
                .setDuration(duration);
        animator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        return animator;
    }
}
