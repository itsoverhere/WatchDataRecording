package research.watchdatarecordingv2;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class ListenRecordsService extends IntentService {

    final static String TAG = " ListenRecordsService";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    static final String ACTION_LISTEN = "research.watchdatarecordingv2.action.listen";

    final static int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 0;
    final static String PATH = "/sensorentry";
    final static String MAPKEY_LOG = "LOG";

    private GoogleApiClient client;

    String log = "";
    String filename = "";
    static StringBuffer sb = new StringBuffer("");

    public ListenRecordsService() {
        super("ListenRecordsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG,"onhadleintent");
        if (intent != null) {
            final String action = intent.getAction();
            Log.i(TAG,"action is " + action);
            if (ACTION_LISTEN.equals(action)) {
                Log.i(TAG,"actionlitsen");
                init();
            }
        }
    }

    public void init(){
        client = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();

        client.connect();
        Log.i(TAG,"init");
    }

    GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.v(TAG, "Connected");
            Wearable.DataApi.addListener(client, dataListener);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.e(TAG, "Connection has been suspended on int " + i);
        }
    };

    GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(TAG, "Connection has failed: \n" + connectionResult.getErrorMessage());
        }
    };

    DataApi.DataListener dataListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();

                    Log.i(TAG, item.getUri().getPath());

                    if (item.getUri().getPath().startsWith(PATH)) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        log = dataMap.getString(MAPKEY_LOG);
                        Log.i(TAG, "log is " + log);
                        sb.append(log);
                    }else if(item.getUri().getPath().startsWith("/stop")){
                        // askPermissionIfNeeded(); Let's assume user has already accepted permission request AND enabled AMBIENT on wear
                        saveFile();
                        Log.i(TAG, "STOP");
                    }
                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.i(TAG, "DELETE");
                } else {
                    Log.i(TAG, "EVENT TYPE IS " + event.getType());
                }
            }
        }
    };
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE &&
//                permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
//                grantResults[0] == PackageManager.PERMISSION_GRANTED){
//
//            ();
//        }
//    }

    public void saveFile(){
        // create folder
        File rootpath = new File(Environment.getExternalStorageDirectory(), "accelerometer_record");
        if (!rootpath.exists()) {
            if (!rootpath.mkdir()) {
                Log.e(TAG, "failed to create directory");
            }
        }

        // save file
        long endTime = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String filename = sdf.format(endTime) + "_logs" + ".csv";
        File file = new File(rootpath.toString(), filename);
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

        notifyUserOfSavedFile(file.getPath());

        Wearable.DataApi.removeListener(client, dataListener);
        client.disconnect();
    }

    public void notifyUserOfSavedFile(String fileName){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getBaseContext())
                .setTicker("Watch recording has been saved")
                .setContentTitle("Watch recording has been saved")
                .setContentText("Your recording has been saved to " + fileName + ".")
                .setSmallIcon(R.mipmap.ic_launcher);
        ((NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE)).notify(0, builder.build());
    }
}
