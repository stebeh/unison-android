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

        Intent intent = new Intent(this, backgroundService ? UnisonService.class : SingleRunService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (Util.isServiceRunning(this,
                backgroundService ? UnisonService.class : SingleRunService.class)) {
            unbindService(connection);
        }
    }

    OutputServiceBinder binder;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            binder = (OutputServiceBinder) service;
            binder.addListener(serviceListener);

            textView.setText(binder.getBuffer().toString());
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            binder.removeListener(serviceListener);
        }
    };

    private OutputServiceListener serviceListener = new OutputServiceListener() {

        @Override
        public void onOutputUpdate() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() { textView.setText(binder.getBuffer().toString());
                }
            });
        }

        @Override
        public void onStatusChange() {

        }
    };
}
