package com.example.alex.spp;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.os.Bundle;

public class PrefActivity extends PreferenceActivity {

    private Preference lengthPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        lengthPref = getPreferenceManager().findPreference("length");
        lengthPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                lengthPref.setSummary(newValue.toString());
                return true;
            }
        });
    }
}
