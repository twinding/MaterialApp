package dk.tw.opencvtest;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGImageView;
import com.caverock.androidsvg.SVGParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Custom list adapter for the lists of materials, cuts, and geometries
 */
public class CustomListAdapter extends ArrayAdapter {

    private final Activity context;
    private final String[] itemName;
    private final Type type;

    public CustomListAdapter(Activity context, String[] itemName, Type type) {
        super(context, R.layout.list_item_custom2, itemName);

        this.context = context;
        this.itemName = itemName;
        this.type = type;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_item_custom2, null, true);

        TextView textView = (TextView) rowView.findViewById(R.id.listText);
        SVGImageView imageView = (SVGImageView) rowView.findViewById(R.id.listIcon);

        textView.setText(itemName[position]);

        SVG svg;
        //Switch on type to figure out what directory to grab file from
        switch (type) {
            case CUTS:
                try {
                    File file = new File(context.getFilesDir(), "cutReadySVGs/" + itemName[position]);
                    FileInputStream fis = new FileInputStream(file);
                    svg = SVG.getFromInputStream(fis);
                    imageView.setSVG(svg);
                } catch (FileNotFoundException | SVGParseException e) {
                    e.printStackTrace();
                }
                break;
            case MATERIALS:
                try {
                    File file = new File(context.getFilesDir(), "svg/" + itemName[position] + ".svg");
                    FileInputStream fis = new FileInputStream(file);
                    svg = SVG.getFromInputStream(fis);
                    imageView.setSVG(svg);
                } catch (FileNotFoundException | SVGParseException e) {
                    e.printStackTrace();
                }
                break;
            case GEOMETRIES:
                try {
                    svg = SVG.getFromAsset(getContext().getAssets(), "SVG/" + itemName[position]);
                    imageView.setSVG(svg);
                } catch (SVGParseException | IOException e) {
                    e.printStackTrace();
                }
                break;
        }

        return rowView;
    }

    public enum Type{
        CUTS, MATERIALS, GEOMETRIES
    }
}
