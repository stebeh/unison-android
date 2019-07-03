package org.simeon.unison;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class ControlActivity extends AppCompatActivity {

    SettingsFragment settingsFragment;

    UnisonService unisonService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_activity);
        settingsFragment = new SettingsFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, UnisonService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {

        OutputServiceBinder binder;

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = (OutputServiceBinder) service;
            unisonService = (UnisonService)binder.getService();
            binder.addListener(serviceListener);

            settingsFragment.updatePausePreference();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            unisonService = null;
            binder.removeListener(serviceListener);
        }
    };

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.control, rootKey);
        }

        void updatePausePreference() {
            boolean is_paused = getUnisonService().isPaused();
            this.getActivity().setTitle("Unison (" + (is_paused ? "paused)" : "running)"));
            getPreferenceManager().findPreference("pause").setTitle(is_paused ? "Resume" : "Pause");
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            switch (preference.getKey()) {
                case "status":
                    startActivity(new Intent(getActivity(), ServiceOutputActivity.class)
                            .putExtra("background", true));
                    break;
                case "log":
                    startActivity(new Intent(getActivity(), FileViewActivity.class)
                            .putExtra("file", getActivity().getFilesDir() + "/unison.log"));
                    break;
                case "pause":
                    if (!getUnisonService().isPaused()) {
                        if (getUnisonService().pause()) {
                            this.getActivity().setTitle("Unison (paused)");
                            preference.setTitle("Resume");
                        }
                    }
                    else {
                        if (getUnisonService().resume()) {
                            this.getActivity().setTitle("Unison (running)");
                            preference.setTitle("Pause");
                        }
                    }
                    break;
                case "restart":
                    getUnisonService().quit();
                    getUnisonService().stopSelf();
                    this.getActivity().startService(new Intent(this.getActivity(), UnisonService.class));
                    break;
                case "quit":
                    getUnisonService().quit();
                    getUnisonService().stopSelf();
                    this.getActivity().finish();
                    break;
                case "recent":
                    startActivity(new Intent(getActivity(), RecentFilesActivity.class)
                            .putExtra("root_dir", getUnisonService().getRootDir())
                            .putStringArrayListExtra("recent_changes", getUnisonService().getRecentChanges()));
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

        private UnisonService getUnisonService() {
            return ((ControlActivity) this.getActivity()).unisonService;
        }
    }

    private OutputServiceListener serviceListener = new OutputServiceListener() {
        @Override
        public void onOutputUpdate(String line) {

        }
    };
}