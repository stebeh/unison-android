package org.simeon.unison;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SingleRunService extends Service {

    private java.lang.Process proc;

    private static final int NOTIFY_ID = 1;

    private final OutputServiceBinder binder = new OutputServiceBinder(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Intent notifyIntent = new Intent(this, ServiceOutputActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder notifyBuilder = new Notification.Builder(this);
        notifyBuilder.setContentTitle("Unison")
                .setSmallIcon(R.drawable.wait)
                .setContentText("Unison is running")
                .setContentIntent(notifyPendingIntent);

        startForeground(NOTIFY_ID, notifyBuilder.build());

        new Thread() {
            public void run() {
                startProcess();
                stopForeground(true);
                stopSelf();
            }
        }.start();

        return binder;
    }

    private void startProcess() {
        try {
            proc = Runtime.getRuntime().exec(
                    new String[]{"./unison", "-batch", "-sshargs", "-y -i .ssh/dbr_key",
                                 "-perms", "0", "-dontchmod"},
                    new String[]{"HOME=" + getFilesDir(), "PATH=" + getFilesDir(), "LD_LIBRARY_PATH=" + getFilesDir() + "/lib"},
                    getFilesDir());
            BufferedReader errRead = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String line;
            while ((line = errRead.readLine()) != null) {
                binder.broadcastUpdate(line + '\n');
                if (line.contains("Failed loading keyfile")) {
                    break;
                }
            }
            errRead.close();
        }
        catch (IOException e) {
            Log.e("Unison", e.getMessage());
        }
    }
}
