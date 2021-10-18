package com.jotarata.android.astrocam;


import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.media.Image;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.util.BufferedFile;


public class FITSProcessor {

    public static FITSProcessor singleton;
    public  boolean processReady = true;

    private final ReentrantLock lock = new ReentrantLock();
    private ArrayList<RAWImage> frameList;
    public  Integer frameCount;
    private int imWidth, imHeight;

    // Bayer matrix arrangement
    private int arrangement;
    byte offset_1, offset_2;

    public FITSProcessor(){
        singleton = this;
    }

    public void InitFrameList(int capacity, int width, int height)
    {
        frameList = new ArrayList<>(capacity);
        frameCount = capacity - 1;
        processReady = false;

        imHeight = height;
        imWidth = width;

        try {
            arrangement = CameraActivity.mCamera2RawInstance.mCharacteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        }catch (NullPointerException e){
            Log.e("Critical Error", "No se pudo extraer la informacion del sensor");
            e.printStackTrace();
            return;
        }


        switch (arrangement)
        {
            case CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB:
            case  CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR:
                offset_1 = 1;
                offset_2 = 0;
                break;
            case  CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG:
            case CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG:
                offset_1 = 0;
                offset_2 = 1;
                break;
            default:
                break;

        }
        Log.d("Bayer Matrix mode:", String.valueOf(arrangement));

        for (int i = 0; i < capacity; i++) {
                AddFrame(new RAWImage(imWidth/2,imHeight/2));
        }
    }

    public void AddFrame(RAWImage frame)
    {
        frameList.add(frame);
    }

    public boolean CaptureSingleFrame(Image.Plane plane)
    {
        ByteBuffer buffer = plane.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        buffer.rewind();

        int pStride = plane.getPixelStride();
        int rStride = plane.getRowStride();

        //int imWidth = mImage.getWidth();
        //int imHeight = mImage.getHeight();


        long captureStartTime = System.currentTimeMillis();

        short halfHeight = (short) (imHeight / 2);
        short halfWidth = (short) (imWidth / 2);

        RAWImage mCurrentFrame = frameList.get(frameCount);

        for (int y = 0; y < halfHeight; y ++)
        {
            for (int x = 0; x < halfWidth; x ++)
            {
                int location = ((x + halfWidth) * pStride + y * rStride) * 2;
                byte offset = location / rStride  % 2 == 0 ? offset_1 : offset_2;
                location += pStride * offset;

                if (location < bytes.length - 1)
                {
                    byte byte1 = bytes[location];
                    byte byte2 = bytes[location + 1];

                    mCurrentFrame.data[x][halfHeight - y - 1] = (short) ((byte2 << 8) | (byte1 & 0xFF));
                }

            }
        }

        frameList.set(frameCount, mCurrentFrame);
        Log.d("RAW development Done in", String.valueOf(System.currentTimeMillis() - captureStartTime) + " ms");
        Log.d("TAG", "ProcessImage: Done, remaining " + String.valueOf(frameCount));
        captureStartTime = System.currentTimeMillis();
        //AddFrame(mCurrentFrame);

        lock.lock();
        try {
            frameCount--;
            synchronized (frameCount)
            {
                frameCount.notify();
            }

        }finally {
            lock.unlock();
        }

        return true;
    }


    public String CombineAllImages(SharedPreferences prefs, Action<String> OnFinish)
    {
        if (frameList == null)
        {
            return null;
        }
        short [][] finalImage = null;
        final int[] randomMove = new int[]{-1, 0, 1};
        final Random rand = new Random();
        for (RAWImage fits : frameList) {
                //Fits fits = new Fits(s);

                short[][] data = fits.data;
                if (finalImage == null)
                {
                    finalImage = data;

                    continue;
                }


                int width = finalImage.length;
                int height = finalImage[0].length;
                for (int y = 0; y < height; y ++)
                {
                    for (int x = 0; x < width; x++)
                    {
                        int rx = 0;
                        int ry = 0;
                        if (x > 1 && x < width - 2 && y > 1 && y < height - 2)
                        {
                            rx = randomMove[rand.nextInt(3)];
                            ry = randomMove[rand.nextInt(3)];
                        }
                        finalImage[x][y] += data[x + rx][y + ry];
                    }
                }

                // File is ready
                Log.d("Image stacker", "File " + fits.toString() + " is ready, preparing to delete..");
              //  File file = new File(s);
              //  file.delete();


        }

        Log.d("Image stacker Ready, files stacked ", String.valueOf(frameList.size()));

        final File mFile = new File(Environment.
                getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "FITS_" + generateTimestamp("_", false) + ".fits");

        Fits fits = new Fits();

        float expTime = (float) (frameList.size() * prefs.getFloat("frame_exp", 0) * 10E-9);
        try {
            if (finalImage == null)
            {
                Log.wtf("Error", "No se pudo crear la imagen final, compruebe su configuracion");
                return "";
            }
            BasicHDU<?> hdu = FitsFactory.hduFactory(finalImage);

            // Setup header
            hdu.addValue("OBJECT", "LIGHTS", "");
            hdu.addValue("PROGRAM", "ASTROCAM", "");
            hdu.addValue("DATE-OBS", generateTimestamp(":", true), "Date of the observation in UTC");
            hdu.addValue("TIMEZONE", TimeZone.getDefault().toString(), "Time zone at the location of the observation");
            hdu.addValue("EXPTIME", expTime, "Total integration time in seconds");
            hdu.addValue("INSTRUME", Build.MANUFACTURER.toUpperCase() + Build.MODEL.toUpperCase(), "Device name");
            hdu.addValue("STACK", frameList.size(), "Number of stacked images");

            hdu.addValue("SENSOR_W", prefs.getFloat("sensor_width", 0), "Width of the device's sensor in millimeters");
            hdu.addValue("SENSOR_H", prefs.getFloat("sensor_height", 0), "Height of the device's sensor in millimeters");
            hdu.addValue("SENSOR_I", prefs.getString("iso", "NULL"), "Sensitivity of the device's sensor in ISO values");

            fits.addHDU(hdu);
            BufferedFile bf = new BufferedFile(mFile, "rw");
            fits.write(bf);
            bf.close();
        } catch (FitsException | IOException e) {
            e.printStackTrace();
        }

        Log.d("Image stacker", "Final FITS saving with no issues");

        lock.lock();
        try
        {
            frameList.clear();
            frameCount = 0;
            processReady = true;
            frameList = null;
        }finally {
            lock.unlock();
        }
        OnFinish.run(mFile.getPath());
        return mFile.getPath();
    }

    public static String generateTimestamp(String timeSep, boolean utc) {
        SimpleDateFormat sdf = new SimpleDateFormat(String.format("yyyy-MM-dd'T'HH{0}mm{0}ss", timeSep), Locale.US); //yyyy_MM_dd_HH_mm

        if (utc)
        {
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return sdf.format(new Date());
    }

    public static class RAWImage
    {
        public short[][] data;
        public RAWImage(int width, int height)
        {
            data = new short[width][height];
        }
    }
}

