package dk.tw.opencvtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
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

        //Geometries are stored in the Assets folder at the moment, so load from there
        if (loadFrom.equals("assets")) {
            try {
                svg = SVG.getFromAsset(getContext().getAssets(), "SVG/" + filename);
            } catch (SVGParseException | IOException e) {
                e.printStackTrace();
            }
        //If it's a saved material or a ready-for-cut combined SVG, load from internal storage
        } else if (loadFrom.equals("internal")) {
            try {
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
        //Lock canvas
        Canvas canvas = surfaceHolder.lockCanvas();
        //Fill canvas with white
        canvas.drawColor(Color.WHITE);
        //Set document width/height to 90%, if 100% and the SVG is right at the edges of the viewBox,
        //it will be right at the edge of the screen
        try {
            svg.setDocumentHeight("90%");
            svg.setDocumentWidth("90%");
        } catch (SVGParseException e) {
            e.printStackTrace();
        }
        //Center the viewBox so it isn't at the left edge of the screen
        RectF viewBox = svg.getDocumentViewBox();
        float offsetX = viewBox.right * 0.1f;
        float offsetY = viewBox.bottom * 0.1f;
        svg.setDocumentViewBox(viewBox.left + offsetX, viewBox.top + offsetY, viewBox.right + offsetX, viewBox.bottom + offsetY);

        //Render the SVG to the canvas
        svg.renderToCanvas(canvas);
        //Unlock the canvas and draw
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
