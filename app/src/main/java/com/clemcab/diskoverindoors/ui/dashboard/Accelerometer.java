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
        void onTranslation(float x_vel,float y_vel, float z_vel, float x, float y, float z);
    }
    private Listener listener;
    public void setListener(Listener l) {
        listener = l;
    }
    private SensorManager sensorManager;
    private Sensor accelSensor;
    private Sensor gravAccelSensor;
    private Sensor magSensor;
    private SensorEventListener sensorEventListener;

    private int sensorBufferMax = 4;
    private float sensorThresh = 0f;
    private float velThresh = 0.01f;
    private int usSamplingDelayAccel = 16667;

    private final float[] gravAccelReading = new float[3];
    private final float[] magReading = new float[3];
    private final float[] rotationMatrix = new float[9];

    private Queue<Float> sensorValsX = new LinkedList<>();
    private Queue<Float> sensorValsY = new LinkedList<>();
    private Queue<Float> sensorValsZ = new LinkedList<>();

    private float Simp(Float[] vals) {
        float velocity =
            (
                (
                        (float)sensorBufferMax / (1000000f/(float)usSamplingDelayAccel)
                )/8f
            ) * (
                (float)vals[0] + 3f*((float)vals[1]+(float)vals[2]) + (float)vals[3]
            );
        return velocity;
    }

    Accelerometer (Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

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
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(sensorEvent.values, 0, gravAccelReading,
                            0, gravAccelReading.length);

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(sensorEvent.values, 0, magReading,
                            0, magReading.length);

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    updateVals(sensorValsX, sensorEvent.values[0]);
                    updateVals(sensorValsY, sensorEvent.values[1]);
                    updateVals(sensorValsZ, sensorEvent.values[2]);
                    Float[] x = sensorValsX.toArray(new Float[0]);
                    Float[] y = sensorValsY.toArray(new Float[0]);
                    Float[] z = sensorValsZ.toArray(new Float[0]);

                    if (sensorValsX.size() >= sensorBufferMax) {
                        SensorManager.getRotationMatrix(rotationMatrix, null, gravAccelReading, magReading);

                        float x_avg = 0f;
                        float y_avg = 0f;
                        float z_avg = 0f;

                        for (int i = 0; i < sensorBufferMax; i++) {
                            x_avg += x[i]*rotationMatrix[0] + y[i]*rotationMatrix[1] + z[i]*rotationMatrix[2];
                            y_avg += x[i]*rotationMatrix[3] + y[i]*rotationMatrix[4] + z[i]*rotationMatrix[5];
                            z_avg += x[i]*rotationMatrix[6] + y[i]*rotationMatrix[7] + z[i]*rotationMatrix[8];
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

                                x_vel = x_vel*rotationMatrix[0] + y_vel*rotationMatrix[1] + z_vel*rotationMatrix[2];
                                y_vel = x_vel*rotationMatrix[3] + y_vel*rotationMatrix[4] + z_vel*rotationMatrix[5];
                                z_vel = x_vel*rotationMatrix[6] + y_vel*rotationMatrix[7] + z_vel*rotationMatrix[8];

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
                        if (listener != null)
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
        if (accelSensor.getMinDelay() > usSamplingDelayAccel)
            usSamplingDelayAccel = accelSensor.getMinDelay();
        else if (usSamplingDelayAccel > accelSensor.getMaxDelay() && accelSensor.getMaxDelay() > 0)
            usSamplingDelayAccel = accelSensor.getMaxDelay();

        sensorManager.registerListener(sensorEventListener, accelSensor, usSamplingDelayAccel);
        sensorManager.registerListener(sensorEventListener, gravAccelSensor, usSamplingDelayAccel);
        sensorManager.registerListener(sensorEventListener, magSensor, usSamplingDelayAccel);
    }
    public void unregister() {
        sensorManager.unregisterListener(sensorEventListener);
    }
}
