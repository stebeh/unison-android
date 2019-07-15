package org.simeon.unison;

public interface OutputServiceListener {
    void onOutputUpdate(String line);

    void onOutputFinish();
}
