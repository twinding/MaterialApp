package dk.tw.opencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CropPictureActivity extends AppCompatActivity {

    private final String TAG = "Crop Picture Activity";
    byte[] pictureData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_picture);

        Log.i(TAG, "hello");

        if (getIntent().getExtras() != null) {
            Log.i(TAG, "Extras was not null");
            Log.i(TAG, getIntent().getExtras().getString("pictureURI"));

            Uri uri = Uri.parse(getIntent().getExtras().getString("pictureURI"));

            Bitmap bmp = null;
            try {
                bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            } catch (IOException e) {
                Log.i(TAG, "Exception on creating BMP");
                e.printStackTrace();
            }

            ImageView iv = (ImageView) findViewById(R.id.imageView);
            iv.setImageBitmap(bmp);

            cropPicture(bmp);
        }
    }

    File cropInput, cropOutput;

    private void cropPicture(Bitmap input) {
        //Crop stuff, for cropping the warped picture

        FileOutputStream out = null;
        try {
            cropInput = File.createTempFile("cropInput", "bmp");
            out = new FileOutputStream(cropInput);
            input.compress(Bitmap.CompressFormat.PNG, 100, out);

            cropOutput = File.createTempFile("cropOutput", "bmp");

            Crop.of(Uri.fromFile(cropInput), Uri.fromFile(cropOutput)).withAspect(0,0).start(this);

            /*ImageView iv = (ImageView) findViewById(R.id.imageView);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(cropOutput.getAbsolutePath(), options);
            iv.setImageBitmap(bitmap);*/
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Crop.REQUEST_CROP && resultCode == RESULT_OK) {
//            ImageView iv = (ImageView) findViewById(R.id.imageView);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap = BitmapFactory.decodeFile(cropOutput.getAbsolutePath(), options);
//            iv.setImageBitmap(bitmap);


            try {
                File crop = File.createTempFile("crop", "png");
                FileOutputStream fos = new FileOutputStream(crop);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Uri uri = Uri.fromFile(crop);

                Intent intent = new Intent(this, FindContoursActivity.class);
                intent.putExtra("pictureURI", uri.toString());
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
