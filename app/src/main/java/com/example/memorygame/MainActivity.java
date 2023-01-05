package com.example.memorygame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button fetch;
    private EditText url;
    private ProgressBar progressBar;
    private TextView progressNote;
    private Thread bkgdThread;
    private ArrayList<String> imageUrl = new ArrayList<>();
    private ArrayList<String> fileNames = new ArrayList<>();
    private String identifier;
    private ArrayList<String> selectedImages = new ArrayList<>();
    //wait for the main activity UI to be fully loaded first before fetching from the url.
    //other urls to test: www.depositphotos.com; www.google.com
    //cold boot phone and rerun application to work.

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("download_completed")) {
                String filename = intent.getStringExtra("filename");
                fileNames.add(filename);
                int num = intent.getIntExtra("number", 0);
                String id = intent.getStringExtra("identifier");
//discard the response from the old url when the image from old url is returned when fetch is clicked again; interrupted.
                if (!id.equals(identifier)) {
                    return;
                }
                updateImageView(filename, num);
                updateProgress();

                if (imageUrl.size() > num) {
                    activateDownloadService(imageUrl.get(num),num+1);
                }
            }
        }
    };

    protected void updateProgress(){
        progressBar = findViewById(R.id.progressBar);
        int progress = progressBar.getProgress() + 1;
        progressBar.setProgress(progress);
        progressNote = findViewById(R.id.progressNote);
        progressNote.setText("Downloaded " + progress + " image(s) of 20 images");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fetch = findViewById(R.id.fetch);
        fetch.setOnClickListener(this);
        initReceiver();
        for (int i= 1; i<=20; i++){
            ImageView image = findViewById(
                    getResources().getIdentifier("image" + i, "id", getPackageName())
            );
            image.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v){
        int id = v.getId();
        if (id == R.id.fetch){
            if (bkgdThread != null) {
                showReadyUI();
                if (progressBar !=null){
                progressBar.setProgress(0);
                progressNote.setText("");
                imageUrl.clear();
                fileNames.clear();
                selectedImages.clear();
                identifier = null;}}
            url = findViewById(R.id.url);
            String urlAddress = url.getText().toString();
            if (!urlAddress.startsWith("https://")){
                Toast.makeText(getApplicationContext(), "The url has to start with https://", Toast.LENGTH_SHORT).show();
                return;
            }
            initImageURL(urlAddress);
            try{
                bkgdThread.join();}
            catch(InterruptedException e){
                System.out.println("InterruptedException");}
            if (imageUrl.isEmpty()){
                Toast.makeText(getApplicationContext(), "This url provided is either invalid or does not have available images for public extraction. Please input another url.", Toast.LENGTH_SHORT).show();
                return;
            }
            activateDownloadService(imageUrl.get(0), 1);
            progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            if (imageUrl.size()<20){
                Toast.makeText(getApplicationContext(), "This url does not have enough images (at least twenty) to start the selection and the game. Please try another url.", Toast.LENGTH_SHORT).show();
            }}
        else{
            if (progressBar.getProgress() == progressBar.getMax()){
                String name = getResources().getResourceEntryName(v.getId());
                int index = Integer.parseInt(name.substring(5));
                ImageView image = (ImageView) v;
                if (!selectedImages.contains(fileNames.get(index-1))){
                    if (selectedImages.size() < 6){
                        image.setColorFilter(ContextCompat.getColor(getApplicationContext(), androidx.cardview.R.color.cardview_shadow_start_color), PorterDuff.Mode.SRC_OVER);
                        selectedImages.add(fileNames.get(index-1));}
                }
                else{
                    selectedImages.remove(fileNames.get(index-1));
                    image.clearColorFilter();
                }
                if (selectedImages.size() == 6){
                    Intent intent = new Intent(MainActivity.this,MainActivity2.class);
                    ArrayList<String> duplicate = new ArrayList<>(selectedImages);
                    duplicate.addAll(selectedImages);
                    Collections.shuffle(duplicate);
                    intent.putStringArrayListExtra("selectedImages", duplicate);
                    startActivity(intent);
            }
        }

    }}

    protected void showReadyUI(){
        for (int i= 1; i<=20; i++){
            ImageView image = findViewById(
                    getResources().getIdentifier("image" + i, "id", getPackageName())
            );
            image.setImageResource(R.drawable.envelope);
            image.clearColorFilter();
        }

    }

    protected void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("download_completed");

        registerReceiver(receiver, filter);
    }


    protected void updateImageView(String filename, int num) {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File destFile = new File(dir, filename);
        ImageView image = findViewById(
                getResources().getIdentifier("image" + num, "id", getPackageName())
        );
        Bitmap bitmap = BitmapFactory.decodeFile(destFile.getAbsolutePath());
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 160, 160, true);
        image.setImageBitmap(resized);

    }

    protected void initImageURL(String url){
        bkgdThread = new Thread(new Runnable() {
            @Override
            public void run(){
                Document document = null;
                try {
                    document = Jsoup.connect(url).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (document != null){
                //Elements imageElements = document.select("img[src$=.jpg]");
                    Elements imageElements = document.select("img");
                    int n = 0;
                    for (Element imageElement: imageElements){
                    //make sure to get the absolute URL using abs: prefix
                    String strImageURL = imageElement.absUrl("src");
                    if (strImageURL.endsWith("jpg") || strImageURL.endsWith("png") || strImageURL.endsWith("jpeg")){
                        imageUrl.add(strImageURL);
                        String id =  UUID.randomUUID().toString();
                        identifier = id;
                        n += 1;
                        if (n == 20){
                            break;
                        }
                    }

                }

            }}
        });
        bkgdThread.start();

    }

    protected void activateDownloadService(String strImageURL, int n){
        Intent intent = new Intent(MainActivity.this,ImageDownloadService.class);
        intent.setAction("download_image");
        intent.putExtra("url", strImageURL);
        intent.putExtra("number", n);
        intent.putExtra("identifier", identifier);

        startService(intent);

    }


}