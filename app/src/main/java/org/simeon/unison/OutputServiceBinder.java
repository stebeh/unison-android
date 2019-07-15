package org.simeon.unison;

import android.os.Binder;

import java.util.ArrayList;

public class OutputServiceBinder extends Binder {
    OutputService service;
    private ArrayList<OutputServiceListener> listeners = new ArrayList<>();

    public OutputServiceBinder(OutputService service) {
        this.service = service;
    }

    OutputService getService() {
        return service;
    }

    public void addListener(OutputServiceListener boundListener) { listeners.add(boundListener); }

    public void removeListener(OutputServiceListener boundListener) { listeners.remove(boundListener); }

    public void broadcastOutput(String out) {
        for (OutputServiceListener listener : listeners) {
            listener.onOutputUpdate(out);
        }
    }

    public void broadcastFinish() {
        for (OutputServiceListener listener : listeners) {
            listener.onOutputFinish();
        }
    }
}