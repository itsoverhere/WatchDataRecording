package research.watchdatarecordingv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class SaveFileActivity extends AppCompatActivity {

    final static int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0;
    final static String TAG = "SaveFileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_file);

        Intent serviceIntent = new Intent();
        serviceIntent.setClass(getBaseContext(), ListenRecordsService.class);
        serviceIntent.setAction(ListenRecordsService.ACTION_LISTEN);
        startService(serviceIntent);
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
//            saveFile();
        }
        // hasStartRecording = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        client.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        Wearable.DataApi.removeListener(client, this);
//        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        Wearable.DataApi.removeListener(client, dataListener);
//        client.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE &&
                permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED){

//            saveFile();
        }
    }

}
