/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.camera;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;

import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.ListPrefSettingPopup;
import com.android.camera.ui.MoreSettingPopup;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;
import com.android.camera.ui.TimeIntervalPopup;
import com.android.camera2.R;

import java.util.Locale;

public class VideoMenu extends PieController
        implements MoreSettingPopup.Listener,
        ListPrefSettingPopup.Listener,
        TimeIntervalPopup.Listener {

    private static String TAG = "CAM_VideoMenu";

    private String[] mSettingsKeys;
    private MoreSettingPopup mPopup1;
    private static final int POPUP_NONE         = 0;
    private static final int POPUP_FIRST_LEVEL  = 1;
    private static final int POPUP_SECOND_LEVEL = 2;

    private VideoUI mUI;
    private int mPopupStatus;
    private AbstractSettingPopup mPopup;
    private CameraActivity mActivity;

    public VideoMenu(CameraActivity activity, VideoUI ui, PieRenderer pie) {
        super(activity, pie);
        mUI = ui;
        mActivity = activity;
    }


    public void initialize(PreferenceGroup group) {
        super.initialize(group);
        mPopup = null;
        mPopup1 = null;
        mPopupStatus = POPUP_NONE;
        PieItem item = null;
        final Resources res = mActivity.getResources();
        Locale locale = res.getConfiguration().locale;
        // smart capture
        if (group.findPreference(CameraSettings.KEY_SMART_CAPTURE_VIDEO) != null) {
            item = makeSwitchItem(CameraSettings.KEY_SMART_CAPTURE_VIDEO, true);
            item.setLabel(res.getString(R.string.pref_smart_capture_label).toUpperCase(locale));
            mRenderer.addItem(item);
        }
        // more options
        PieItem more = makeItem(R.drawable.ic_more_options);
        more.setLabel(res.getString(R.string.camera_menu_more_label));
        mRenderer.addItem(more);
        // camera switcher
        if (group.findPreference(CameraSettings.KEY_CAMERA_ID) != null) {
            item = makeItem(R.drawable.ic_switch_back);
            IconListPreference lpref = (IconListPreference) group.findPreference(
                    CameraSettings.KEY_CAMERA_ID);
            item.setLabel(lpref.getLabel());
            item.setImageResource(mActivity,
                    ((IconListPreference) lpref).getIconIds()
                    [lpref.findIndexOfValue(lpref.getValue())]);

            final PieItem fitem = item;
            item.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(PieItem item) {
                    // Find the index of next camera.
                    ListPreference pref =
                            mPreferenceGroup.findPreference(CameraSettings.KEY_CAMERA_ID);
                    if (pref != null) {
                        int index = pref.findIndexOfValue(pref.getValue());
                        CharSequence[] values = pref.getEntryValues();
                        index = (index + 1) % values.length;
                        int newCameraId = Integer.parseInt((String) values[index]);
                        fitem.setImageResource(mActivity,
                                ((IconListPreference) pref).getIconIds()[index]);
                        fitem.setLabel(pref.getLabel());
                        mListener.onCameraPickerClicked(newCameraId);
                    }
                }
            });
            mRenderer.addItem(item);
        }
        // flash
        if (group.findPreference(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE) != null) {
            item = makeItem(CameraSettings.KEY_VIDEOCAMERA_FLASH_MODE);
            mRenderer.addItem(item);
        }
        // time laps frame interval
        if (group.findPreference(CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL) != null) {
            item = makeItem(R.drawable.ic_timelapse_none);
            final IconListPreference timeLapsPref = (IconListPreference)
                group.findPreference(CameraSettings.KEY_VIDEO_TIME_LAPSE_FRAME_INTERVAL);
            item.setLabel(res.getString(
                R.string.pref_video_time_lapse_frame_interval_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    LayoutInflater inflater =  mActivity.getLayoutInflater();
                    TimeIntervalPopup popup = (TimeIntervalPopup) inflater.inflate(
                    R.layout.time_interval_popup, null, false);
                    popup.initialize(timeLapsPref);
                    popup.setSettingChangedListener(VideoMenu.this);
                    mUI.dismissPopup();
                    mPopup = popup;
                    mUI.showPopup(mPopup);
                }
            });
            more.addItem(item);
        }
        // white balance
        if (group.findPreference(CameraSettings.KEY_WHITE_BALANCE) != null) {
            item = makeItem(CameraSettings.KEY_WHITE_BALANCE);
            more.addItem(item);
        }
        // color effects
        if (group.findPreference(CameraSettings.KEY_VIDEO_COLOR_EFFECT) != null) {
            item = makeItem(R.drawable.ic_color_effect);
            final ListPreference effectPref =
                group.findPreference(CameraSettings.KEY_VIDEO_COLOR_EFFECT);
            item.setLabel(res.getString(
                R.string.pref_coloreffect_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    ListPrefSettingPopup popup = (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                            R.layout.list_pref_setting_popup, null, false);
                    popup.initialize(effectPref);
                    popup.setSettingChangedListener(VideoMenu.this);
                    mUI.dismissPopup();
                    mPopup = popup;
                    mUI.showPopup(mPopup);
                }
            });
            more.addItem(item);
        }
        // video effects
        if (group.findPreference(CameraSettings.KEY_VIDEO_EFFECT) != null) {
            item = makeItem(R.drawable.ic_effects_holo_light);
            final ListPreference effectPref = group.findPreference(CameraSettings.KEY_VIDEO_EFFECT);
            item.setLabel(res.getString(R.string.pref_video_effect_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    ListPrefSettingPopup popup =
                        (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.list_pref_setting_popup, null, false);
                    popup.initialize(effectPref);
                    popup.setSettingChangedListener(VideoMenu.this);
                    mUI.dismissPopup();
                    mPopup = popup;
                    mUI.showPopup(mPopup);
                }
            });
            more.addItem(item);
        }
        // Settings
        PieItem settings = makeItem(R.drawable.ic_settings_holo_light);
        settings.setLabel(mActivity.getResources().getString(R.string.camera_menu_settings_label));
        settings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                if (mPopup1 == null || mPopupStatus != POPUP_FIRST_LEVEL){
                    initializePopup();
                    mPopupStatus = POPUP_FIRST_LEVEL;
                }
                mUI.showPopup(mPopup1);
            }
        });
        more.addItem(settings);

        mSettingsKeys = new String[] {
                CameraSettings.KEY_RECORD_LOCATION,
                CameraSettings.KEY_POWER_KEY_SHUTTER,
                CameraSettings.KEY_VOLUME_KEY_MODE,
                CameraSettings.KEY_VIDEO_QUALITY,
                CameraSettings.KEY_VIDEO_JPEG_QUALITY,
                CameraSettings.KEY_DIS,
                CameraSettings.KEY_VIDEO_ENCODER,
                CameraSettings.KEY_AUDIO_ENCODER,
                CameraSettings.KEY_VIDEO_DURATION,
                CameraSettings.KEY_VIDEO_HIGH_FRAME_RATE,
                CameraSettings.KEY_VIDEO_HDR
        };

    }

    @Override
    // Hit when an item in a popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        if (mPopup != null) {
            mUI.dismissPopup();
        }
        onSettingChanged(pref);
    }

    public void popupDismissed() {
        if (mPopupStatus == POPUP_SECOND_LEVEL) {
            initializePopup();
            mPopupStatus = POPUP_FIRST_LEVEL;
            mUI.showPopup(mPopup1);
            if(mPopup1 != null) {
                mPopup1 = null;
            }
        } else {
            initializePopup();
            if (mPopup != null) {
                mPopup = null;
            }
        }
    }

    @Override
    public void overrideSettings(final String ... keyvalues) {
        super.overrideSettings(keyvalues);
        if (mPopup1 == null) {
            initializePopup();
        }
        mPopup1.overrideSettings(keyvalues);
    }

    @Override
    // Hit when an item in the first-level popup gets selected, then bring up
    // the second-level popup
    public void onPreferenceClicked(ListPreference pref) {
        if (mPopupStatus != POPUP_FIRST_LEVEL) {
            return;
        }

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        ListPrefSettingPopup basic = (ListPrefSettingPopup) inflater.inflate(
                R.layout.list_pref_setting_popup, null, false);
        basic.initialize(pref);
        basic.setSettingChangedListener(this);
        mUI.dismissPopup();
        mPopup = basic;
        mUI.showPopup(mPopup);
        mPopupStatus = POPUP_SECOND_LEVEL;
    }

    protected void initializePopup() {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        MoreSettingPopup popup = (MoreSettingPopup) inflater.inflate(
                R.layout.more_setting_popup, null, false);
        popup.setSettingChangedListener(this);
        popup.initialize(mPreferenceGroup, mSettingsKeys);
        if (mActivity.isSecureCamera()) {
            // Prevent location preference from getting changed in secure camera mode
            popup.setPreferenceEnabled(CameraSettings.KEY_RECORD_LOCATION, false);
        }
        mPopup1 = popup;
    }

    // Return true if the preference has the specified key but not the value.
    private static boolean notSame(ListPreference pref, String key, String value) {
        return (key.equals(pref.getKey()) && !value.equals(pref.getValue()));
    }

    private void setPreference(String key, String value) {
        ListPreference pref = mPreferenceGroup.findPreference(key);
        if (pref != null && !value.equals(pref.getValue())) {
            pref.setValue(value);
            reloadPreferences();
        }
    }
}
