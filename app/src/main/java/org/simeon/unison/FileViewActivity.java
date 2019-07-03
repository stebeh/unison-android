package org.simeon.unison;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileViewActivity extends AppCompatActivity {

    ScrollView scrollView;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.output_activity);

        scrollView = findViewById(R.id.output_scroll);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

        textView = findViewById(R.id.output_text);
        try {
            FileInputStream fileStream = new FileInputStream(getIntent().getStringExtra("file"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(fileStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            textView.setText(sb.toString());
            reader.close();
        }
        catch (FileNotFoundException e) {
            textView.setText("Log file does not exist");
        }
        catch (IOException e) {
            Log.d("Unison", e.getMessage());
        }
    }
}
