package org.simeon.unison;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SingleRunService extends IntentService implements OutputService {

    private java.lang.Process proc;

    private static final int NOTIFY_ID = 1;

    public SingleRunService() {
        super("SingleRunService");
    }

    private final OutputServiceBinder binder = new OutputServiceBinder(this);
    private StringBuffer errBuf = new StringBuffer();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public String getOutput() {
        return errBuf.toString();
    }

    public void quit() {
        proc.destroy();
        stopForeground(true);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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

        startProcess();

        // temporary fix for service exiting too early
        // (there should definitely be better communication between service and activity)
        try {
            Thread.sleep(500);
        }
        catch (Exception e) {
            return;
        }

        stopForeground(true);
    }

    private void startProcess() {
        errBuf = new StringBuffer();
        try {
            proc = Runtime.getRuntime().exec(
                    new String[]{"./unison", "-batch", "-sshargs", "-y -i .ssh/dbr_key",
                                 "-perms", "0", "-dontchmod"},
                    new String[]{"HOME=" + getFilesDir(), "PATH=" + getFilesDir(), "LD_LIBRARY_PATH=" + getFilesDir() + "/lib"},
                    getFilesDir());
            BufferedReader errRead = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String line;
            while ((line = errRead.readLine()) != null) {
                errBuf.append(line + '\n');
                binder.broadcastOutput(errBuf.toString());
                if (line.contains("Failed loading keyfile")) {
                    break;
                }
            }
            errRead.close();

            binder.broadcastFinish();
        }
        catch (IOException e) {
            Log.e("Unison", e.getMessage());
        }
    }
}
