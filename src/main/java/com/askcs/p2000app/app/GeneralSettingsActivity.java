package com.askcs.p2000app.app;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.askcs.p2000app.R;

/**
 * Created by Jordi on 13-3-14.
 */
public class GeneralSettingsActivity extends PreferenceActivity {
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.general_preferences);
  }
}