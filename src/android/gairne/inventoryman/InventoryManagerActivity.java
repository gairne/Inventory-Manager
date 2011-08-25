package android.gairne.inventoryman;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class InventoryManagerActivity extends Activity {
	public static String FILENAME = "inventory";
	
	private String[] itemsOnDisplay;
	private ArrayAdapter<String> listAdapter;
	private ListView history;
	private Button scan;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        scan = (Button) findViewById(R.id.scan);
        scan.setVisibility(Button.INVISIBLE);
        
        File root = Environment.getExternalStorageDirectory();
        if (!root.canWrite()){
        	Toast.makeText(getApplicationContext(), "Cannot write to root", Toast.LENGTH_SHORT).show();
        }
        FILENAME = (new File(root, FILENAME)).getAbsolutePath();
        
        history = (ListView) findViewById(R.id.history);
        itemsOnDisplay = new String[0];
        readHistory();
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemsOnDisplay);
        history.setAdapter(listAdapter);
        
        scan.setVisibility(Button.VISIBLE);
        scan.setOnClickListener(mScan);
        Toast.makeText(getApplicationContext(), "Ready to go", Toast.LENGTH_SHORT).show();
    }
    
    public Button.OnClickListener mScan = new Button.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, 0);
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("Scom.google.zxing.client.android.SCAN.SCAN_RESULT_FORMAT");
                ArrayList<String> temp = new ArrayList<String>(Arrays.asList(itemsOnDisplay));
                temp.add(contents);
                Log.v("INVMAN", contents);
                itemsOnDisplay = temp.toArray(itemsOnDisplay);
                writeHistory();
                updateList();
                Toast.makeText(getApplicationContext(), "Completed (" + itemsOnDisplay[itemsOnDisplay.length-1] + ")", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
            	Toast.makeText(getApplicationContext(), "Cancelled in onActivityResult", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    public void updateList() {
    	listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemsOnDisplay);
        history.setAdapter(listAdapter);
    }
    
    public void writeHistory() {
    	try {
    		FileOutputStream os = new FileOutputStream(new File(FILENAME));
    		OutputStreamWriter out = new OutputStreamWriter(os);
    		for (String line : itemsOnDisplay) {
    			out.write(line + "\n");
    		}
    		out.close();
    	}
    	catch (java.io.IOException e) {
    		Toast.makeText(getApplicationContext(), "IOException in writeHistory: " + e.getMessage() + " | " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    		return;
    	}
    }
    
    public void readHistory() {
    	ArrayList<String> temp = new ArrayList<String>();
    	try {
    		File input = new File(FILENAME);
    		if (!input.exists()) {
    			Toast.makeText(getApplicationContext(), "File does not exist: " + FILENAME, Toast.LENGTH_SHORT).show();
    			input.createNewFile();
    		}
    		if (!input.canRead()) {
    			Toast.makeText(getApplicationContext(), "canRead = False: " + FILENAME, Toast.LENGTH_SHORT).show();
    			return;
    		}
    		if (!input.canWrite()) {
    			Toast.makeText(getApplicationContext(), "canWrite = False: " + FILENAME, Toast.LENGTH_SHORT).show();
    			return;
    		}
    		if (!input.isFile()) {
    			Toast.makeText(getApplicationContext(), "Not a file: " + FILENAME, Toast.LENGTH_SHORT).show();
    			return;
    		}
    		//Toast.makeText(getApplicationContext(), "readHistory: File OK", Toast.LENGTH_SHORT).show();
    		FileInputStream fis = new FileInputStream(input);
    		InputStreamReader isr = new InputStreamReader(fis);
    		BufferedReader in = new BufferedReader(isr);
    	    String line;
    	    //Toast.makeText(getApplicationContext(), "readHistory: About to read", Toast.LENGTH_SHORT).show();
    	    while ((line = in.readLine()) != null) {
    	    	  temp.add(line);
    	    }
    	    //Toast.makeText(getApplicationContext(), "readHistory: Read OK", Toast.LENGTH_SHORT).show();
    	    in.close();
    	}
    	catch (java.io.FileNotFoundException e) {
    		Toast.makeText(getApplicationContext(), "FileNotFoundException in readHistory: " + e.getMessage() + " | " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	catch (java.io.IOException e) {
    		Toast.makeText(getApplicationContext(), "IOException in readHistory: " + e.getMessage() + " | " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	//Toast.makeText(getApplicationContext(), "Successfully read", Toast.LENGTH_SHORT).show();
    	itemsOnDisplay = temp.toArray(itemsOnDisplay);
    	//Toast.makeText(getApplicationContext(), "Successfully readHistory", Toast.LENGTH_SHORT).show();
    }
}