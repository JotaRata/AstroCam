package com.jotarata.android.astrocam;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.Toast;

public class SensorActivity implements SensorEventListener
{
    private final Context mContext;
    private long lastUpdate;
    private float last_x;
    private float last_z;
    private float last_y;
    private final float SHAKE_THRESHOLD = 10;
    private OnShakeListener mListener;

    public  boolean isShaking = false;
    private SensorManager mSensorMgr;

    public interface OnShakeListener {
        void OnShake();
        void OnRest();
    }
    public SensorActivity(Context context)
    {
        mContext = context;
        resume();
    }
    public void resume() {
        Log.d("sensor run", "");
        mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        Sensor acelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }
        boolean supported = mSensorMgr.registerListener(this, acelerometer, SensorManager.SENSOR_DELAY_GAME);
        if (!supported) {
            mSensorMgr.unregisterListener(this, acelerometer);
            throw new UnsupportedOperationException("Accelerometer not supported");
        }
    }
    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            mSensorMgr = null;
        }
    }
    public void SetListener(OnShakeListener listener)
    {
        mListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms.
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                isShaking = false;

                float x = values[0];
                float y = values[1];
                float z = values[2];

                float speed = Math.abs(x+y+z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    //Log.d("sensor", "shake detected w/ speed: " + speed);

                    mListener.OnShake();
                }else
                {
                    mListener.OnRest();
                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
