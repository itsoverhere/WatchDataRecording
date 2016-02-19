package research.watchdatarecordingv2;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
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
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_LISTEN.equals(action)) {
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
                    // DataItem changed
                    DataItem item = event.getDataItem();

//                Log.i(TAG, "data changed " + item.getUri().getPath());
                    Log.i("tag", item.getUri().getPath());

                    if (item.getUri().getPath().startsWith(PATH)) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        // updateCount(dataMap.getInt(COUNT_KEY));
                        log = dataMap.getString(MAPKEY_LOG);
//                    dataMap.remove(M)
                        Log.i(TAG, "log is " + log);
                        sb.append(log);
                    }else if(item.getUri().getPath().startsWith("/stop")){
                        Log.i("taaag", "item is stop");
                        askPermissionIfNeeded();
                        Log.i(TAG, "STOP");
                    }
                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    // DataItem deleted
                } else {
                    Log.i(TAG, "EVENT TYPE IS " + event.getType());
                }
            }
        }
    };
}
