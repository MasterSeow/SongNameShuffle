package de.deus.seow.songnameshuffle;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int MY_WRITE_EXTERNAL_STORAGE = 102;
    private static final int RESULT_LOAD_PATH = 103;
    private static final Random random = new Random(System.currentTimeMillis());
    private boolean randomize = true;
    private Button btnRandomize,btnDerandomize;

    public interface ProgressListener {
        void updateProgress(int percentage);

        void finish();
    }

    ProgressListener progressListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDerandomize = findViewById(R.id.button_derandomize);
        btnRandomize = findViewById(R.id.button_randomize);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_WRITE_EXTERNAL_STORAGE);

        } else {
            btnRandomize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    randomize = true;
                    loadFolder();
                }
            });
            btnDerandomize.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    randomize = false;
                    loadFolder();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_WRITE_EXTERNAL_STORAGE:
                recreate();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_PATH && resultCode == RESULT_OK && null != data) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new ProgressDialog()).commit();
            btnDerandomize.setEnabled(false);
            btnRandomize.setEnabled(false);
            Uri uriTree = data.getData();
            DocumentFile documentFile = DocumentFile.fromTreeUri(this, uriTree);
            new RandomizeFilesTask().execute(documentFile.listFiles());
        }
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private void loadFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, RESULT_LOAD_PATH);
    }


    private class RandomizeFilesTask extends AsyncTask<DocumentFile, Integer, Void> {
        protected Void doInBackground(DocumentFile... documentFiles) {
            Looper.prepare();
            int count = documentFiles.length;
            for (int i = 0; i < count; i++) {
                if (randomize)
                    randomize(documentFiles[i]);
                else {
                    derandomize(documentFiles[i]);
                }
                publishProgress((int) ((i / (float) count) * 100));
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
            if (progressListener != null)
                progressListener.updateProgress(progress[0]);
        }

        protected void onPostExecute(Void result) {
            if (progressListener != null)
                progressListener.finish();
            btnDerandomize.setEnabled(true);
            btnRandomize.setEnabled(true);
        }

        private void randomize(DocumentFile file) {
            derandomize(file);
            int randomNumber = random.nextInt(1000);
            file.renameTo(String.valueOf(randomNumber) + "-" + file.getName());
        }

        private void derandomize(DocumentFile file) {
            if (file.getName().matches(".*\\d-.*")) {
                file.renameTo(file.getName().substring(file.getName().indexOf("-") + 1));
            }
        }
    }
}
