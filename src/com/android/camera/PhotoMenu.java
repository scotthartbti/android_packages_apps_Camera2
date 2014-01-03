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

import java.util.Locale;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.Camera.Parameters;
import android.view.LayoutInflater;

import com.android.camera.ui.AbstractSettingPopup;
import com.android.camera.ui.CountdownTimerPopup;
import com.android.camera.ui.ListPrefSettingPopup;
import com.android.camera.ui.MoreSettingPopup;
import com.android.camera.ui.PieItem;
import com.android.camera.ui.PieItem.OnClickListener;
import com.android.camera.ui.PieRenderer;
import com.android.camera2.R;

public class PhotoMenu extends PieController
        implements MoreSettingPopup.Listener,
        CountdownTimerPopup.Listener,
        ListPrefSettingPopup.Listener {
    private static String TAG = "PhotoMenu";

    private String[] mSettingsKeys;
    private MoreSettingPopup mPopup1;
    private static final int POPUP_NONE         = 0;
    private static final int POPUP_FIRST_LEVEL  = 1;
    private static final int POPUP_SECOND_LEVEL = 2;

    private final String mSettingOff;

    private PhotoUI mUI;
    private int mPopupStatus;
    private AbstractSettingPopup mPopup;
    private CameraActivity mActivity;

    public PhotoMenu(CameraActivity activity, PhotoUI ui, PieRenderer pie) {
        super(activity, pie);
        mUI = ui;
        mSettingOff = activity.getString(R.string.setting_off_value);
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
        // The order is from left to right in the menu.

        // HDR+ (GCam).
        if (group.findPreference(CameraSettings.KEY_CAMERA_HDR_PLUS) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_HDR_PLUS, true);
            mRenderer.addItem(item);
        }

        // HDR.
        if (group.findPreference(CameraSettings.KEY_CAMERA_HDR) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_HDR, true);
            mRenderer.addItem(item);
        }

        // Smart capture.
        if (group.findPreference(CameraSettings.KEY_SMART_CAPTURE_PHOTO) != null) {
            item = makeSwitchItem(CameraSettings.KEY_SMART_CAPTURE_PHOTO, true);
            item.setLabel(res.getString(R.string.pref_smart_capture_label).toUpperCase(locale));
            mRenderer.addItem(item);
        }

        // Exposure compensation.
        if (group.findPreference(CameraSettings.KEY_EXPOSURE) != null) {
            item = makeItem(CameraSettings.KEY_EXPOSURE);
            item.setLabel(res.getString(R.string.pref_exposure_label));
            mRenderer.addItem(item);
        }

        // More settings.
        PieItem more = makeItem(R.drawable.ic_more_options);
        more.setLabel(res.getString(R.string.camera_menu_more_label));
        mRenderer.addItem(more);

        // Flash.
        if (group.findPreference(CameraSettings.KEY_FLASH_MODE) != null) {
            item = makeItem(CameraSettings.KEY_FLASH_MODE);
            item.setLabel(res.getString(R.string.pref_camera_flashmode_label));
            mRenderer.addItem(item);
        }
        // Camera switcher.
        if (group.findPreference(CameraSettings.KEY_CAMERA_ID) != null) {
            item = makeSwitchItem(CameraSettings.KEY_CAMERA_ID, false);
            final PieItem fitem = item;
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    // Find the index of next camera.
                    ListPreference pref = mPreferenceGroup
                            .findPreference(CameraSettings.KEY_CAMERA_ID);
                    if (pref != null) {
                        int index = pref.findIndexOfValue(pref.getValue());
                        CharSequence[] values = pref.getEntryValues();
                        index = (index + 1) % values.length;
                        pref.setValueIndex(index);
                        mListener.onCameraPickerClicked(index);
                    }
                    updateItem(fitem, CameraSettings.KEY_CAMERA_ID);
                }
            });
            mRenderer.addItem(item);
        }
        // Countdown timer.
        final ListPreference ctpref = group.findPreference(CameraSettings.KEY_TIMER);
        final ListPreference beeppref = group.findPreference(CameraSettings.KEY_TIMER_SOUND_EFFECTS);
        item = makeItem(R.drawable.ic_timer);
        item.setLabel(res.getString(R.string.pref_camera_timer_title).toUpperCase(locale));
        item.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(PieItem item) {
                CountdownTimerPopup timerPopup =
                    (CountdownTimerPopup) mActivity.getLayoutInflater().inflate(
                    R.layout.countdown_setting_popup, null, false);
                timerPopup.initialize(ctpref, beeppref);
                timerPopup.setSettingChangedListener(PhotoMenu.this);
                mUI.dismissPopup();
                mPopup = timerPopup;
                mUI.showPopup(mPopup);
            }
        });
        more.addItem(item);
        // White balance.
        if (group.findPreference(CameraSettings.KEY_WHITE_BALANCE) != null) {
            item = makeItem(CameraSettings.KEY_WHITE_BALANCE);
            item.setLabel(res.getString(R.string.pref_camera_whitebalance_label));
            more.addItem(item);
        }
        // ISO mode
        if (group.findPreference(CameraSettings.KEY_ISO) != null) {
            item = makeItem(R.drawable.ic_iso);
            final ListPreference isoPref =
                group.findPreference(CameraSettings.KEY_ISO);
            item.setLabel(res.getString(R.string.pref_camera_iso_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    ListPrefSettingPopup popup =
                        (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.list_pref_setting_popup, null, false);
                    popup.initialize(isoPref);
                    popup.setSettingChangedListener(PhotoMenu.this);
                    mUI.dismissPopup();
                    mPopup = popup;
                    mUI.showPopup(mPopup);
                }
            });
            more.addItem(item);
        }
        // Color effects.
        if (group.findPreference(CameraSettings.KEY_CAMERA_COLOR_EFFECT) != null) {
            item = makeItem(R.drawable.ic_color_effect);
            final ListPreference effectPref =
                    group.findPreference(CameraSettings.KEY_CAMERA_COLOR_EFFECT);
            item.setLabel(res.getString(R.string.pref_coloreffect_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    ListPrefSettingPopup popup =
                        (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.list_pref_setting_popup, null, false);
                    popup.initialize(effectPref);
                    popup.setSettingChangedListener(PhotoMenu.this);
                    mUI.dismissPopup();
                    mPopup = popup;
                    mUI.showPopup(mPopup);
                }
            });
            more.addItem(item);
        }
        // Scene mode.
        if (group.findPreference(CameraSettings.KEY_SCENE_MODE) != null) {
            item = makeItem(R.drawable.ic_sce);
            final ListPreference scenePref = group.findPreference(CameraSettings.KEY_SCENE_MODE);
            item.setLabel(res.getString(R.string.pref_camera_scenemode_title).toUpperCase(locale));
            item.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(PieItem item) {
                    ListPrefSettingPopup popup =
                        (ListPrefSettingPopup) mActivity.getLayoutInflater().inflate(
                        R.layout.list_pref_setting_popup, null, false);
                    popup.initialize(scenePref);
                    popup.setSettingChangedListener(PhotoMenu.this);
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
                CameraSettings.KEY_TRUE_VIEW,
                CameraSettings.KEY_POWER_KEY_SHUTTER,
                CameraSettings.KEY_VOLUME_KEY_MODE,
                CameraSettings.KEY_PICTURE_SIZE,
                CameraSettings.KEY_PICTURE_FORMAT,
                CameraSettings.KEY_CAMERA_JPEG_QUALITY,
                CameraSettings.KEY_STORAGE,
                CameraSettings.KEY_HISTOGRAM,
                CameraSettings.KEY_SATURATION,
                CameraSettings.KEY_CONTRAST,
                CameraSettings.KEY_SHARPNESS,
                CameraSettings.KEY_AUTOEXPOSURE,
                CameraSettings.KEY_ANTIBANDING,
                CameraSettings.KEY_DENOISE,
                CameraSettings.KEY_FOCUS_MODE,
                CameraSettings.KEY_SELECTABLE_ZONE_AF,
                CameraSettings.KEY_FACE_DETECTION,
                CameraSettings.KEY_FACE_RECOGNITION,
                CameraSettings.KEY_AE_BRACKET_HDR
        };

    }

    @Override
    // Hit when an item in a popup gets selected
    public void onListPrefChanged(ListPreference pref) {
        if (mPopup != null && mPopup1 != null) {
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

        ListPreference pref = mPreferenceGroup.findPreference(
               CameraSettings.KEY_SCENE_MODE);
        String sceneMode = pref != null ? pref.getValue() : null;
        ListPreference prefFace =
            mPreferenceGroup.findPreference(CameraSettings.KEY_FACE_DETECTION);
        String faceDetection = prefFace != null ? prefFace.getValue() : null;
        if (sceneMode != null && !Parameters.SCENE_MODE_AUTO.equals(sceneMode)) {
            popup.setPreferenceEnabled(CameraSettings.KEY_FOCUS_MODE, false);
            popup.setPreferenceEnabled(CameraSettings.KEY_AUTOEXPOSURE, false);
        }
        if (faceDetection != null && !Parameters.FACE_DETECTION_ON.equals(faceDetection)) {
            popup.setPreferenceEnabled(CameraSettings.KEY_FACE_RECOGNITION, false);
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

    @Override
    public void onSettingChanged(ListPreference pref) {
        // Reset the scene mode if HDR is set to on. Reset HDR if scene mode is
        // set to non-auto.
        if (notSame(pref, CameraSettings.KEY_CAMERA_HDR, mSettingOff)) {
            setPreference(CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO);
        } else if (notSame(pref, CameraSettings.KEY_SCENE_MODE, Parameters.SCENE_MODE_AUTO)) {
            setPreference(CameraSettings.KEY_CAMERA_HDR, mSettingOff);
        }
        super.onSettingChanged(pref);
    }
}
