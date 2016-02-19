package research.watchdatarecordingv2;

import java.util.ArrayList;

/**
 * Created by courtneyngo on 12/29/15.
 */
public class SensorEntry {

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

    static String ACCELEROMETER_X = "ACCELEROMETER_X";
    static String ACCELEROMETER_Y = "ACCELEROMETER_Y";
    static String ACCELEROMETER_Z = "ACCELEROMETER_Z";
    static String STEP_COUNTER = "STEP COUNT";
    static String WRIST_TILT_GESTURE = "WRIST_TILT_GESTURE"; // to check
    static String GYROSCOPE_X = "GYROSCOPE_X";

    static String GYROSCOPE_Y = "GYROSCOPE_Y";
    static String GYROSCOPE_Z = "GYROSCOPE_Z";
    static String MAGNETIC_FIELD_X = "MAGNETIC_FIELD_X";
    static String MAGNETIC_FIELD_Y = "MAGNETIC_FIELD_Y";
    static String MAGNETIC_FIELD_Z = "MAGNETIC_FIELD_Z";
    static String LIGHT = "LIGHT";
    static String ROTATION_VECTOR_X = "ROTATION_VECTOR_X";
    static String ROTATION_VECTOR_Y = "ROTATION_VECTOR_Y";
    static String ROTATION_VECTOR_Z = "ROTATION_VECTOR_Z";
    static String ROTATION_VECTOR_QUATERNION = "ROTATION_VECTOR_QUATERNION";
    static String ROTATION_VECTOR_HEADING = "ROTATION_VECTOR_HEADING";
    static String ORIENTATION_X = "ORIENTATION_X";
    static String ORIENTATION_Y = "ORIENTATION_Y";
    static String ORIENTATION_Z = "ORIENTATION_Z";
    static String GRAVITY_X = "GRAVITY_X";
    static String GRAVITY_Y = "GRAVITY_Y";
    static String GRAVITY_Z = "GRAVITY_Z";
    static String LINEAR_ACCELERATION_X = "LINEAR_ACCELERATION_X";
    static String LINEAR_ACCELERATION_Y = "LINEAR_ACCELERATION_Y";
    static String LINEAR_ACCELERATION_Z = "LINEAR_ACCELERATION_Z";
    static String SIGNIFICANT_MOTION = "SIGNIFICANT_MOTION";

    static String ACTIVITY = "ACTIVITY";
//    static String PROXIMITY = "PROXIMITY";
//    static String AMBIENT_TEMPERATURE = "AMBIENT_TEMPERATURE";
//    static String PRESSURE = "PRESSURE";
//    static String HUMIDITY = "HUMIDITY";

    static ArrayList<String> sensorNames;
    private ArrayList<Double> sensorValues;

    private String activity;

    public static ArrayList<String> getSensorNames() {
        return sensorNames;
    }

    public static void setSensorNames(ArrayList<String> sensorNames) {
        SensorEntry.sensorNames = sensorNames;
    }

    public ArrayList<Double> getSensorValues() {
        return sensorValues;
    }

    public void setSensorValues(ArrayList<Double> sensorValues) {
        this.sensorValues = sensorValues;
    }

    public Double getValueOf(String sensorName){
        int index = -1;
        index = sensorNames.indexOf(sensorName);
        return sensorValues.get(index);
    }

    public String toString(){
        StringBuffer stringBuffer = new StringBuffer("");
        for(Double d: sensorValues){
            stringBuffer.append(d + ",");
        }

        stringBuffer.append(this.activity + "\n");
        return stringBuffer.toString();
    }

    public static String toStringSensorNames(){
        StringBuffer stringBuffer = new StringBuffer("");
        stringBuffer.append("Time,");
        for(String s: sensorNames){
            stringBuffer.append(s +",");
        }
        stringBuffer.append("\n");
        return stringBuffer.toString();
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}
