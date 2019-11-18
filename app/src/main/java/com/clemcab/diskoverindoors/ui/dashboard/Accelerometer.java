package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class Accelerometer {
    public interface  Listener {
        void onTranslation(float x_vel,float y_vel, float z_vel, float x, float y, float z);
    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    private int sensorBufferMax = 4;
    private float[] lastVelocity = {0f,0f,0f};
    private float sensorThresh = 0f;
    private float velThresh = 0f;
    private int usSamplingDelay = 16667;

    private Queue<Float> sensorValsX = new LinkedList<>();
    private Queue<Float> sensorValsY = new LinkedList<>();
    private Queue<Float> sensorValsZ = new LinkedList<>();

    private float Simp(Float[] vals) {
        float velocity =
            (
                (
                        (float)sensorBufferMax / (1000000f/(float)usSamplingDelay)
                )/8f
            ) * (
                (float)vals[0] + 3f*((float)vals[1]+(float)vals[2]) + (float)vals[3]
            );
        return velocity;
    }

    Accelerometer (Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        sensorEventListener = new SensorEventListener() {
            private void updateVals(Queue<Float> vals, float val) {
                if (vals.size() >= sensorBufferMax) {
                    for (int i = 0; i < vals.size(); i++) {
                        vals.remove();
                    }
                }
                vals.add(val);
            }

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (listener != null) {
                    updateVals(sensorValsX, sensorEvent.values[0]);
                    updateVals(sensorValsY, sensorEvent.values[1]);
                    updateVals(sensorValsZ, sensorEvent.values[2]);
                    Float[] x = sensorValsX.toArray(new Float[0]);
                    Float[] y = sensorValsY.toArray(new Float[0]);
                    Float[] z = sensorValsZ.toArray(new Float[0]);

                    if (sensorValsX.size() >= sensorBufferMax){
                        float x_avg = 0f;
                        float y_avg = 0f;
                        float z_avg = 0f;

                        for (int i=0; i<sensorBufferMax; i++) {
                            x_avg += x[i];
                            y_avg += y[i];
                            z_avg += z[i];
                        }
                        x_avg /= sensorBufferMax;
                        y_avg /= sensorBufferMax;
                        z_avg /= sensorBufferMax;

                        boolean inThresh =
                            Math.abs(x_avg) >= sensorThresh &&
                            Math.abs(y_avg) >= sensorThresh &&
                            Math.abs(z_avg) >= sensorThresh;
                        float x_vel = Simp(x);
                        float y_vel = Simp(y);
                        float z_vel = Simp(z);
                        if (inThresh) {
                            boolean inThreshVel =
                                Math.abs(x_vel) >= velThresh &&
                                Math.abs(y_vel) >= velThresh &&
                                Math.abs(z_vel) >= velThresh;
                            if (inThreshVel) {
                                x_vel = Simp(x);
                                y_vel = Simp(y);
                                z_vel = Simp(z);
                            } else {
                                x_vel = 0f;
                                y_vel = 0f;
                                z_vel = 0f;
                            }
                        } else {
                            x_vel = 0f;
                            y_vel = 0f;
                            z_vel = 0f;
                        }
                        listener.onTranslation(x_vel, y_vel, z_vel, x_avg, y_avg, z_avg);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

    }
    public void register() {
        if (sensor.getMinDelay() > usSamplingDelay)
            usSamplingDelay = sensor.getMinDelay();
        else if (usSamplingDelay > sensor.getMaxDelay() && sensor.getMaxDelay() > 0)
            usSamplingDelay = sensor.getMaxDelay();
        sensorManager.registerListener(sensorEventListener, sensor, usSamplingDelay);
    }
    public void unregister() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
