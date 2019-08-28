package org.simeon.unison;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private boolean isServiceRunning(boolean background) {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info : am.getRunningServices(Integer.MAX_VALUE)) {
            if (info.service.getClassName().equals(
                    background ? UnisonService.class.getName() : SingleRunService.class.getName()))
                return true;
        }
        return false;
    }

    private void copyBinary(String binaryFile) {
        String outPath = getFilesDir() + "/" + binaryFile;

        if (binaryFile.equals("unison_fsmonitor")) {
            // hyphens are not allowed in resource files
            outPath = getFilesDir() + "/unison-fsmonitor";
        }

        if (!new File(outPath).exists()) {
            InputStream inputStream = getResources().openRawResource(
                    getResources().getIdentifier(binaryFile, "raw", getPackageName()));
            try {
                FileOutputStream outputStream = new FileOutputStream(outPath);
                byte[] buf = new byte[inputStream.available()];
                inputStream.read(buf);
                outputStream.write(buf);
                inputStream.close();
                outputStream.close();

                Runtime.getRuntime().exec(new String[]{"chmod", "500", outPath});
            }
            catch (IOException e) {
                Log.e("Unison", e.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        if (isServiceRunning(false)) {
            startActivity(new Intent(this, ServiceOutputActivity.class));
            this.finish();
            return;
        }*/
        if (isServiceRunning(true)) {
            startActivity(new Intent(this, ControlActivity.class));
            this.finish();
            return;
        }

        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        for (String binaryFile : new String[]{"unison", "unison_fsmonitor", "ssh", "dropbearconvert"}) {
            copyBinary(binaryFile);
        }

        for (String dir : new String[]{"/.unison", "/.ssh"}) {
            File f = new File(getFilesDir(), dir);
            if (!f.exists()) {
                f.mkdir();
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            EditTextPreference configPref = getPreferenceManager().findPreference("config_file");
            EditTextPreference keyPref = getPreferenceManager().findPreference("key_file");
            if (configPref.getText() == null) {
                configPref.setText(Environment.getExternalStorageDirectory() + "/.unison/default.prf");
            }
            if (keyPref.getText() == null) {
                keyPref.setText(Environment.getExternalStorageDirectory() + "/.unison/id_rsa");
            }

            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), "config_file");
            onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), "key_file");
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            switch (preference.getKey()) {
                case "config_file":
                    break;
                case "key_file":
                    break;
                case "run_once":
                    startActivity(new Intent(getActivity(), ServiceOutputActivity.class).
                            putExtra("background", false));
                    getActivity().finish();
                    break;
                case "run_background":
                    getActivity().startService(new Intent(getActivity(), UnisonService.class));
                    getActivity().finish();
                    break;
                case "log":
                    startActivity(new Intent(getActivity(), FileViewActivity.class).
                            putExtra("file", getActivity().getFilesDir() + "/unison.log"));
                    break;
                case "feedback":
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.email))),
                            "E-mail developer"));
                    break;
                case "about":
                    startActivity(Intent.createChooser(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website))),
                            "Open GitHub page"));
                    break;
            }

            return true;
        }

        void setRunEnabled(boolean enabled) {
            getPreferenceManager().findPreference("run_once").setEnabled(enabled);
            getPreferenceManager().findPreference("run_background").setEnabled(enabled);
            getPreferenceManager().findPreference("start_boot").setEnabled(enabled);
        }

        void deletePath(String filePath) {
            new File((filePath)).delete();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            switch (s) {
                case "config_file":
                    EditTextPreference configPref = getPreferenceManager().findPreference("config_file");
                    String configPath = configPref.getText();
                    String configDestPath = getActivity().getFilesDir() + "/.unison/default.prf";

                    if (new File(configPath).exists()) {
                        try {
                            Runtime.getRuntime().exec(
                                    new String[]{"cp", "-f", configPath, configDestPath});
                            setRunEnabled(true);
                        }
                        catch (IOException e) {
                            setRunEnabled(false);
                            deletePath(configDestPath);
                            Log.e("Unison", e.getMessage());
                        }
                    }
                    else {
                        setRunEnabled(false);
                        deletePath(configDestPath);
                    }
                    break;
                case "key_file":
                    EditTextPreference keyPref = getPreferenceManager().findPreference("key_file");
                    String keyPath = keyPref.getText();
                    String keyDestPath = getActivity().getFilesDir() + "/.ssh/dbr_key";

                    if (new File(keyPath).exists()) {
                        try {
                            new File(keyDestPath).delete();
                            Process proc = Runtime.getRuntime().exec(
                                    new String[]{"./dropbearconvert", "openssh", "dropbear", keyPath, keyDestPath},
                                    null, getActivity().getFilesDir());
                            proc.waitFor();
                            if (proc.exitValue() != 0) {
                                //setRunEnabled(false);
                                deletePath(keyDestPath);
                            }
                        }
                        catch (IOException e) {
                            setRunEnabled(false);
                            Log.e("Unison", e.getMessage());
                        }
                        catch (InterruptedException e) {
                            setRunEnabled(false);
                            Log.e("Unison", e.getMessage());
                        }
                    }
                    else {
                        //setRunEnabled(false);
                        deletePath(keyDestPath);
                    }
                    break;
            }
        }
    }
}