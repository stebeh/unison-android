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

    boolean running;

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
    protected void onResume() {
        super.onResume();

        running = true;
    }

    @Override
    protected void onStop() {
        super.onStop();

        running = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {

        OutputServiceBinder binder;

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = (OutputServiceBinder) service;
            unisonService = (UnisonService)binder.getService();
            binder.addListener(serviceListener);

            settingsFragment.updatePreferences();
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

        void updatePreferences() {
            int procStatus = getUnisonService().getProcStatus();

            Preference pausePreference = getPreferenceManager().findPreference("pause");

            switch (procStatus) {
                case UnisonService.PROC_INACTIVE:
                    getActivity().setTitle("Unison (stopped)");
                    pausePreference.setTitle("Pause");
                    pausePreference.setEnabled(false);
                    break;
                case UnisonService.PROC_RUNNING:
                    getActivity().setTitle("Unison (running)");
                    pausePreference.setTitle("Pause");
                    pausePreference.setEnabled(true);
                    break;
                case UnisonService.PROC_PAUSED:
                    getActivity().setTitle("Unison (paused)");
                    pausePreference.setTitle("Resume");
                    pausePreference.setEnabled(true);
                    break;
                case UnisonService.PROC_FINISHED:
                    getActivity().setTitle("Unison (error)");
                    pausePreference.setTitle("Pause");
                    pausePreference.setEnabled(false);

            }
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
                    if (getUnisonService().getProcStatus() == UnisonService.PROC_RUNNING) {
                        getUnisonService().pause();
                    }
                    else if (getUnisonService().getProcStatus() == UnisonService.PROC_PAUSED) {
                        getUnisonService().resume();
                    }
                    break;
                case "restart":
                    getUnisonService().quit();
                    getUnisonService().stopForeground(true);
                    getUnisonService().stopSelf();
                    this.getActivity().startService(new Intent(this.getActivity(), UnisonService.class));
                    break;
                case "quit":
                    getUnisonService().quit();
                    getUnisonService().stopForeground(true);
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
        public void onOutputUpdate() {

        }

        public void onStatusChange() {
            if (running) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        settingsFragment.updatePreferences();
                    }
                });
            }
        }
    };
}