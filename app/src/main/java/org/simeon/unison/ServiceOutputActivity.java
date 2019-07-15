package org.simeon.unison;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

public class ServiceOutputActivity extends AppCompatActivity {

    ScrollView scrollView;
    TextView textView;

    boolean backgroundService;
    OutputService outputService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.output_activity);

        backgroundService = getIntent().getBooleanExtra("background", false);

        scrollView = findViewById(R.id.output_scroll);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        textView = findViewById(R.id.output_text);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // TODO: this is not an elegant way of generalizing this class
        Intent intent = new Intent(this, backgroundService ? UnisonService.class : SingleRunService.class);
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
            outputService = binder.getService();
            binder.addListener(serviceListener);

            String output = outputService.getOutput();
            if (!output.isEmpty())
                textView.setText(outputService.getOutput());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            binder.removeListener(serviceListener);
        }
    };

    private OutputServiceListener serviceListener = new OutputServiceListener() {

        @Override
        public void onOutputUpdate(final String errText) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(errText);
                }
            });
        }

        @Override
        public void onOutputFinish() {
            outputService.quit();
        }
    };
}
