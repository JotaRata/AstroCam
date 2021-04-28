package com.jotarata.android.astrocam;

import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.util.SizeF;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;

import com.jotarata.astrocam.R;

public class SettingsActivity extends AppCompatActivity {

    private static final  float log2 = (float) Math.log(2);
    public static CameraCharacteristics mCharacteristics;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);

        }

        mCharacteristics = CameraActivity.mCamera2RawInstance.mCharacteristics;
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        private SharedPreferences  mPrefs;

        private ListPreference isoList;
        private SeekBarPreference expSlider;
        private Preference exp_info;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            mPrefs = getPreferenceManager().getSharedPreferences();

            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            PreferenceScreen mprefs = getPreferenceScreen();
            isoList = (ListPreference)mprefs.findPreference("iso");
            expSlider = (SeekBarPreference)mprefs.findPreference("exposure");
            exp_info =   (Preference) mprefs.findPreference("exp_info");

            SetIsoValues();
            SetExpoValues();
        }

        private void SetIsoValues() {
            Range<Integer> ISORanges = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            int m100 = ISORanges.getLower()/100;
            int M100 = ISORanges.getUpper()/100;

            int logm = (int) Math.ceil(Math.log(m100)/log2);
            int logM = (int) Math.ceil(Math.log(M100)/log2);

            Log.d("ISO Values Min", String.valueOf(m100));
            Log.d("ISO Values Max", String.valueOf(M100));
            String[] isoValues = new String[logM - logm + 1];
            for (int i = 0; i <= logM - logm; i++)
            {
                isoValues[i] = String.valueOf((int)Math.pow(2, i + logm) * 100);
            }
            isoList.setEntryValues(isoValues);

            String[] isoLabels = isoValues.clone();
            isoLabels[0] += " (Mas oscuro)";
            isoLabels[logM - logm] += " (Mas ruido)";
            isoList.setEntries(isoLabels);

        }

        private  void SetExpoValues(){
            float[] focallengths = mCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);

            int maxExposure = 30;

            float eFocalLEngth = 18;    // 18mm by default
            if (focallengths.length > 0)
            {
                SizeF sensorSize = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                SharedPreferences.Editor mEditor = mPrefs.edit();
                mEditor.putFloat("sensor_width", sensorSize.getWidth());
                mEditor.putFloat("sensor_height", sensorSize.getHeight());
                mEditor.commit();

                float cf = (float) Math.max(sensorSize.getHeight(), sensorSize.getWidth());
                eFocalLEngth = focallengths[0] * cf;
                maxExposure = (int)(500 / eFocalLEngth); // Using the 500 rule to determine the maximum exposure time +1 comes from frame saving times
            }
            Log.d("Max Exposure", String.valueOf(maxExposure));
            Log.d("Focal length", String.valueOf(focallengths[0]));
            Log.d("Effective Focal Length", String.valueOf(eFocalLEngth));


            expSlider.setMax(Math.max(maxExposure, 4));
            expSlider.setMin(1);

            UpdateExpInfo(expSlider.getValue(), maxExposure);

            final int finalMaxExposure = maxExposure;
            expSlider.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    UpdateExpInfo((Integer) newValue, finalMaxExposure);
                    return true;
                }
            });
        }
        private void UpdateExpInfo(int userExp, int recommendedTime)
        {
            Range<Long> exprange = mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);

            float  maxExp = (float) (exprange.getUpper() / 1_000_000_000.0);

            float maxExpPerFrame = Math.min(userExp, maxExp);
            int numberOfFrames = (int) Math.max(userExp / (maxExpPerFrame), 1);

            SharedPreferences.Editor mEditor = mPrefs.edit();
            mEditor.putInt("numberF", numberOfFrames);
            mEditor.putFloat("frame_exp", maxExpPerFrame * 1_000_000_000);
            mEditor.commit();

            int estimadedWait = (int)(numberOfFrames * (maxExpPerFrame + 1.8f));

            String text = String.format("Se tomaran %o imagenes con un tiempo de exposicion de %.2f segundos cada una.\n\n", numberOfFrames, maxExpPerFrame);
            text += String.format("La sesion durara aproximadamente %o segundos\n\n", estimadedWait);
            if (estimadedWait > recommendedTime)
            {
                text += "ATENCION: Es posible que el movimiento de la Tierra se vuelva aparente";
            }
            exp_info.setTitle(text);

        }
    }
}