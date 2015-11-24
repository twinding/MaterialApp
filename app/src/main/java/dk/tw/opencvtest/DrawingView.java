package dk.tw.opencvtest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class DrawingView extends SurfaceView implements SurfaceHolder.Callback {

    private final String TAG = "DrawingView";
    private final float scale = 0.78f;

    private String geometryFilename, materialFilename;
    private float offsetX = 0;
    private float offsetY = 0;
    private boolean moving = false;

    private final SurfaceHolder surfaceHolder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SVG material, geometry;
    private Uri geometryUri;
    private float materialWidth, materialHeight, geometryWidth, geometryHeight;
    private Bitmap imageBitmap;

    private class StringContainer{ //Container for a string value
        private String string;

        StringContainer(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        public void setString(String string) {
            this.string = string;
        }
    }

    /**
     * Creates a dialog that shows all files in the SVG asset folder and allows the user
     * to pick one
     * @return A Dialog that shows the list of SVGs and allows the user to pick one
     */
    private Dialog createAlertDialog() {
        //StringContainer is used because it's not possible to change a String from an inner anonymous class
        final StringContainer selectedItem = new StringContainer("");
        //Get builder
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        //Title text for the dialog
        builder.setTitle("Pick a geometry to add");

        try {
            //Get list of SVGs
            final CharSequence[] assetFileList = getContext().getAssets().list("SVG");
            //Initially set selected item to item 0
            selectedItem.setString(assetFileList[0].toString());
            //Single choice list with the SVGS, updates the StringContainer when something is selected
            builder.setSingleChoiceItems(assetFileList, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedItem.setString(assetFileList[which].toString()); //Update selected item
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set a confirmation button on the dialog
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getContext(), selectedItem.getString(), Toast.LENGTH_SHORT).show();
                addGeometry(selectedItem.getString());
            }
        });

        return builder.create();
    }

    /**
     * Add a geometry to the canvas, currently only supports adding a single geometry
     * Could be updated with a list of geometries, would require some rewriting of code in the whole class
     * @param filename The filename of the SVG to add to the picture
     */
    private void addGeometry(String filename) {
        try {
            geometryFilename = filename;

            Canvas canvas = surfaceHolder.lockCanvas();
            canvas.drawColor(Color.WHITE); //Fill canvas with white

            material.renderToCanvas(canvas, material.getDocumentViewBox());

            geometry = SVG.getFromAsset(getContext().getAssets(), "SVG/" + geometryFilename);
            geometryWidth = geometry.getDocumentViewBox().right * scale;
            geometryHeight = geometry.getDocumentViewBox().bottom * scale;

            geometry.setDocumentWidth("100%");
            geometry.setDocumentHeight("100%");

            geometry.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP));

            drawScaled(canvas, geometry, 100f, 100f);

            /*canvas.save(); //Save canvas settings

            //Scale for relative size
            canvas.scale(scale, scale);

            geometry.setDocumentViewBox(100, 100, geometryWidth, geometryHeight);
            geometry.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP));
            geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

            canvas.restore(); //Restore canvas settings*/

            surfaceHolder.unlockCanvasAndPost(canvas);
        } catch (SVGParseException | IOException e) {
            e.printStackTrace();
        }

    }

    public DrawingView(Context context) {
        super(context);
        createAlertDialog().show();
        materialFilename = "material.svg";

        try {
            material = SVG.getFromAsset(getContext().getAssets(), "SVG/" + materialFilename);
            materialWidth = material.getDocumentViewBox().right;
            materialHeight = material.getDocumentViewBox().bottom;

            /*geometry = SVG.getFromAsset(getContext().getAssets(), "SVG/shape20cm.svg");
            geometryWidth = geometry.getDocumentViewBox().right * scale;
            geometryHeight = geometry.getDocumentViewBox().bottom * scale;*/

        } catch (SVGParseException | IOException e) {
            e.printStackTrace();
        }

        surfaceHolder = getHolder();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        surfaceHolder.addCallback(this); //SurfaceHolder callback (surfaceCreated, surfaceChanged, surfaceDestroyed)

        /*//Converting SVG contents to Android VectorDrawable XML
        String geometryXML = convertSVG("shape20cm.svg");
        //Write to permanent file
        File file = new File(Environment.getExternalStorageDirectory().toString()+"/shape20cm.xml");
        geometryUri = Uri.fromFile(file);
        try {
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(geometryXML);
            fileWriter.close();
            MediaScannerConnection.scanFile(getContext(), new String[]{file.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    /**
     * When the view is touched this method is called, handles moving the geometry around when dragged
     */
    public boolean onTouchEvent(MotionEvent event) { //http://stackoverflow.com/questions/13305706/android-drawing-objects-on-screen-and-obtaining-geometry-data/13308008#13308008
        if (!surfaceHolder.getSurface().isValid()) {
            Log.e("DrawingActivity", "Invalid surface!");
            return false;
        }

        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawColor(Color.WHITE); //Fill canvas with white

        //Touch coordinates
        float touchX = event.getX();
        float touchY = event.getY();

        material.renderToCanvas(canvas, material.getDocumentViewBox());

        switch (event.getAction()) { //Switch depending on whether it is the initial touch, a dragging gesture, or the user stops the gesture
            case MotionEvent.ACTION_DOWN: //Initial touch
                RectF geometryViewBox = geometry.getDocumentViewBox();
                if (offsetViewBox(geometryViewBox).contains(touchX, touchY)) { //The touch gesture was inside the geometry viewbox
                    moving = true;
                    //Apply color filter to the moving geometry
                    geometry.setColorFilter(new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP));

                    //Calculate the offset from the touch position to the top left corner of geometry
                    offsetX = touchX - geometryViewBox.left;
                    offsetY = touchY - geometryViewBox.top;

                    drawScaled(canvas, geometry, touchX - offsetX, touchY - offsetY);
                } else drawScaled(canvas, geometry); //Draw the geometry in place
                break;
            case MotionEvent.ACTION_MOVE: //Dragging gesture
                if (moving) {
                    drawScaled(canvas, geometry, touchX - offsetX, touchY - offsetY);
                } else drawScaled(canvas, geometry); //Draw the geometry in place
                break;
            case MotionEvent.ACTION_UP: //Touch gesture is stopped
                if (moving) {
                    moving = false;
                    //Apply color filter to the moving geometry
                    geometry.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP));

                    drawScaled(canvas, geometry, touchX - offsetX, touchY - offsetY);
                } else drawScaled(canvas, geometry); //Draw the geometry in place
                break;
        }

        //Debug, draw viewbox
        //Needs to be offset because the coordinates change when scaling
        /*RectF rect = geometry.getDocumentViewBox();
        rect = offsetViewBox(rect);
        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawRect(rect, paint);*/
        this.invalidate();
        surfaceHolder.unlockCanvasAndPost(canvas);

        return true;
    }

    /**
     * Draw a scaled SVG to the given position on a canvas
     * @param canvas The canvas to draw to
     * @param geometry The SVG to draw
     * @param x X coordinate, if null will draw at the current position
     * @param y Y coordinate, if null will draw at the current position
     */
    private void drawScaled(Canvas canvas, SVG geometry, Float x, Float y) {
        RectF viewBox = geometry.getDocumentViewBox();
        //Can't figure out why multiplying by scale is not needed here
        float geometryWidth = viewBox.right - viewBox.left;
        float geometryHeight = viewBox.bottom - viewBox.top;

        if (x == null || y == null) { //If coordinates are null, the geometry will be redrawn at its current position
            x = viewBox.left;
            y = viewBox.top;
        } else { //Divide by scale so it follows the touched position
            x /= scale;
            y /= scale;
        }

        //Save canvas settings
        canvas.save();

        //Scale for relative size
        canvas.scale(scale, scale);

        //Set the viewbox to the touched position
        geometry.setDocumentViewBox(x, y, geometryWidth, geometryHeight);

        //Render
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        //Restore canvas to the state saved above
        canvas.restore();
    }

    /**
     * Convenience method
     * Draw a scaled SVG at its current position, to the given canvas by calling
     * drawScaled(canvas, geometry, x, y) with null as coordinate parameters
     * @param canvas The canvas to draw to
     * @param geometry The SVG to draw
     */
    private void drawScaled(Canvas canvas, SVG geometry) {
        drawScaled(canvas, geometry, null, null);
    }

    /**
     * Offset the given viewbox to overcome the differences in coordinates when scaling
     * @param viewBox The viewbox to offset
     * @return Viewbox corrected for scaling
     */
    private RectF offsetViewBox(RectF viewBox) {
        viewBox.offsetTo(viewBox.left * scale, viewBox.top * scale);
        return viewBox;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = surfaceHolder.lockCanvas();

        canvas.drawColor(Color.WHITE); //Fill canvas with white

        material.renderToCanvas(canvas, material.getDocumentViewBox());



        /*canvas.save(); //Save canvas settings

        //Scale for relative size
        canvas.scale(scale, scale);

        geometry.setDocumentViewBox(100, 100, geometryWidth, geometryHeight);
        geometry.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP));
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        canvas.restore(); //Restore canvas settings*/

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public void menuClicked() {
        Toast.makeText(getContext(), "Saving...", Toast.LENGTH_SHORT).show();
        combineSVGs(geometry);

        /*//Write to permanent file
        File file = new File(Environment.getExternalStorageDirectory().toString()+"/save.png");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            MediaScannerConnection.scanFile(getContext(), new String[]{file.getAbsolutePath()}, null, null);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    public void combineSVGs(SVG geometry) {
        float geometryX = geometry.getDocumentViewBox().left;
        float geometryY = geometry.getDocumentViewBox().top;

        DocumentBuilder db = null;
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document geometryDoc = db.parse(getContext().getAssets().open("SVG/" + geometryFilename));
            Document materialDoc = db.parse(getContext().getAssets().open("SVG/" + materialFilename));
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(".//polygon");
            NodeList geometryNL = (NodeList) expr.evaluate(geometryDoc, XPathConstants.NODESET);
            expr = xpath.compile(".//svg");
            NodeList materialNL = (NodeList) expr.evaluate(materialDoc, XPathConstants.NODESET);
            Element materialSvgNode = (Element) materialNL.item(0);
            printDocument(materialDoc);

            for (int i = 0; i < geometryNL.getLength(); i++) {
                Element node = (Element) geometryNL.item(i);
                node.setAttribute("stroke", "#FF0000");
                String points = node.getAttribute("points");
                node.setAttribute("points", offsetPoints(geometryX, geometryY, points));

                Node importNode = materialDoc.importNode(node, true);
                materialDoc.getDocumentElement().appendChild(importNode);
            }
            printDocument(materialDoc);
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            e.printStackTrace();
        }
    }

    public String offsetPoints(float x, float y, String points) {
        points = points.replace("\n", "").replace("\r", "").replace("\t", " ").replace("  ", " ");
        String[] coordinateSets = points.split(" ");
        float[] xCoords = new float[coordinateSets.length];
        float[] yCoords = new float[coordinateSets.length];
        String[] coord;
        for (int i = 0; i < coordinateSets.length; i++) {
            coord = coordinateSets[i].split(",");
            xCoords[i] = (Float.parseFloat(coord[0]) + x) * scale;
            yCoords[i] = (Float.parseFloat(coord[1]) + y) * scale;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < coordinateSets.length; i++) {
            sb.append(xCoords[i]).append(",").append(yCoords[i]).append(" ");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

     /**
     * Converts a given SVG string to Android Vector format
     * Only works for SVGs with <polygon> elements
     * @param svg The SVG, as string, to convert
     * @return Android Vector formatted string
     */
    public String convertSVG(String svg) {
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(getContext().getAssets().open(svg));
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(".//polygon");
            NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
//            Log.i(TAG, "XPath found " + nl.getLength() + " results");

            for (int i = 0; i < nl.getLength(); i++) {
                doc.renameNode(nl.item(i), null, "path");
                Element node = (Element) nl.item(i);
                String convertedPoints = convertPointsToPath(node.getAttribute("points"));
                node.removeAttribute("points");
                node.setAttribute("android:pathData", convertedPoints);

                node.setAttribute("android:strokeColor", node.getAttribute("stroke"));
                node.removeAttribute("stroke");

                node.setAttribute("android:fillColor", "#00000000");
                node.removeAttribute("fill");

                String strokeWidth = node.hasAttribute("stroke-width") ? node.getAttribute("stroke-width") : "1";
                node.setAttribute("android:strokeWidth", strokeWidth);
                node.removeAttribute("stroke-width");

                node.removeAttribute("stroke-miterlimit");
//                Log.i(TAG, "points: " + node.getAttribute("points"));
//                Log.i(TAG, "points converted to path: " + convertPointsToPath(node.getAttribute("points")));
            }

            expr = xpath.compile(".//svg");
            nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                Element node = (Element) nl.item(i);
                if (node.hasAttributes()) {
                    node.setAttribute("android:height", "24dp");
                    node.setAttribute("android:width", "24dp");
                    node.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android");

                    if (node.hasAttribute("width")) {
                        String width = node.getAttribute("width");
                        width = width.endsWith("px") ? width.substring(0, width.length()-2) : width;

                        node.setAttribute("android:viewportWidth", width);
                        node.removeAttribute("width");
                    }

                    if (node.hasAttribute("height")) {
                        String height = node.getAttribute("height");
                        height = height.endsWith("px") ? height.substring(0, height.length()-2) : height;

                        node.setAttribute("android:viewportHeight", height);
                        node.removeAttribute("height");
                    }

                    node.removeAttribute("xmlns");
                    node.removeAttribute("version");
                    node.removeAttribute("xmlns:xlink");
                    node.removeAttribute("id");
                    node.removeAttribute("x");
                    node.removeAttribute("y");
                    node.removeAttribute("viewBox");
                    node.removeAttribute("enable-background");
                    node.removeAttribute("xml:space");
                }
                doc.renameNode(nl.item(i), null, "vector");
            }

            return printDocument(doc);

//            String[] pointsAttr = ((String) expr.evaluate(doc, XPathConstants.STRING)).split("\\p{Space}");
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Prints the given document with appropriate XML prefixes
     * @param doc The document to print
     * @return The document as a string
     */
    public String printDocument(Document doc) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            Writer out = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            longLogSplitOnLineBreak(out.toString(), "PrintDocument");
            return out.toString();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Converts the points of a <polygon> element to <path> appropriate formatting
     * @param input The points to convert to path
     * @return The converted points as string
     */
    public String convertPointsToPath(String input) {
        Log.i(TAG, "convertPointsToPathinptu: " + input);
        input = input.replace("\n", "").replace("\r", "").replace("\t", " ").replace("  ", " ");
        Log.i(TAG, "convertPointsToPath input modified: " + input);
        String[] splitResult = input.split(",|\\s");
        StringBuilder result = new StringBuilder();
        result.append("M ").append(splitResult[0]).append(" ");
        result.append(splitResult[1]).append(" ");

        Log.i(TAG, "convertPointsToPath array: " + Arrays.toString(splitResult));
        Log.i(TAG, "convertPointsToPath splitresult: " + splitResult.length);
        for (int i = 2; i < splitResult.length; i += 2) {
            result.append("L ").append(splitResult[i]).append(" ");
            result.append(splitResult[i + 1]).append(" ");
        }
        result.append("z");

        return result.toString();
    }

    /**
     * Helper method to circumvent LogCat's 4000 character limit for printing
     * Only works if the input has line breaks
     * @param input The string to print
     * @param prefix Prefix text for the print, e.g. "MyApp"
     */
    public void longLogSplitOnLineBreak(String input, String prefix) {
        String[] splitResult = input.split("\r?\n|\r");
        for (String output : splitResult) {
            Log.i(TAG, prefix + ": " + output);
        }
    }
}