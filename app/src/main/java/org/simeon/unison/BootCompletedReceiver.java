package org.simeon.unison;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceManager;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("start_boot", false)) {
            context.startService(new Intent(context, UnisonService.class));
        }
    }
}
