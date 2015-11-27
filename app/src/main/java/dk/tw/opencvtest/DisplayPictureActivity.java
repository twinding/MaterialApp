package dk.tw.opencvtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class DisplayPictureActivity extends AppCompatActivity {

    DisplayPictureView displayPictureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Get info on where to load from, and which file
        String loadFrom = getIntent().getExtras().getString("loadFrom");
        String filename = getIntent().getExtras().getString("fileToLoad");

        displayPictureView = new DisplayPictureView(this, loadFrom, filename);
        setContentView(displayPictureView);
    }
}
