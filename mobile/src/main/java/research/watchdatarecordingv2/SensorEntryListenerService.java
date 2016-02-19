package research.watchdatarecordingv2;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.Charset;
import java.util.Timer;
import java.util.TimerTask;

public class SensorEntryListenerService extends WearableListenerService {

    final static String STOPPER = "FULL_STOP";
    final static String TAG = "SensorEntryListener";


    private static String message;
    static int count = 0;
    static boolean hasStartRecording = false;

    Timer timer = null;
    TimerTask recordTask = null;

    static StringBuffer log = new StringBuffer("");

    public SensorEntryListenerService() {
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        message = new String(messageEvent.getData(), Charset.forName("UTF-8"));
        Log.i(TAG, "message is " + message);
//        Intent saveFileIntent = new Intent(getBaseContext(), SaveFileActivity.class);
//        saveFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Intent serviceIntent = new Intent(getBaseContext(), ListenRecordsService.class);
        serviceIntent.setAction(ListenRecordsService.ACTION_LISTEN);
        startService(serviceIntent);

//        startActivity(saveFileIntent);
//        if(!hasStartRecording){
//            Log.i(TAG, "!hasStartRecording");
//            // showToast("message : " + new String(messageEvent.getData(), Charset.forName("UTF-8"))); // messageEvent.getData().toString());
//            log.append(message);
//            hasStartRecording = true;
//
//            timer = new Timer();
//            timer.schedule((recordTask = new TimerTask() {
//                @Override
//                public void run() {
//                    // Log.i(TAG, "message is " + message);
//                    if(count == 3){
//                        Log.i(TAG, "logs are " + log.toString());
//                        timer.cancel();
//                        recordTask.cancel();
//                        goToSaveFileActivity();
//                    }else{
//                        count++;
//                    }
//                }
//            }), 1000); // after 1s
//
//        }else{
//            Log.i(TAG, "hasStartRecording");
//            if(message.equals(STOPPER)){
//                Log.i(TAG, "message is stopper");
//                count = 2;
//                // find a way to check if I should really change the value
////                hasStartRecording = false;
////                goToSaveFileActivity();
//            }else{
//                log.append(message);
//                count = 0;
//            }
//        }
    }

    public void goToSaveFileActivity(){
//        Log.i()
        Intent saveFileIntent = new Intent(getBaseContext(), SaveFileActivity.class);
        saveFileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        saveFileIntent.putExtra("log", log.toString());
        saveFileIntent.putExtra("filename", System.currentTimeMillis() + "_logs" + ".csv"); // not whole file path
        hasStartRecording = false;
        startActivity(saveFileIntent);
    }



}
