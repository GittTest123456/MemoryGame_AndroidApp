package com.example.memorygame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity implements View.OnClickListener {
    private int pendingImages = 0;
    private Integer otherIndex;
    private List<Integer> openedImagesCount = new ArrayList<>();
    private List<String> allFilesNames = new ArrayList<>();
    private TextView matchCount;
    private Chronometer simpleChronometer;
    private MediaPlayer correctPlayer;
    private MediaPlayer wrongPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        simpleChronometer = findViewById(R.id.simpleChronometer);
        simpleChronometer.start();
        matchCount = findViewById(R.id.matches);
        matchCount.setText("0 of 6 matches");
        Intent intent = getIntent();
        List<String> selectedImages = intent.getStringArrayListExtra("selectedImages");
        allFilesNames = new ArrayList<>(selectedImages);
        for (int i= 1; i<=12; i++){
            ImageView image = findViewById(
                    getResources().getIdentifier("image" + i, "id", getPackageName())
            );
            image.setOnClickListener(this);
        }
        int correctId = getResources().getIdentifier("correct", "raw", getPackageName());
        correctPlayer = MediaPlayer.create(this, correctId);
        int wrongId = getResources().getIdentifier("wrong", "raw", getPackageName());
        wrongPlayer = MediaPlayer.create(this, wrongId);
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

    @Override
    public void onClick(View v){
        String name = getResources().getResourceEntryName(v.getId());
        int index = Integer.parseInt(name.substring(5));
        ImageView image = (ImageView) v;
        String selectedFileName = allFilesNames.get(index-1);
        if (pendingImages < 2 && !openedImagesCount.contains(index)){
            if (pendingImages == 0){
                otherIndex = index;
                updateImageView(selectedFileName, index);
                openedImagesCount.add(index);
                pendingImages += 1;
            }
            else{
                updateImageView(selectedFileName, index);
                pendingImages += 1;
                if (selectedFileName.equals(allFilesNames.get(otherIndex-1))){
                    openedImagesCount.add(index);
                    int numberMatches = openedImagesCount.size()/2;
                    matchCount = findViewById(R.id.matches);
                    matchCount.setText("" + numberMatches + " of 6 matches");
                    correctPlayer.start();
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            otherIndex = null;
                            pendingImages = 0;
                            if (openedImagesCount.size() == 12){
                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                            }
                        }
                    }, 2000);
                }
                else{
                    image.postDelayed(new Runnable(){
                        @Override
                        public void run(){
                            image.setImageResource(R.drawable.envelope);
                        }
                    },2000);
                    ImageView otherImage = findViewById(getResources().getIdentifier("image" + otherIndex, "id", getPackageName()));
                    otherImage.postDelayed(new Runnable(){
                        @Override
                        public void run(){
                            otherImage.setImageResource(R.drawable.envelope);
                            openedImagesCount.remove(otherIndex);
                            otherIndex = null;
                            pendingImages = 0;
                        }
                    },2000);
                    wrongPlayer.start();
                }
            }
        }
    }
}