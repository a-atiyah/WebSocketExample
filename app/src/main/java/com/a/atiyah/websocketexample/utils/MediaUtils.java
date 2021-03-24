package com.a.atiyah.websocketexample.utils;

import android.media.MediaPlayer;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MediaUtils {

    public static void decodeAudio(String base64AudioData, File fileName, String path, MediaPlayer mp) {
        try {

            FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(Base64.decode(base64AudioData.getBytes(), Base64.DEFAULT));
            fos.close();

            try {
                mp = new MediaPlayer();
                mp.setDataSource(path);
                mp.prepare();
                mp.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void encodeAudio(String selectedPath) {

        byte[] audioBytes;
        try {

            // Just to check file size.. Its is correct i-e; Not Zero
            File audioFile = new File(selectedPath);
            long fileSize = audioFile.length();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileInputStream fis = new FileInputStream(new File(selectedPath));
            byte[] buf = new byte[1024];
            int n;
            while (-1 != (n = fis.read(buf)))
                baos.write(buf, 0, n);
            audioBytes = baos.toByteArray();

            // Here goes the Base64 string
//            _audioBase64 = Base64.encodeToString(audioBytes, Base64.DEFAULT);
//            Log.d(LOG_TAG, "encodeAudio:::" + _audioBase64);
//            sendAudio(_audioBase64);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
