package dk.tw.opencvtest;

import android.content.Intent;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class SelectMaterialListViewActivity extends AppCompatActivity {

    ListView listView;
    ArrayAdapter<String> openCvDataAdapter;
    ArrayAdapter<String> cutReadySvgsAdapter;
    ArrayAdapter<String> geometriesAdapter;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_material_list_view);

        button = (Button) findViewById(R.id.loadButton); //Load button reference
        listView = (ListView) findViewById(R.id.listView); //List view reference

        //Get directory of the saved cuts, combined geometries and materials
        File cutReadySvgsDir = new File(getFilesDir(), "cutReadySVGs");
        //Check if list is null (empty), and return empty String array if so
        String[] cutReadySvgs = cutReadySvgsDir.list() == null ? new String[]{} : cutReadySvgsDir.list();
        //Set up adapter for switching the list view to showing the saved cuts
        cutReadySvgsAdapter = new ArrayAdapter<>(this, R.layout.list_item_custom, android.R.id.text1, cutReadySvgs);

        try {
            //Get list of saved geometry SVGs, they're stored in the assets folder, so it is slightly different from the other saved items
//            String[] geometryList = getAssets().list("SVG");
            String[] geometryList = getAssets().list("SVG") == null ? new String[]{} : getAssets().list("SVG");
            //Set up adapter for switching the view to showing geometries
            geometriesAdapter = new ArrayAdapter<>(this, R.layout.list_item_custom, android.R.id.text1, geometryList);
        } catch (IOException e) {
            e.printStackTrace();
        }

//        String[] values = new String[]{"item1", "item2", "item3", "item4", "item5", "item6", "item7", "item8", "item9", "item10", "item11", "item12", "item13", "item14", "item15", "item16", "item17", "item18", "item19", "item20", "item21", "item22", "item23", "item24", "item25", "item26", "item27", "item28", "item29", "item30"};
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item_custom, android.R.id.text1, values);

        //Get directory of saved OpenCV data (materials), files are named the same as their respective SVGs
        File opencvdataDir = new File(getFilesDir(), "opencvdata");
        //Check if list is null (empty), and return empty String array if so
        String[] openCvData = opencvdataDir.list() == null ? new String[]{} : opencvdataDir.list();
        //Set up the initial adapter of the list view, the saved materials
        openCvDataAdapter = new ArrayAdapter<>(this, R.layout.list_item_custom, android.R.id.text1, openCvData);
        listView.setAdapter(openCvDataAdapter);
        //Set the first item in the list to be selected
        listView.setItemChecked(0, true);

        //Set the function to be called when the load button is pressed
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadButtonScannedMaterials();
            }
        });

        setActiveButton("materials");
    }

    public void materialsButton(View view) {
        setActiveButton("materials");

        //Set adapter
        listView.setAdapter(openCvDataAdapter);
        //Set first item as selected
        listView.setItemChecked(0, true);
        //Set method to be called when load button is pressed
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadButtonScannedMaterials();
            }
        });
    }

    public void cadFilesButton(View view) {
        setActiveButton("cadfiles");

        //Set adapter
        listView.setAdapter(geometriesAdapter);
        //Set first item as selected
        listView.setItemChecked(0, true);
        //Set method to be called when load button is pressed
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadButtonGeometries();
            }
        });
    }

    public void readyCutsButton(View view) {
        setActiveButton("readycuts");

        //Set adapter
        listView.setAdapter(cutReadySvgsAdapter);
        //Set first item as selected
        listView.setItemChecked(0, true);
        //Set method to be called when load button is pressed
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadButtonReadyCuts();
            }
        });
    }

    private void setActiveButton(String which) {
        Button materialsButton, cadFilesButton, readyCutsButton;
        materialsButton = (Button) findViewById(R.id.materialsButton);
        cadFilesButton = (Button) findViewById(R.id.cadFilesButton);
        readyCutsButton = (Button) findViewById(R.id.readyCutsButton);

        materialsButton.setPaintFlags(0);
        cadFilesButton.setPaintFlags(0);
        readyCutsButton.setPaintFlags(0);

        switch (which) {
            case "materials":
                setTitle("Scanned materials");
                materialsButton.setPaintFlags(materialsButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
            case "cadfiles":
                setTitle("CAD files");
                cadFilesButton.setPaintFlags(cadFilesButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
            case "readycuts":
                setTitle("Ready cuts");
                readyCutsButton.setPaintFlags(readyCutsButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
        }


    }

    public void loadButtonScannedMaterials() {
//        Toast.makeText(this, listView.getAdapter().getItem(listView.getCheckedItemPosition()).toString(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoadFromInternalStorageActivity.class);
        intent.putExtra("fileToLoad", listView.getAdapter().getItem(listView.getCheckedItemPosition()).toString());
        startActivity(intent);
    }

    public void loadButtonReadyCuts() {
//        Toast.makeText(this, "Load ready cuts", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DisplayPictureActivity.class);
        intent.putExtra("loadFrom", "internal");
        intent.putExtra("fileToLoad", listView.getAdapter().getItem(listView.getCheckedItemPosition()).toString());
        startActivity(intent);
    }

    public void loadButtonGeometries() {
//        Toast.makeText(this, "Load geometries", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DisplayPictureActivity.class);
        intent.putExtra("loadFrom", "assets");
        intent.putExtra("fileToLoad", listView.getAdapter().getItem(listView.getCheckedItemPosition()).toString());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Add menu items to the menu
        menu.addSubMenu("Scanned materials");
        menu.addSubMenu("Geometries");
        menu.addSubMenu("Ready cuts");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            //Switch depending on what was pressed in menu
            case "Scanned materials":
                Toast.makeText(this, "Scanned materials", Toast.LENGTH_SHORT).show();

                //Set adapter
                listView.setAdapter(openCvDataAdapter);
                //Set first item as selected
                listView.setItemChecked(0, true);
                //Set method to be called when load button is pressed
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadButtonScannedMaterials();
                    }
                });
                break;
            case "Geometries":
                Toast.makeText(this, "Geometries", Toast.LENGTH_SHORT).show();

                //Set adapter
                listView.setAdapter(geometriesAdapter);
                //Set first item as selected
                listView.setItemChecked(0, true);
                //Set method to be called when load button is pressed
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadButtonGeometries();
                    }
                });
                break;
            case "Ready cuts":
                Toast.makeText(this, "Ready cuts", Toast.LENGTH_SHORT).show();

                //Set adapter
                listView.setAdapter(cutReadySvgsAdapter);
                //Set first item as selected
                listView.setItemChecked(0, true);
                //Set method to be called when load button is pressed
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        loadButtonReadyCuts();
                    }
                });
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
