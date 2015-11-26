package dk.tw.opencvtest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.DigitsKeyListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class DrawingActivity extends AppCompatActivity {

    DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String filename = getIntent().getExtras().getString("filename");
        drawingView = new DrawingView(this, filename);
        setContentView(drawingView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.addSubMenu("Save");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getTitle().toString()) {
            case "Save":
                saveFilePromptForFilenameDialog().show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private Dialog saveFilePromptForFilenameDialog() {
        //Get builder
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Title text for the dialog
        builder.setTitle("Enter name of material");
        final EditText fileNameInput = new EditText(this);
        //Single line only
        fileNameInput.setSingleLine(true);
        //Only allow regular letters, numbers, and spaces
        fileNameInput.setKeyListener(DigitsKeyListener.getInstance("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz "));
        //Hint in EditText
        fileNameInput.setHint("Enter name...");
        builder.setView(fileNameInput);

        //Set positive button on the dialog
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String input = fileNameInput.getText().toString().trim(); //Get the string entered in the EditText and remove trailing and preceding spaces
                if (input.equals("")) { //If blank filename was entered
                    Toast.makeText(DrawingActivity.this, "Filename was blank, please enter a name.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DrawingActivity.this, input, Toast.LENGTH_SHORT).show();
                    drawingView.save(input);
                }
            }
        });
        //Set negative button on the dialog
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); //Dismiss the dialog
            }
        });

        return builder.create();
    }
}
