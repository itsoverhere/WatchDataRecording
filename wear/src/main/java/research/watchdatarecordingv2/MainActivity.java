package research.watchdatarecordingv2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements DataApi.DataListener {

    // This will record all data from the following sensors:
    //    android.sensor.accelerometer          x, y, z (m/s2)
    //    android.sensor.step_counter           count (steps)
    //    android.sensor.wrist_tilt_gesture
    //    android.sensor.gyroscope              x, y, z (rad/s)
    //    android.sensor.magnetic_field         x, y, z (microT)
    //    android.sensor.light                  illuminance (lx)
    //    android.sensor.rotation_vector        x=y*z or East, y= points to North Pole, z = points to sky | perpendicular to ground
    //    android.sensor.orientation            azimuth, pitch, roll (degrees)
    //    android.sensor.gravity                x, y, x (m/s2)
    //    android.sensor.linear_acceleration    x, y, z (m/s2)
    //    android.sensor.significant_motion     N/A

    static final String TAG_PRE_RECORD = "PRE RECORD";
    static final String TAG_RECORDING = "RECORDING";
    static final String TAG_POST_RECORD = "POST RECORD";
    static final int SPEECH_REQUEST_CODE = 1;

    final static String PATH = "/sensorentry";
    final static String MAPKEY_LOG = "LOG";

    static final long CONNECTION_TIME_OUT_MS = 1000;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mStepCounter;
    private Sensor mWristTiltGesture;
    private Sensor mGyroscope;
    private Sensor mMagneticField;
    private Sensor mLight;
    private Sensor mRotationVector;
    private Sensor mOrientation;
    private Sensor mGravity;
    private Sensor mLinearAcceleration;
    private Sensor mSignificantMotion;

    private float xAccelerometer, yAccelerometer, zAccelerometer,
            stepCount,
            wristTiltGesture,
            xGyroscope, yGyroscope, zGyroscope,
            xMagneticField, yMagneticField, zMagneticField,
            light,
            xRotationVector, yRotationVector, zRotationVector, quaternionRotationVector, headingRotationVector,
            xOrientation,yOrientation, zOrientation,
            xGravity, yGravity, zGravity,
            xLinearAcceleration, yLinearAcceleration, zLinearAcceleration,
            significantMotion;

    private String activity = "None";

    float[] m_lastMagFields = new float[3];
    float[] m_lastAccels = new float[3];
    private float[] m_rotationMatrix = new float[16];
    private float[] m_orientation = new float[4];

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    SimpleDateFormat sdf_time = new SimpleDateFormat("HH:mm:ss");
    SimpleDateFormat sdf_date = new SimpleDateFormat("yyyy-MM-dd");

    TimerTask recordTask;
    Timer timer;

    TimerTask sendTask;
    Timer timer2; // for sending messages every second

//    StringBuffer s

    private GoogleApiClient client;

    private String nodeId = "-"; // nodeId of the connected handheld device

    private TextView tvStatus; // display either ON if recording or OFF if not
    private TextView tvActivity; // displays the current activity the user is performing
    private Button buttonChangeActivity;
    private RelativeLayout relLayoutBackground;

    private boolean isRecording = false;
    private boolean includesMetaData = false;

    StringBuffer log = new StringBuffer("");
    StringBuffer messageToSend = new StringBuffer("");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                tvStatus = (TextView) stub.findViewById(R.id.tv_status);
                tvActivity = (TextView) stub.findViewById(R.id.tv_activity);
                buttonChangeActivity = (Button) findViewById(R.id.button_change);
                relLayoutBackground = (RelativeLayout) stub.findViewById(R.id.background);

                tvStatus.setOnClickListener(tvStatusClickListener);
                buttonChangeActivity.setOnClickListener(buttonChangeActivityListener);

                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            }
        });
        setAmbientEnabled();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);

        // change background to black
        relLayoutBackground.setBackgroundColor(Color.parseColor("#000000"));
        // change text to white
        // textColor is already white
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        // change background to original color
        if(!isRecording)
            relLayoutBackground.setBackgroundColor(Color.parseColor("#AFB42B")); // green
        else
            relLayoutBackground.setBackgroundColor(Color.parseColor("#607d8b")); // blue-gray
    }

    public void startRecord(){
        createSensorNames();
        registerListeners();

        retrieveDeviceNode(); // DataAPI
        // initiateTransfer();

        timer = new Timer();
        timer.scheduleAtFixedRate((recordTask = new TimerTask() {
            @Override
            public void run() {
                if (!nodeId.equals("-")) {
                    if (!includesMetaData) {
                        // String currentMessage = SensorEntry.toStringSensorNames();
                        messageToSend.append(SensorEntry.toStringSensorNames());
                        // log.append(currentMessage);
                        // sendEntry(currentMessage);
//                        sendLogs(currentMessage);
                        includesMetaData = true;
                    }
                    record();
                }
            }
        }), 0, 10); // every 10ms

        timer2 = new Timer();
        timer2.scheduleAtFixedRate((sendTask = new TimerTask() {
            @Override
            public void run() {
                if (!nodeId.equals("-")) {
                    sendLogs(messageToSend.toString());
                    messageToSend = new StringBuffer("");
                    // record();
                }
            }
        }), 0, 1000); // every 1000ms

    }

    private void stopLogs() {
        Log.i("MA", "message in sendStop");
        client = getGoogleApiClient(this);
        if (nodeId != null) {
            new Thread(new stopLogsRunnable()).start();
        }
    }

    private void sendLogs(String message) {
        Log.i("MA", "message in sendLogs is " + message);
        client = getGoogleApiClient(this);
        if (nodeId != null) {
            new Thread(new sendLogsRunnable(message)).start();
        }
    }

    class sendLogsRunnable implements Runnable{
        String message = "";

        sendLogsRunnable(String message){
            this.message = message;
        }

        sendLogsRunnable(){}

        @Override
        public void run() {
            client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
            Wearable.DataApi.addListener(client, dataListener);
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH + System.currentTimeMillis());
            putDataMapReq.getDataMap().putString(MAPKEY_LOG, message.toString());
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(client, putDataReq)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess())
                                Log.i("SEND RUNNABLE", "data item result of sending log is success " + dataItemResult.getStatus());
                            else if (dataItemResult.getStatus().isCanceled())
                                Log.i("SEND RUNNABLE", "data item result of sending log is cancelled " + dataItemResult.getStatus());
                            else if (dataItemResult.getStatus().isInterrupted())
                                Log.i("SEND RUNNABLE", "data item result of sending log is interrupted " + dataItemResult.getStatus());
                        }
                    });
            // client.disconnect();
            Log.i("RUN", "sendlogsrunnable 3rd");
//
//            putDataMapReq = PutDataMapRequest.create(PATH + System.currentTimeMillis() + "second");
//            putDataMapReq.getDataMap().putString(MAPKEY_LOG, "hello");
//            putDataReq = putDataMapReq.asPutDataRequest();
//            Wearable.DataApi.putDataItem(client, putDataReq)
//                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//                        @Override
//                        public void onResult(DataApi.DataItemResult dataItemResult) {
//                            if (dataItemResult.getStatus().isSuccess())
//                                Log.i("SEND RUNNABLE", "2nd data item result of sending log is success " + dataItemResult.getStatus());
//                            else if (dataItemResult.getStatus().isCanceled())
//                                Log.i("SEND RUNNABLE", "2nd data item result of sending log is cancelled " + dataItemResult.getStatus());
//                            else if (dataItemResult.getStatus().isInterrupted())
//                                Log.i("SEND RUNNABLE", "2nd data item result of sending log is interrupted " + dataItemResult.getStatus());
//                        }
//                    });
//            // client.disconnect();
//            Log.i("RUN", "sendlogsrunnable 2nd 3rd");
        }
    }

    class stopLogsRunnable implements Runnable{

        stopLogsRunnable(){}

        @Override
        public void run() {
            client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
            Wearable.DataApi.addListener(client, dataListener);
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/stop"+System.currentTimeMillis());
            putDataMapReq.getDataMap().putString(MAPKEY_LOG, "bogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogusbogus");
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            Wearable.DataApi.putDataItem(client, putDataReq)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (dataItemResult.getStatus().isSuccess())
                                Log.i("STOP RUNNABLE", "data item result of sending log is success " + dataItemResult.getStatus());
                            else if (dataItemResult.getStatus().isCanceled())
                                Log.i("STOP RUNNABLE", "data item result of sending log is cancelled " + dataItemResult.getStatus());
                            else if (dataItemResult.getStatus().isInterrupted())
                                Log.i("STOP RUNNABLE", "data item result of sending log is interrupted " + dataItemResult.getStatus());
                        }
                    });
            client.disconnect();
        }
    }

    private void initiateTransfer() {
        Log.i("MA", "initiate transfer");
        client = getGoogleApiClient(this);
        if (nodeId != null) {
            new Thread(new initiateTransferRunnable("start")).start();
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        // nothing, not expecting anything from the handheld
    }

    class initiateTransferRunnable implements Runnable{

        String message;

        public initiateTransferRunnable(String message){
            this.message = message;
        }

        @Override
        public void run() {
            client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
            Wearable.MessageApi.sendMessage(client, nodeId, PATH, message.getBytes(Charset.forName("UTF-8"))).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if(sendMessageResult.getStatus().isSuccess()){
                                Log.e("MA", "success send");
                            }
                        }
                    }
            );
            client.disconnect();
            Log.i("MA", "message sent is " + message);
        }
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(googleApiClientConnectionCallback)
                .addOnConnectionFailedListener(googleApiClientOnConnectionFailedListener)
                .build();
    }

    private void retrieveDeviceNode() {
        Log.i("MA", "retrieveDeviceNode");
        client = getGoogleApiClient(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                Log.i(TAG_PRE_RECORD, "node size is " + nodes.size());
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                    Log.i(TAG_PRE_RECORD, "node id is " + nodeId);
                }
                client.disconnect();
            }
        }).start();
    }

    DataApi.DataListener dataListener = new DataApi.DataListener() {
        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {

        }
    };

    GoogleApiClient.OnConnectionFailedListener googleApiClientOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.i(TAG_PRE_RECORD, "Connection Failed. \n Result : \t " + connectionResult.getErrorMessage());
        }
    };

    GoogleApiClient.ConnectionCallbacks googleApiClientConnectionCallback  = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.i(TAG_PRE_RECORD, "Connection successful.");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.i(TAG_PRE_RECORD, "Connection suspended.");
        }
    };

    public void record() {
        SensorEntry sensorEntry = setReadSensorValues();
        String string = sdf_time.format(System.currentTimeMillis()) + ",";
//        string += sensorEntry.toString();
        messageToSend.append(string + sensorEntry.toString());
//
//        Log.i(TAG_RECORDING, string);
        //  log.append(string);
        // sendLogs(string);

        // sendEntry(string);
    }

    public void stopRecord() {
//        sendEntry("FULL_STOP");
//        sendEntry("gibberish");
//        initiateTransfer();
        recordTask.cancel();
        sendTask.cancel();
        timer.cancel();
        timer2.cancel();
        stopLogs();

        // currentMessage = "FULL_STOP";
        Log.e("stopRecord", "success");
    }

    public void registerListeners(){
        // accelerometer
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(sensorAccelerometerListener, mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);

        if (mAccelerometer == null) {
            Log.i(TAG_PRE_RECORD, "mAccelerometer is null");
        }

        // step counter
        mStepCounter = mSensorManager
                .getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(sensorStepCounterListener, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        if (mStepCounter == null) {
            Log.i(TAG_PRE_RECORD, "mStepCounter is null");
        }

        // wrist tilt gesture
//        mWristTiltGesture = mSensorManager
//                .getDefaultSensor(Sensor.TYPE_);
//        mSensorManager.registerListener(sensorStepCounterListener, mStepCounter, SensorManager.SENSOR_DELAY_FASTEST);

        // gyroscope
        mGyroscope = mSensorManager
                .getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(sensorGyroscopeListener, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);

        if (mGyroscope == null) {
            Log.i(TAG_PRE_RECORD, "mGyroscope is null");
        }

        // magnetic field
        mMagneticField = mSensorManager
                .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(sensorMagneticFieldListener, mMagneticField, SensorManager.SENSOR_DELAY_FASTEST);
//
        if (mMagneticField == null) {
            Log.i(TAG_PRE_RECORD, "mMagneticField is null");
        }

        // light
        mLight = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(sensorLightListener, mLight, SensorManager.SENSOR_DELAY_FASTEST);

        if (mLight == null) {
            Log.i(TAG_PRE_RECORD, "mLight is null");
        }

        // rotation vector
        mRotationVector = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mSensorManager.registerListener(sensorRotationVectorListener, mRotationVector, SensorManager.SENSOR_DELAY_FASTEST);

        if (mRotationVector == null) {
            Log.i(TAG_PRE_RECORD, "mRotationVector is null");
        }

        // orientation
        mOrientation = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(sensorOrientationListener, mOrientation, SensorManager.SENSOR_DELAY_FASTEST);

        if (mOrientation == null) {
            Log.i(TAG_PRE_RECORD, "mOrientation is null");
        }

        // gravity
        mGravity = mSensorManager
                .getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorManager.registerListener(sensorGravityListener, mGravity, SensorManager.SENSOR_DELAY_FASTEST);

        if (mGravity == null) {
            Log.i(TAG_PRE_RECORD, "mGravity is null");
        }

        // linear acceleration
        mLinearAcceleration = mSensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorManager.registerListener(sensorLinearAccelerationListener, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);

        if (mLinearAcceleration == null) {
            Log.i(TAG_PRE_RECORD, "mLinearAcceleration is null");
        }

        // significant motion
//        mSignificantMotion = mSensorManager
//                .getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
//        mSensorManager.registerListener(sensorSignificantMotionListener, mSignificantMotion, SensorManager.SENSOR_DELAY_FASTEST);
//
//        if (mSignificantMotion == null) {
//            Log.i(TAG_PRE_RECORD, "mSignificantMotion is null");
//        }
    }

    public void createSensorNames(){
        SensorEntry.sensorNames = new ArrayList<>();
        SensorEntry.sensorNames.add(SensorEntry.ACCELEROMETER_X);
        SensorEntry.sensorNames.add(SensorEntry.ACCELEROMETER_Y);
        SensorEntry.sensorNames.add(SensorEntry.ACCELEROMETER_Z);
        SensorEntry.sensorNames.add(SensorEntry.STEP_COUNTER);
        SensorEntry.sensorNames.add(SensorEntry.WRIST_TILT_GESTURE);
        SensorEntry.sensorNames.add(SensorEntry.GYROSCOPE_X);
        SensorEntry.sensorNames.add(SensorEntry.GYROSCOPE_Y);
        SensorEntry.sensorNames.add(SensorEntry.GYROSCOPE_Z);
        SensorEntry.sensorNames.add(SensorEntry.MAGNETIC_FIELD_X);
        SensorEntry.sensorNames.add(SensorEntry.MAGNETIC_FIELD_Y);
        SensorEntry.sensorNames.add(SensorEntry.MAGNETIC_FIELD_Z);
        SensorEntry.sensorNames.add(SensorEntry.ROTATION_VECTOR_X);
        SensorEntry.sensorNames.add(SensorEntry.ROTATION_VECTOR_Y);
        SensorEntry.sensorNames.add(SensorEntry.ROTATION_VECTOR_Z);
//        sensorNames.add(SensorEntry.ROTATION_VECTOR_HEADING);
//        sensorNames.add(SensorEntry.ROTATION_VECTOR_QUATERNION);
        SensorEntry.sensorNames.add(SensorEntry.LIGHT);
        SensorEntry.sensorNames.add(SensorEntry.ORIENTATION_X);
        SensorEntry.sensorNames.add(SensorEntry.ORIENTATION_Y);
        SensorEntry.sensorNames.add(SensorEntry.ORIENTATION_Z);
        SensorEntry.sensorNames.add(SensorEntry.GRAVITY_X);
        SensorEntry.sensorNames.add(SensorEntry.GRAVITY_Y);
        SensorEntry.sensorNames.add(SensorEntry.GRAVITY_Z);
        SensorEntry.sensorNames.add(SensorEntry.LINEAR_ACCELERATION_X);
        SensorEntry.sensorNames.add(SensorEntry.LINEAR_ACCELERATION_Y);
        SensorEntry.sensorNames.add(SensorEntry.LINEAR_ACCELERATION_Z);
//        SensorEntry.sensorNames.add(SensorEntry.SIGNIFICANT_MOTION);
    }

    public SensorEntry setReadSensorValues(){
        SensorEntry sensorEntry = new SensorEntry();
        ArrayList<Double> values = new ArrayList<>();

        values.add((double) xAccelerometer);
        values.add((double) yAccelerometer);
        values.add((double) zAccelerometer);

        values.add((double) stepCount);

        // values.add((double) wristTiltGesture);

        values.add((double) xGyroscope);
        values.add((double) yGyroscope);
        values.add((double) zGyroscope);

        values.add((double) xMagneticField);
        values.add((double) yMagneticField);
        values.add((double) zMagneticField);

        values.add((double) xRotationVector);
        values.add((double) yRotationVector);
        values.add((double) zRotationVector);
//            values.add((double) quaternionRotationVector);
//            values.add((double) headingRotationVector);

        values.add((double) light);

        float[] orientation = computeOrientation();
        values.add((double) orientation[0]);
        values.add((double) orientation[1]);
        values.add((double) orientation[2]);

        // gravity
        values.add((double) xGravity);
        values.add((double) yGravity);
        values.add((double) zGravity);

        // linear acceleration
        values.add((double) xLinearAcceleration);
        values.add((double) yLinearAcceleration);
        values.add((double) zLinearAcceleration);

        // significant motion
        values.add((double) significantMotion);

        sensorEntry.setSensorValues(values);

        sensorEntry.setActivity(activity);

//        if(sensorEntry.getSensorValues().size()+1 != SensorEntry.sensorNames.size()){
//            Log.i(TAG_RECORDING, "Detected sensor values = " + sensorEntry.getSensorValues().size() + "\n Set sensor values = " + SensorEntry.sensorNames.size());
//        }

        return sensorEntry;
    }

    private float[] computeOrientation() {
        float[] orientation = new float[3];
        if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels, m_lastMagFields)) {
            SensorManager.getOrientation(m_rotationMatrix, m_orientation);

            orientation[0] = (float) (Math.toDegrees(m_orientation[0])); // + Declination from GeoLocation latlng declination
            orientation[1] = (float) Math.toDegrees(m_orientation[1]);
            orientation[2] = (float) Math.toDegrees(m_orientation[2]);
        }
        return orientation;
    }

    SensorEventListener sensorAccelerometerListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            m_lastAccels = sensorEvent.values;
            xAccelerometer = sensorEvent.values[0];
            yAccelerometer = sensorEvent.values[1];
            zAccelerometer = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorStepCounterListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            stepCount = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorWristTiltGestureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            wristTiltGesture = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorGyroscopeListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            xGyroscope = sensorEvent.values[0];
            yGyroscope = sensorEvent.values[1];
            zGyroscope = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorMagneticFieldListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            m_lastMagFields = sensorEvent.values;
            xMagneticField = sensorEvent.values[0];
            yMagneticField = sensorEvent.values[1];
            zMagneticField = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            light = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorRotationVectorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            xRotationVector = sensorEvent.values[0];
            yRotationVector = sensorEvent.values[1];
            zRotationVector = sensorEvent.values[2];
            quaternionRotationVector = sensorEvent.values[3];
            headingRotationVector = sensorEvent.values[4];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorOrientationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            xOrientation = sensorEvent.values[0];
            yOrientation = sensorEvent.values[1];
            zOrientation = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorGravityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            xGravity = sensorEvent.values[0];
            yGravity = sensorEvent.values[1];
            zGravity = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorLinearAccelerationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            xLinearAcceleration = sensorEvent.values[0];
            yLinearAcceleration = sensorEvent.values[1];
            zLinearAcceleration = sensorEvent.values[2];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    SensorEventListener sensorSignificantMotionListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            significantMotion = sensorEvent.values[0];
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister all listeners
    }

    View.OnClickListener tvStatusClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(isRecording){
                tvStatus.setText("currently OFF");
                relLayoutBackground.setBackgroundColor(Color.parseColor("#afb42b"));
                // stop recording
                stopRecord();

            }else{
                tvStatus.setText("currently ON");
                relLayoutBackground.setBackgroundColor(Color.parseColor("#607d8b"));

                // start recording
                startRecord();
            }

            isRecording = !isRecording;
        }
    };

    View.OnClickListener buttonChangeActivityListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            displaySpeechRecognizer();
        }
    };

    // VOICE - ACTION RELATED CODE

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
    // This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
            Log.i(TAG_RECORDING, "User said " + spokenText);
            activity = spokenText;
            tvActivity.setText(spokenText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
