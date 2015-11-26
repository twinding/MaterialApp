package dk.tw.opencvtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DisplayPictureView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder surfaceHolder;
    private final String TAG = "DisplayPictureView";
    private SVG svg;

    public DisplayPictureView(Context context, String loadFrom, String filename) {
        super(context);
        Log.i(TAG, "Constructor");
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        if (loadFrom.equals("assets")) {
            try {
                Log.i(TAG, "Load from assets");
                svg = SVG.getFromAsset(getContext().getAssets(), "SVG/" + filename);
            } catch (SVGParseException | IOException e) {
                e.printStackTrace();
            }
        } else if (loadFrom.equals("internal")) {
            try {
                Log.i(TAG, "Load from internal");
                File file = new File(context.getFilesDir(), "cutReadySVGs/" + filename);
                FileInputStream fis = new FileInputStream(file);
                svg = SVG.getFromInputStream(fis);
            } catch (FileNotFoundException | SVGParseException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created");

        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawColor(Color.WHITE);

        svg.renderToCanvas(canvas);

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
