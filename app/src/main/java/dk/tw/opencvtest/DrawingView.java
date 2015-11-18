package dk.tw.opencvtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

    private float offsetX = 0;
    private float offsetY = 0;
    private boolean moving = false;

    private final SurfaceHolder surfaceHolder;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private SVG material, geometry;
    private Uri geometryUri;
    float materialWidth, materialHeight, geometryWidth, geometryHeight;


    public DrawingView(Context context) {
        super(context);

        try {
            material = SVG.getFromAsset(getContext().getAssets(), "material.svg");
            materialWidth = material.getDocumentViewBox().right;
            materialHeight = material.getDocumentViewBox().bottom;

            geometry = SVG.getFromAsset(getContext().getAssets(), "shape20cm.svg");
            geometryWidth = geometry.getDocumentViewBox().right * scale;
            geometryHeight = geometry.getDocumentViewBox().bottom * scale;

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
     * When the view is touched this method is called
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

        surfaceHolder.unlockCanvasAndPost(canvas);

        return true;
    }

    /**
     * Draw a scaled SVG to the given position on a canvas
     * @param canvas The canvas to draw to
     * @param geometry The SVG to draw
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void drawScaled(Canvas canvas, SVG geometry, float x, float y) {

        RectF viewBox = geometry.getDocumentViewBox();
        //Can't figure out why multiplying by scale is not needed here
        float geometryWidth = viewBox.right - viewBox.left;
        float geometryHeight = viewBox.bottom - viewBox.top;

        //Save canvas settings
        canvas.save();

        //Scale for relative size
        canvas.scale(scale, scale);

        //Set the viewbox to the touched position, divide by scale so it follows the touched position
        geometry.setDocumentViewBox(x / scale, y / scale, geometryWidth, geometryHeight);

        //Render
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        //Restore canvas to the state saved above
        canvas.restore();
    }

    /**
     * Draw a scaled SVG at its current position, to the given canvas
     * @param canvas The canvas to draw to
     * @param geometry The SVG to draw
     */
    private void drawScaled(Canvas canvas, SVG geometry) {
        RectF viewBox = geometry.getDocumentViewBox();
        //Can't figure out why multiplying by scale is not needed here
        float geometryWidth = viewBox.right - viewBox.left;
        float geometryHeight = viewBox.bottom - viewBox.top;

        //Save canvas settings
        canvas.save();

        //Scale for relative size
        canvas.scale(scale, scale);

        //Set the viewbox to the touched position, divide by scale so it follows the touched position
        geometry.setDocumentViewBox(viewBox.left, viewBox.top, geometryWidth, geometryHeight);

        //Render
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        //Restore canvas to the state saved above
        canvas.restore();
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

        canvas.save(); //Save canvas settings

        //Scale for relative size
        canvas.scale(scale, scale);

        geometry.setDocumentViewBox(100, 100, geometryWidth, geometryHeight);
        geometry.setColorFilter(new PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP));
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        canvas.restore(); //Restore canvas settings

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

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