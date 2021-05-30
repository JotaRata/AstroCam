package com.jotarata.android.astrocam;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

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
        Init();
        resume();
    }
    public void Init()
    {
        mSensorMgr = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);

    }
    public void resume() {
        if (mSensorMgr == null) {
            throw new UnsupportedOperationException("Sensors not supported");
        }

        Sensor mAccelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true);
        boolean supported = mSensorMgr.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        if (!supported) {
            Log.d("SENSOR", "Linear acceerometer not found, fallback to accelerometer");
            mSensorMgr.unregisterListener(this, mAccelerometer);

            for (Sensor s : mSensorMgr.getSensorList(Sensor.TYPE_ALL))
            {
                if (s.getStringType().toUpperCase().contains("ACCELEROMETER"))
                {
                    mAccelerometer = s;
                    break;
                }
            }

             supported = mSensorMgr.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

            if (!supported) {
                new AlertDialog.Builder(mContext)
                        .setMessage("No se pudo iniciar el acelerometro en este dispositivo\nAsegurate entonces de dejar el telefono muy quieto")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
            //throw new UnsupportedOperationException("Accelerometer not supported");
        }
    }
    public void pause() {
        if (mSensorMgr != null) {
            mSensorMgr.unregisterListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
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
