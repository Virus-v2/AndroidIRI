package com.northbridgeanalytics.mysensors;


import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
//import android.support.v7.preference.PreferenceFragmentCompat;
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class SettingsFragment extends PreferenceFragmentCompat {

    boolean useEsriJASON;

    @Override
    public void onCreatePreferencesFix(Bundle bundle, String s) {

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