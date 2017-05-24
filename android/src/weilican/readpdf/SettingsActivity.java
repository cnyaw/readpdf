/*
  SettingsActivity.java
  Read PDF.

  Copyright (c) 2016 Waync Cheng.
  All Rights Reserved.

  2016/5/29 Waync created
 */

package weilican.readpdf;
 
import android.os.Bundle;
import android.preference.PreferenceActivity;
 
public class SettingsActivity extends PreferenceActivity
{
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.settings);
  }
}
