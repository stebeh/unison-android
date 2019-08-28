package org.simeon.unison;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime;
import java.lang.Thread;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnisonService extends Service {

    public static final int PROC_INACTIVE = 0;
    public static final int PROC_RUNNING = 1;
    public static final int PROC_PAUSED = 2;
    public static final int PROC_FINISHED = 3;

    private java.lang.Process proc;
    private int procStatus = PROC_INACTIVE;

    private static final int RECONNECT_DELAY = 10000;

    String rootDir;
    private LinkedList<String> recentChanges = new LinkedList<>();
    Pattern rootPattern = Pattern.compile("Connected \\[\\/\\/.*\\/\\/(.*) ->.*");
    Pattern filePattern = Pattern.compile("\\[END\\] (\\w+)ing (file )?(.*)");

    NotificationManager notifyManager;
    Notification.Builder notifyBuilder;
    private static final int NOTIFY_ID = 1;

    Timer reconnectTimer = new Timer();

    private static final int STATUS_STARTED = 0;
    private static final int STATUS_LOOKING = 1;
    private static final int STATUS_WORKING = 2;
    private static final int STATUS_SYNCHRONIZED = 3;
    private static final int STATUS_ERROR = 4;
    private static final int STATUS_KEY_ERROR = 5;
    private static final int STATUS_LOST_CONNECTION = 6;

    HashMap<String, Integer> statusMsg = new HashMap<String, Integer>() {{
        put("Connected", STATUS_STARTED);
        put("Looking for changes", STATUS_LOOKING);
        put("started propagating changes", STATUS_WORKING);
        put("Nothing to do", STATUS_SYNCHRONIZED);
        put("Synchronization complete", STATUS_SYNCHRONIZED);
        put("[END]", STATUS_SYNCHRONIZED);
        put("Fatal error:", STATUS_ERROR);
        put("Failed loading keyfile", STATUS_KEY_ERROR);
        put("Lost connection with the server", STATUS_LOST_CONNECTION);
    }};

    private final OutputServiceBinder binder = new OutputServiceBinder(this);

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private int getProcessPid(Process proc) {
        // A ridiculously hacky way to get the PID of process
        Field field;
        try {
            field = proc.getClass().getDeclaredField("pid");
        }
        catch (NoSuchFieldException e) {
            return 0;
        }
        try {
            field.setAccessible(true);
            int pid = (int) field.get(proc);
            field.setAccessible(false);
            return pid;
        }
        catch (IllegalAccessException e) {
            return 0;
        }
    }

    public int getProcStatus() {
        return procStatus;
    }


    public void quit() {
        proc.destroy();
        reconnectTimer.cancel();
        procStatus = PROC_INACTIVE;
    }

    public void pause() {
        if (procStatus != PROC_RUNNING) return;
        try {
            int pid = getProcessPid(proc);
            if (pid != 0) {
                Runtime.getRuntime().exec(new String[]{"kill", "-STOP", Integer.toString(pid)});
                notifyBuilder.setSmallIcon(R.drawable.wait);
                notifyManager.notify(NOTIFY_ID, notifyBuilder.build());
                procStatus = PROC_PAUSED;
                binder.broadcastStatus();
            }
        }
        catch (IOException e) {
        }
        binder.broadcastStatus();
    }

    public void resume() {
        if (procStatus != PROC_PAUSED) return;
        try {
            int pid = getProcessPid(proc);
            if (pid != 0) {
                Runtime.getRuntime().exec(new String[]{"kill", "-CONT", Integer.toString(pid)});
                notifyBuilder.setSmallIcon(R.drawable.good);
                notifyManager.notify(NOTIFY_ID, notifyBuilder.build());
                procStatus = PROC_RUNNING;
            }
        }
        catch (IOException e) {
        }
        binder.broadcastStatus();
    }

    public ArrayList<String> getRecentChanges() {
        return new ArrayList<String>(recentChanges);
    }

    public String getRootDir() {
        return rootDir;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //registerReceiver(new WifiStateReceiver(), new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));

        Intent notifyIntent = new Intent(this, ControlActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notifyBuilder = new Notification.Builder(this);
        notifyBuilder.setContentTitle("Unison")
                .setSmallIcon(R.drawable.wait)
                .setContentText("Starting...")
                .setContentIntent(notifyPendingIntent);

        startForeground(NOTIFY_ID, notifyBuilder.build());

        new Thread() {
            @Override
            public void run() {
                startProcess();
            }
        }.start();

        return Service.START_STICKY;
    }

    private void startProcess() {
        try {
            procStatus = PROC_RUNNING;
            binder.broadcastStatus();

            proc = Runtime.getRuntime().exec(
                    new String[]{"./unison", "-batch", "-sshargs", "-y -i .ssh/dbr_key",
                            "-perms", "0", "-dontchmod",
                            "-repeat", "watch",},
                    new String[]{"HOME=" + getFilesDir(), "PATH=" + getFilesDir(), "LD_LIBRARY_PATH=" + getFilesDir() + "/lib"},
                    getFilesDir());

            BufferedReader errRead = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
            String line;
            while ((line = errRead.readLine()) != null) {
                processEvent(line);
                binder.broadcastUpdate(line + '\n');
            }
            errRead.close();
        }
        catch (IOException e) {
            Log.e("Unison", e.getMessage());
        }
    }

    private void processEvent(String line) {
        Log.v("Unison", line);

        for (String msg : statusMsg.keySet()) {
            if (line.contains(msg)) {
                int status = statusMsg.get(msg);

                switch (status) {
                    case STATUS_STARTED:
                        Matcher rootMatcher = rootPattern.matcher(line);
                        if (rootMatcher.matches()) {
                            rootDir = rootMatcher.group(1);
                        }
                    case STATUS_LOOKING:
                        notifyBuilder.setSmallIcon(R.drawable.wait)
                                .setContentText(getStatusMsg("Looking for changes"));
                        break;
                    case STATUS_WORKING:
                        notifyBuilder.setSmallIcon(R.drawable.sync1)
                                .setContentText(getStatusMsg("Propagating changes"));
                        break;
                    case STATUS_SYNCHRONIZED:
                        notifyBuilder.setSmallIcon(R.drawable.good)
                                .setContentText(getStatusMsg("Up-to-date"));
                        Matcher fileMatcher = filePattern.matcher(line);
                        if (fileMatcher.matches()) {
                            String fileAction = fileMatcher.group(1) + "ing";
                            String filePath = fileMatcher.group(3);
                            fileAction = fileAction
                                    .replace("Updating", "Updated")
                                    .replace("Deleting", "Deleted")
                                    .replace("Copying", "Copied");
                            recentChanges.push(getStatusMsg(fileAction + " " + filePath));
                        }
                        break;
                    case STATUS_ERROR:
                        procStatus = PROC_FINISHED;
                        notifyBuilder.setSmallIcon(R.drawable.error)
                                .setContentText(getStatusMsg("Fatal error. See status..."));
                        break;
                    case STATUS_KEY_ERROR:
                        procStatus = PROC_FINISHED;
                        notifyBuilder.setSmallIcon(R.drawable.error)
                                .setContentText(getStatusMsg("Fatal error: Invalid key file."));
                        stopForeground(false);
                        break;
                    case STATUS_LOST_CONNECTION:
                        procStatus = PROC_FINISHED;
                        notifyBuilder.setSmallIcon(R.drawable.error)
                                .setContentText(getStatusMsg("Fatal error. See status..."));

                        boolean autoRestart = PreferenceManager.getDefaultSharedPreferences(
                                getApplicationContext()).getBoolean("auto_restart", false);
                        int reconnectDelay = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(
                                getApplicationContext()).getString("restart_delay", "30"));
                        reconnectDelay = reconnectDelay > 10 ? reconnectDelay : 10;
                        if (autoRestart) {
                            reconnectTimer = new Timer();
                            reconnectTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    proc.destroy();
                                    startProcess();
                                }
                            }, reconnectDelay * 1000);
                        }
                        break;
                }

                binder.broadcastStatus();

                notifyManager.notify(NOTIFY_ID, notifyBuilder.build());
            }
        }
    }

    private String getStatusMsg(String inner) {
        DateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss] ");
        return dateFormat.format(Calendar.getInstance().getTime()) + inner;
    }

    /*
    public class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PreferenceManager.getDefaultSharedPreferences(
                    getApplicationContext()).getBoolean("auto_pause", false)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifiState == WifiManager.WIFI_STATE_DISABLING && procStatus == PROC_RUNNING) {
                    pause();
                }
                else if (wifiState == WifiManager.WIFI_STATE_ENABLED && procStatus == PROC_PAUSED) {
                    resume();
                }
            }
        }
    }*/
}
