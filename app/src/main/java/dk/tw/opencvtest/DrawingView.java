package dk.tw.opencvtest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
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

import java.io.File;
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
//            geometry.setDocumentPreserveAspectRatio(new PreserveAspectRatio(PreserveAspectRatio.Alignment.XMidYMid, PreserveAspectRatio.Scale.Meet));
//            Log.i(TAG, "DrawingView: " + geometry.getDocumentPreserveAspectRatio().getAlignment().toString() + " " + geometry.getDocumentPreserveAspectRatio().getScale().toString());
            geometry.setDocumentViewBox(geometry.getDocumentViewBox().left * scale, geometry.getDocumentViewBox().top * scale, geometry.getDocumentViewBox().right * scale, geometry.getDocumentViewBox().bottom * scale);
            geometryWidth = geometry.getDocumentViewBox().right;
            geometryHeight = geometry.getDocumentViewBox().bottom;

        } catch (SVGParseException | IOException e) {
            e.printStackTrace();
        }

        surfaceHolder = getHolder();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        surfaceHolder.addCallback(this);

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



    public boolean onTouchEvent(MotionEvent event) { //http://stackoverflow.com/questions/13305706/android-drawing-objects-on-screen-and-obtaining-geometry-data/13308008#13308008
        if (!surfaceHolder.getSurface().isValid()) {
            Log.e("DrawingActivity", "Invalid surface!");
            return false;
        }

        Canvas canvas = surfaceHolder.lockCanvas();

        canvas.drawColor(Color.WHITE);

        float x = event.getX();
        float y = event.getY();

        material.renderToCanvas(canvas, material.getDocumentViewBox());

        canvas.save();
        canvas.scale(-scale, -scale);

        geometry.setDocumentViewBox(x / scale, y / scale, geometryWidth, geometryHeight);
        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        canvas.restore();


        if (event.getAction() == MotionEvent.ACTION_UP) {
            Log.i(TAG, "onTouchEvent geometry width: " + geometryWidth + " geometry height: " + geometryHeight);
            Log.i(TAG, "onTouchEvent x: " + x + ", y: " + y);
            Log.i(TAG, "onTouchEvent: x+width: " + (geometryWidth+x) + " y+height: " + (geometryHeight+y));
            Log.i(TAG, "onTouchEvent material viewbox: " + material.getDocumentViewBox() + ", width: " + material.getDocumentWidth() + ", height: " + material.getDocumentHeight());
            Log.i(TAG, "onTouchEvent geometry viewbox: " + geometry.getDocumentViewBox() + ", width: " + geometry.getDocumentWidth() + ", height: " + geometry.getDocumentHeight());
        }


        //print contents of file
        /*Log.i(TAG, "star path: " + geometryUri.getPath());
        File file = new File(geometryUri.getPath());

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                Log.i(TAG, "onTouchEvent #: " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/



        surfaceHolder.unlockCanvasAndPost(canvas);

        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawColor(Color.WHITE);


        Log.i(TAG, "surface created");

        material.renderToCanvas(canvas, material.getDocumentViewBox());
//        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());

        canvas.save();
        canvas.scale(scale,scale);
        canvas.rotate(180, canvas.getWidth()/2, canvas.getHeight()/2);

        geometry.renderToCanvas(canvas, geometry.getDocumentViewBox());
        canvas.restore();

        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

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

    public void longLogSplitOnLineBreak(String input, String prefix) {
        String[] splitResult = input.split("\r?\n|\r");
        for (String output : splitResult) {
            Log.i(TAG, prefix + ": " + output);
        }
    }
}