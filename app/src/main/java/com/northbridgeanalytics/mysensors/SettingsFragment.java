package com.northbridgeanalytics.mysensors;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.View;


public class SettingsFragment extends PreferenceFragmentCompat {

    boolean useEsriJASON;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {

        addPreferencesFromResource(R.xml.preferences);

        // TODO I think this can be used to fire validation of REST service.
//        SwitchPreferenceCompat esriJASON = (SwitchPreferenceCompat) findPreference("preference_filename_json");
//        esriJASON.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
//            @Override
//            public boolean onPreferenceChange(Preference preference, Object o) {
//
//                useEsriJASON = (boolean) o;
//
//                Log.i("Preference", "Esri is " + useEsriJASON);
//
//                return true;
//            }
//        });

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getListView().setBackgroundColor(Color.WHITE);
    }



}