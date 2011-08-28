/*
 * Copyright 2011  Matthew Mole <code@gairne.co.uk>
 * 
 * This file is part of Inventory Manager.
 * 
 * Inventory Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Inventory Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Inventory Manager.  If not, see <http://www.gnu.org/licenses/>.
 */

package android.gairne.inventoryman;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

public class InventoryManagerActivity extends Activity {
	public static String APP_DIR = "invman/";
	public static String FILENAME = APP_DIR + "inventory";
	public static String CAMERA_DIR = APP_DIR + "images/";
	
	public static final int BARCODE_REQ = 0;
	public static final int CAMERA_REQ = 1;
	
	private String[] itemsOnDisplay;
	private ArrayAdapter<String> listAdapter;
	private ListView history;
	private Button scan;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		setContentView(R.layout.main);
	}
	
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
        CAMERA_DIR = (new File(root, CAMERA_DIR)).getAbsolutePath();
        		
        if (!new File(CAMERA_DIR).exists()) {
        	(new File(CAMERA_DIR)).mkdirs();
        }
        
        history = (ListView) findViewById(R.id.history);
        itemsOnDisplay = new String[0];
        readHistory();
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, itemsOnDisplay);
        history.setAdapter(listAdapter);
        
        registerForContextMenu(history);
        
        scan.setVisibility(Button.VISIBLE);
        scan.setOnClickListener(mScan);
        Toast.makeText(getApplicationContext(), "Ready to go", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.context_menu, menu);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
      switch (item.getItemId()) {
      case R.id.edit:
    	  return true;
      case R.id.delete:
    	  String[] newItemsOnDisplay = new String[itemsOnDisplay.length-1];
    	  for (int i = 0; i < newItemsOnDisplay.length; i++) {
    		  if (i >= info.position) {
    			  newItemsOnDisplay[i] = itemsOnDisplay[i+1];
    		  }
    		  else {
    			  newItemsOnDisplay[i] = itemsOnDisplay[i];
    		  }
    	  }
    	  itemsOnDisplay = newItemsOnDisplay;
    	  writeHistory();
    	  updateList();
    	  return true;
      default:
        return super.onContextItemSelected(item);
      }
    }
    
    
    
    public Button.OnClickListener mScan = new Button.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("com.google.zxing.client.android.SCAN.SCAN_MODE", "PRODUCT_MODE");
            startActivityForResult(intent, BARCODE_REQ);
        }
    };
    
    public void takePhoto(String barcode) {
    	Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
    	File photo = new File(CAMERA_DIR + "/" + barcode + ".jpg");
    	if (photo.exists()) {
    		return;
    	}
    	intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
    	startActivityForResult(intent, CAMERA_REQ);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == BARCODE_REQ) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
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
    		FileInputStream fis = new FileInputStream(input);
    		InputStreamReader isr = new InputStreamReader(fis);
    		BufferedReader in = new BufferedReader(isr);
    	    String line;
    	    while ((line = in.readLine()) != null) {
    	    	  temp.add(line);
    	    }
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
    	itemsOnDisplay = temp.toArray(itemsOnDisplay);
    }
}
