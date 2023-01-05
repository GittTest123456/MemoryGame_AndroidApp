package com.example.memorygame;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

public class ImageDownloadService extends Service {
    Thread bkgdThread;
    public ImageDownloadService() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action != null) {
            if (action.equals("download_image")) {
                String url = intent.getStringExtra("url");
                int number = intent.getIntExtra("number", 0);
                String identifier = intent.getStringExtra("identifier");
                startDownloadImage(url, number, identifier);
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    protected void startDownloadImage(String imgURL, int number, String identifier) {
        String destFilename = UUID.randomUUID().toString() +
                imgURL.substring(imgURL.lastIndexOf("."));
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File destFile = new File(dir, destFilename );

        // creating a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (downloadImage(imgURL, destFile)) {
                    Intent intent = new Intent();
                    intent.setAction("download_completed");
                    intent.putExtra("filename", destFilename);
                    intent.putExtra("number", number);
                    intent.putExtra("identifier", identifier);
                    sendBroadcast(intent);
                }
            }
        }).start();
    }

    protected boolean downloadImage(String imgURL, File destFile){
        try{
            URL url = new URL(imgURL);
            URLConnection conn = url.openConnection();
            InputStream in = conn.getInputStream();
            FileOutputStream out = new FileOutputStream(destFile);
            byte[] buf = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = in.read(buf)) != -1){
                out.write(buf, 0, bytesRead);
            }
            out.close();
            in.close();
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}