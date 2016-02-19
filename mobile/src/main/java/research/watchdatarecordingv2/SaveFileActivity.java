package research.watchdatarecordingv2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class SaveFileActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    final static int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0;
    final static String TAG = "SaveFileActivty";
    final static String PATH = "/sensorentry";
    final static String MAPKEY_LOG = "LOG";

    private GoogleApiClient client;

    String log = "";
    String filename = "";
    TextView tvStatus;
    StringBuffer sb = new StringBuffer("");

    // message : Accepting the permission request allows the app to save all your hard work.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);

        log = getIntent().getStringExtra("log");
        filename = getIntent().getStringExtra("filename");

        tvStatus = (TextView) findViewById(R.id.tv_status);

        client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void askPermissionIfNeeded(){

        boolean hasPermission = (ContextCompat.checkSelfPermission(getBaseContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);

        // if not, ask for user's permission on runtime
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            Log.i(TAG, "app has no permission, asking user now");
        }else{
            Log.i(TAG, "app has permission, go ahead and save file");
            saveFile();
        }
        // hasStartRecording = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        client.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(client, this);
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Wearable.DataApi.removeListener(client, this);
        client.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE &&
                permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){

            saveFile();
        }
    }

    public void saveFile(){
        tvStatus.setText("Saving file.");

        // save file
        File rootpath = new File(Environment.getExternalStorageDirectory(), "accelerometer_record");
        if (!rootpath.exists()) {
            if (!rootpath.mkdir()) {
                Log.e(TAG, "failed to create directory");
            }
        }

        long endTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename = sdf.format(endTime) + "_logs" + ".csv";
        File file = new File(rootpath.toString(), filename);
        Log.i("TAG", "file is " + rootpath.toString());
        Log.i("TAG", "file is " + file.getPath());
        try {
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file.getPath());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            bufferedOutputStream.write(sb.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
