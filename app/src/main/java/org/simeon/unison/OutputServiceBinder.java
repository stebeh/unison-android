package org.simeon.unison;

import android.app.Service;
import android.os.Binder;

import java.util.ArrayList;

public class OutputServiceBinder extends Binder {
    Service service;
    private StringBuffer outputBuf = new StringBuffer();
    private ArrayList<OutputServiceListener> listeners = new ArrayList<>();

    public OutputServiceBinder(Service service) {
        this.service = service;
    }

    public Service getService() {
        return service;
    }

    public StringBuffer getBuffer() {
        return outputBuf;
    }

    public void addListener(OutputServiceListener boundListener) { listeners.add(boundListener); }

    public void removeListener(OutputServiceListener boundListener) { listeners.remove(boundListener); }

    public void broadcastUpdate(String line) {
        outputBuf.append(line);
        for (OutputServiceListener listener : listeners) {
            listener.onOutputUpdate();
        }
    }

    public void broadcastStatus() {
        for (OutputServiceListener listener : listeners) {
            listener.onStatusChange();
        }
    }
}