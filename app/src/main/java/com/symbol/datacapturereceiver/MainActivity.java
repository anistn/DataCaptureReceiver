package com.symbol.datacapturereceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    /**
     *  *************************************************************
     *  How to configure DataWedge to send intent to this application
     *  *************************************************************
     *
     * More info on DataWedge can be found here:
     *      http://techdocs.zebra.com/datawedge/5-0/guide/about/
     * More info on DataCapture Intents can be found here:
     *      http://techdocs.zebra.com/emdk-for-android/6-3/tutorial/tutdatacaptureintent/
     *
     * Setup1 (Automatic):
     * 0- Install the app on the device
     * 1- Copy the dwprofile_datacapture.db file that is provided in the asset folder in
     * device sdcard
     * 2- Open DataWedge
     * 3- Select Settings in the Menu
     * 4- Select Import Profile
     * 5- Select the previously imported file
     * 6- Quit DataWedge
     * 7- Run the application
     *
     * Setup2 (Manual):
     * 0- Install the app on the device
     * 1- Open DataWedge
     * 2- Menu -> New Profile
     * 3- Enter a name for the profile
     * 4- Select the newly created profile
     * 5- Select Applications -> Associated apps
     * 6- Menu -> New app/activity
     * 7- Select com.symbol.datacapturereceiver
     * 8- Select com.symbol.datacapturereceiver.MainActivity
     * 9- Go back
     * 10- Disable Keystroke output
     * 11- Enable Intent Output
     * 12- Select Intent Output -> Intent Action
     * 13- Enter (case sensitive): com.symbol.datacapturereceiver.RECVR
     * 14- Select Intent Output -> Intent Category
     * 15- Enter (case sensitive): android.intent.category.DEFAULT
     * 16- Select Intent Output -> Intent Delivery
     * 17- Select via StartActivity to handle the date inside the OnCreate Method and in onNewIntent
     *     If you select this option, don't forget to add com.android.launcher in the Associated apps if
     *     you want your app to be started from the launcher when a barcode is scanned, otherwise the scanner
     *     will only work when the datacapturereceiver app is running
     *     Configure android:launchMode="" in your AndroidManifest.xml application tag if you want
     *     specific behaviors.
     *     https://developer.android.com/guide/topics/manifest/activity-element.html
     *     http://androidsrc.net/android-activity-launch-mode-example/
     * 18- Select Broadcast Intent to handle the data inside a Broadcast Receiver
     * 19- Configure the Symbology : go to Barcode input -> Decoders
     * 20- Configure Aim (only the barcode in the center of the scanner target is read)
     *      Go to Barcode input -> Reader params -> Picklist -> Enabled
     */

    private static String TAG = "DataCaptureReceiver";
    private static String mProfileName = "com.symbol.datacapturereceiver";
    private static String mIntentAction = "com.symbol.datacapturereceiver.RECVR";
    private static String mIntentCategory = "android.intent.category.DEFAULT";
    private EditText et_results;
    private String mResults = "";
    private boolean mContinuous = true;
    private Date mScanDate = null;

    /**
     * Local Broadcast receiver
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    handleDecodeData(intent);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_results = (EditText)findViewById(R.id.et_results);

        Button btStart = (Button) findViewById(R.id.button_start);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mScanDate  = new Date();
                sendDataWedgeIntentWithExtra(DataWedgeConstants.DWAPI_ACTION_SOFTSCANTRIGGER, DataWedgeConstants.EXTRA_PARAMETER, DataWedgeConstants.DWAPI_START_SCANNING);
            }
        });

        Button btStop = (Button) findViewById(R.id.button_stop);
        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataWedgeIntentWithExtra(DataWedgeConstants.DWAPI_ACTION_SOFTSCANTRIGGER, DataWedgeConstants.EXTRA_PARAMETER, DataWedgeConstants.DWAPI_STOP_SCANNING);
            }
        });

        Button btToggle = (Button) findViewById(R.id.button_toggle);
        btToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataWedgeIntentWithExtra(DataWedgeConstants.DWAPI_ACTION_SOFTSCANTRIGGER, DataWedgeConstants.EXTRA_PARAMETER, DataWedgeConstants.DWAPI_TOGGLE_SCANNING);
            }
        });

        Button btEnable = (Button) findViewById(R.id.button_enable);
        btEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataWedgeIntentWithExtra(DataWedgeConstants.DWAPI_ACTION_SCANNERINPUTPLUGIN, DataWedgeConstants.EXTRA_PARAMETER, DataWedgeConstants.DWAPI_PARAMETER_SCANNERINPUTPLUGIN_ENABLE);
            }
        });

        Button btDisable = (Button) findViewById(R.id.button_disable);
        btDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataWedgeIntentWithExtra(DataWedgeConstants.DWAPI_ACTION_SCANNERINPUTPLUGIN, DataWedgeConstants.EXTRA_PARAMETER, DataWedgeConstants.DWAPI_PARAMETER_SCANNERINPUTPLUGIN_DISABLE);
            }
        });

        Button btCreate = (Button) findViewById(R.id.button_create);
        btCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Intent profile creation
                createProfile();
            }
        });

        Button btImport = (Button) findViewById(R.id.button_import);
        btImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //"database profile auto creation" import mode (filebased)
                importProfile("dwprofile_com.symbol.datacapturereceiver");
            }
        });

        Button btSwitch = (Button) findViewById(R.id.button_switch);
        btSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchScannerParams();
            }
        });

        Button btClear = (Button) findViewById(R.id.button_clear);
        btClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mResults = "";
                et_results.setText(mResults);
            }
        });

        // in case we have been launched by the DataWedge intent plug-in
        // using the StartActivity method let's handle the intent
        Intent i = getIntent();
        handleDecodeData(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the internal broadcast receiver when we are alive
        IntentFilter myFilter = new IntentFilter();
        myFilter.addAction(mIntentAction);
        myFilter.addCategory(mIntentCategory);
        this.getApplicationContext().registerReceiver(mMessageReceiver, myFilter);
    }

    @Override
    protected void onPause() {
        // Unregister internal broadcast receiver when we are going in background
        this.getApplicationContext().unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    // This one is necessary only if you choose to send the data by StartActivity
    @Override
    protected void onNewIntent(Intent intent) {
        if(handleDecodeData(intent))
            return;
        super.onNewIntent(intent);
    }

    // This method is responsible for getting the data from the intent
    // formatting it and adding it to the end of the edit box
    private boolean handleDecodeData(Intent i) {
        // check the intent action is for us
        if ( i.getAction().contentEquals(mIntentAction) ) {

            Date current = new Date();

            long diff = 0;
            if(mScanDate != null)
                diff = current.getTime() - mScanDate.getTime();

            // define a string that will hold our output
            String out = "";
            // get the source of the data
            String source = i.getStringExtra(DataWedgeConstants.SOURCE_TAG);
            // save it to use later
            if (source == null) source = "scanner";
            // get the data from the intent
            String data = i.getStringExtra(DataWedgeConstants.DATA_STRING_TAG);

            // let's define a variable for the data length
            Integer data_len = 0;
            // and set it to the length of the data
            if (data != null)
                data_len = data.length();

            String sLabelType = null;

            // check if the data has come from the barcode scanner
            if (source.equalsIgnoreCase("scanner")) {
                // check if there is anything in the data
                if (data != null && data.length() > 0) {
                    // we have some data, so let's get it's symbology
                    sLabelType = i.getStringExtra(DataWedgeConstants.LABEL_TYPE_TAG);
                    // check if the string is empty
                    if (sLabelType != null && sLabelType.length() > 0) {
                        // format of the label type string is LABEL-TYPE-SYMBOLOGY
                        // so let's skip the LABEL-TYPE- portion to get just the symbology
                        sLabelType = sLabelType.substring(11);
                    }
                    else {
                        // the string was empty so let's set it to "Unknown"
                        sLabelType = "Unknown";
                    }
                    // let's construct the beginning of our output string
                    out = "Source: Scanner, " + "Symbology: " + sLabelType + ", Length: " + data_len.toString() + ", Data: ";
                }
            }

            // check if the data has come from the MSR
            if (source.equalsIgnoreCase("msr")) {
                // construct the beginning of our output string
                out = "Source: MSR, Length: " + data_len.toString() + ", Data: ";
            }

            if(data != null)
            {

                //if(sLabelType.equalsIgnoreCase("CODE128"))
                //if(sLabelType.equalsIgnoreCase("QRCODE"))
                out = out + showSpecialChars(data);
            }

            if(diff > 0) // Report 0 in continuous mode, only displayed if > to 0
                out += "\nTimeDiff ms = "  + diff + "\n";

            mResults += out + "\n";
            et_results.setText(mResults);

            return true;
        }
        return false;
    }

    private String showSpecialChars(String data)
    {
        String returnString="";
        char[] dataChar = data.toCharArray();
        for(char acar : dataChar)
        {
            if(Character.isLetterOrDigit(acar))
            {
                returnString += acar;
            }
            else
            {
                returnString += "["+(int)acar+"]";
            }
        }

        return returnString;
    }

    private void sendDataWedgeIntentWithExtra(String action, String extraKey, String extraValue)
    {
        Intent dwIntent = new Intent();
        dwIntent.setAction(action);
        dwIntent.putExtra(extraKey, extraValue);
        this.sendBroadcast(dwIntent);
    }

    private void sendDataWedgeIntentWithExtra(String action, String extraKey, Bundle extras)
    {
        Intent dwIntent = new Intent();
        dwIntent.setAction(action);
        dwIntent.putExtra(extraKey, extras);
        this.sendBroadcast(dwIntent);
    }

    private void importProfile(String progileFilenameWithoutDbExtension)
    {
        // Source : http://techdocs.zebra.com/datawedge/6-7/guide/settings/
        //Export your profile using
        //1. Open DataWedge
        //2. Open Hamburger Menu -> Settings (Paramètres)
        //3. Open "Export" list entry
        //4. Select profile to export
        //5. Retrieve exportes file in folder "\sdcard\Android\data\com.symbol.datawedge\files"

        // Open the db as the input stream
        InputStream fis = null;
        FileOutputStream fos = null;
        File outputFile = null;
        File finalFile = null;

        try {

            String autoImportDir = "/enterprise/device/settings/datawedge/autoimport/";
            String temporaryFileName = progileFilenameWithoutDbExtension + ".tmp";
            String finalFileName = progileFilenameWithoutDbExtension + ".db";


                fis = getAssets().open(finalFileName);


            // create a File object for the parent directory
            File outputDirectory = new File(autoImportDir);

            // create a temporary File object for the output file
            outputFile = new File(outputDirectory,temporaryFileName);
            finalFile = new File(outputDirectory, finalFileName);

            // attach the OutputStream to the file object
            fos = new FileOutputStream(outputFile);

            // transfer bytes from the input file to the output file
            byte[] buffer = new byte[1024];
            int length;
            int tot = 0;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
                tot+= length;
            }
            Log.d(TAG,tot+" bytes copied");

            //flush the buffers
            fos.flush();

            //release resources
            fos.close();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            fos = null;
            //set permission to the file to read, write and exec.
            if(outputFile != null)
            {
                outputFile.setExecutable(true, false);
                outputFile.setReadable(true, false);
                outputFile.setWritable(true, false);
                //rename the file
                if(finalFile != null)
                    outputFile.renameTo(finalFile);
            }

        }
    }

    private void createProfile()
    {
        sendDataWedgeIntentWithExtra(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2, DataWedgeConstants.EXTRA_CREATE_PROFILE, mProfileName);

        //  Now configure that created profile to apply to our application
        Bundle profileConfig = new Bundle();
        profileConfig.putString("PROFILE_NAME", mProfileName);
        profileConfig.putString("PROFILE_ENABLED", "true"); //  Seems these are all strings
        profileConfig.putString("CONFIG_MODE", "UPDATE");
        Bundle barcodeConfig = new Bundle();
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE");
        barcodeConfig.putString("RESET_CONFIG", "true"); //  This is the default but never hurts to specify
        Bundle barcodeProps = new Bundle();
        barcodeProps.putString("aim_mode", "on");
        barcodeProps.putString("lcd_mode", "3");

        // Mode rafale => aim type "continuous read", pas de beam timer
        barcodeProps.putString("aim_type", "5");

        barcodeConfig.putBundle("PARAM_LIST", barcodeProps);
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig);
        Bundle appConfig = new Bundle();
        appConfig.putString("PACKAGE_NAME", getPackageName());      //  Associate the profile with this app
        appConfig.putStringArray("ACTIVITY_LIST", new String[]{"*"});
        profileConfig.putParcelableArray("APP_LIST", new Bundle[]{appConfig});
        sendDataWedgeIntentWithExtra(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2, DataWedgeConstants.EXTRA_SET_CONFIG, profileConfig);
        //  You can only configure one plugin at a time, we have done the barcode input, now do the intent output
        profileConfig.remove("PLUGIN_CONFIG");
        Bundle intentConfig = new Bundle();
        intentConfig.putString("PLUGIN_NAME", "INTENT");
        intentConfig.putString("RESET_CONFIG", "true");
        Bundle intentProps = new Bundle();
        intentProps.putString("intent_output_enabled", "true");
        intentProps.putString("intent_action", mIntentAction);
        intentProps.putString("intent_category", mIntentCategory);
        intentProps.putString("intent_delivery", "2");
        intentConfig.putBundle("PARAM_LIST", intentProps);
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig);
        sendDataWedgeIntentWithExtra(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2, DataWedgeConstants.EXTRA_SET_CONFIG, profileConfig);
    }

    private void switchScannerParams()
    {
        mContinuous = !mContinuous;
        Bundle barcodeProps = new Bundle();
        if(mContinuous)
        {
            barcodeProps.putString("aim_type", "5");
            barcodeProps.putString("beam_timer", "0");
        }
        else
        {
            barcodeProps.putString("aim_type", "0");
            barcodeProps.putString("beam_timer", "5000");
        }
        mResults = "";
        mResults += (mContinuous ? "Switched to Continuous mode" : "Switched to normal mode") + "\n";
        et_results.setText(mResults);
        sendDataWedgeIntentWithExtra(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2, DataWedgeConstants.EXTRA_SWITCH_SCANNER_PARAMS, barcodeProps);

    }

}
