package org.simeon.unison;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.io.File;

public class RecentFilesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent_changes);

        final String rootDir = getIntent().getStringExtra("root_dir");
        final ArrayList<CharSequence> recentChanges = getIntent().getCharSequenceArrayListExtra("recent_changes");

        ListView listView = (ListView) findViewById(R.id.file_list);

        TextView textView = new TextView(this);
        textView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        textView.setText("No changes since start");
        ((ViewGroup) listView.getParent()).addView(textView);
        listView.setEmptyView(textView);

        listView.setAdapter(new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, recentChanges.toArray(new String[0])));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String fileMsg = recentChanges.get(i).toString();
                String filePath = rootDir + "/" + fileMsg.substring(fileMsg.lastIndexOf(" ") + 1);
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        MimeTypeMap.getFileExtensionFromUrl(filePath));
                try {
                    startActivity(new Intent().setAction(Intent.ACTION_VIEW)
                            .setDataAndType(Uri.fromFile(new File(filePath)), mimeType));
                }
                catch (ActivityNotFoundException e) {
                    Toast.makeText(getApplicationContext(),
                            "No application that can handle this file type", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        this.finish();
    }
}
