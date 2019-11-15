package com.clemcab.diskoverindoors.ui.dashboard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.LinkedList;
import java.util.Queue;

public class Accelerometer {
    public interface  Listener {
        void onTranslation(float x,float y, float z);
    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    private int sensorBufferMax = 10;
    private float[] lastVelocity = {0f,0f,0f};
    private float sensorThresh = 0.1f;

    private Queue<Float> sensorValsX = new LinkedList<>();
    private Queue<Float> sensorValsY = new LinkedList<>();
    private Queue<Float> sensorValsZ = new LinkedList<>();

    private float Simp(Float[] vals) {
        float velocity = (((float)sensorBufferMax/50f)/8f) * ((float)vals[0] + 3f*((float)vals[1]+(float)vals[2]) + (float)vals[3]);
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

                        boolean inThresh = x_avg <= sensorThresh || y_avg <= sensorThresh || z_avg <= sensorThresh;
                        if (inThresh) {
                            lastVelocity[0] = Simp(x);
                            lastVelocity[1] = Simp(y);
                            lastVelocity[2] = Simp(z);
                            float x_vel = lastVelocity[0];
                            float y_vel = lastVelocity[1];
                            float z_vel = lastVelocity[2];

                            listener.onTranslation(x_vel, y_vel, z_vel);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

    }
    public void register() {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_GAME);
    }
    public void unregister() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
