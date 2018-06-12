package com.symbol.datacapturereceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.zebra.datawedgeprofileintents.*;

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
    private static String mDemoProfileName = "com.symbol.datacapturereceiver";
    private static String mDemoIntentAction = "com.symbol.datacapturereceiver.RECVR";
    private static String mDemoIntentCategory = "android.intent.category.DEFAULT";
    private static long mDemoTimeOutMS = 30000; //30s timeout...
    private static boolean mStartInContinuousMode = false;
    private static boolean mOptmizeRefresh = true;
    private static boolean mDisplaySpecialChars = true;


    private TextView et_results;
    private ScrollView sv_results;
    private String mResults = "";
    private boolean mContinuousModeSwitch = false;
    private Date mScanDate = null;
    private Date mProfileProcessingStartDate = null;

    /*
    Scanner status checker
     */
    DWStatusScanner mScannerStatusChecker = null;

    /*
        Handler and runnable to scroll down textview
     */
    private Handler mScrollDownHandler = null;
    private Runnable mScrollDownRunnable = null;

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

        et_results = (TextView)findViewById(R.id.et_results);
        sv_results = (ScrollView)findViewById(R.id.sv_results);

        Button btEnableDW = (Button) findViewById(R.id.button_enabledw);
        btEnableDW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {  datawedgeEnableWithErrorChecking();  }
        });

        Button btDisableDW = (Button) findViewById(R.id.button_disabledw);
        btDisableDW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { datawedgeDisableWithErrorChecking(); }
        });

        Button btStart = (Button) findViewById(R.id.button_start);
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { mScanDate  = new Date(); startScanWithErrorChecking(); }
        });

        Button btStop = (Button) findViewById(R.id.button_stop);
        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScanWithErrorChecking();
            }
        });

        Button btToggle = (Button) findViewById(R.id.button_toggle);
        btToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScanWithErrorChecking();
            }
        });

        Button btEnable = (Button) findViewById(R.id.button_enable);
        btEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pluginEnableWithErrorChecking();
            }
        });

        Button btDisable = (Button) findViewById(R.id.button_disable);
        btDisable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pluginDisableWithErrorChecking();
            }
        });

        Button btCreate = (Button) findViewById(R.id.button_create);
        btCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupProfileAsync();
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

        Button btDelete = (Button) findViewById(R.id.button_delete);
        btDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteProfileAsync();
            }
        });

        Button btSwitch = (Button) findViewById(R.id.button_switch);
        btSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mContinuousModeSwitch = !mContinuousModeSwitch;
                switchScannerParamsAsync(mContinuousModeSwitch);
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
        myFilter.addAction(mDemoIntentAction);
        myFilter.addCategory(mDemoIntentCategory);
        this.getApplicationContext().registerReceiver(mMessageReceiver, myFilter);
        mScrollDownHandler = new Handler(Looper.getMainLooper());
        setupScannerStatusChecker();
    }

    @Override
    protected void onPause() {
        // Unregister internal broadcast receiver when we are going in background
        this.getApplicationContext().unregisterReceiver(mMessageReceiver);
        if(mScrollDownRunnable != null)
        {
            mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            mScrollDownRunnable = null;
            mScrollDownHandler = null;
        }
        stopScannerStatusChecker();
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
        if ( i.getAction().contentEquals(mDemoIntentAction) ) {

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
                out = out + (mDisplaySpecialChars ? showSpecialChars(data) : data);
            }

            if(diff > 0) // Report 0 in continuous mode, only displayed if > to 0
            {
                out += "\nSoftware scan took " + diff + "ms";
                mScanDate = null;
            }


            addLineToResults(out);

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

    private void startScanWithErrorChecking()
    {
        addLineToResults("Start Software Scan");
        DWScannerStartScan dwstartscan = new DWScannerStartScan(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwstartscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Start Scan on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Starting Scanner on profile: " + profileName + "\n" + resultInfo);
                }
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to start Scanner on profile: " + profileName);
            }
        });
    }

    private void stopScanWithErrorChecking()
    {
        addLineToResults("Stop Software Scan");
        DWScannerStopScan dwstopscan = new DWScannerStopScan(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwstopscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Stop Scan on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Stoping Scanner on profile: " + profileName + "\n" + resultInfo);
                }
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to stop scanner on profile: " + profileName);
            }
        });
    }

    private void toggleScanWithErrorChecking()
    {
        addLineToResults("Toggle Software Scan");
        DWScannerToggleScan dwtogglescan = new DWScannerToggleScan(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwtogglescan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Toggle Scan on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Toggleing Scanner on profile: " + profileName + "\n" + resultInfo);
                }
            }
            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to toggle scanner on profile: " + profileName);
            }
        });
    }

    private void pluginEnableWithErrorChecking()
    {
        addLineToResults("Enabling DataWedge Plugin");
        DWScannerPluginEnable dwpluginenable = new DWScannerPluginEnable(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwpluginenable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Enabling plugin on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Enabline plugin on profile: " + profileName + "\n" + resultInfo);
                }
            }
            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to enable plugin on profile: " + profileName);
            }
        });
    }
    
    private void pluginDisableWithErrorChecking()
    {
        addLineToResults("Disabling DataWedge Plugin");
        DWScannerPluginDisable dwplugindisable = new DWScannerPluginDisable(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwplugindisable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Disabling plugin on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Disabling plugin on profile: " + profileName + "\n" + resultInfo);
                }
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to disable plugin on profile: " + profileName);
            }
        });
    }

    private void datawedgeEnableWithErrorChecking()
    {
        addLineToResults("Enabling DataWedge Plugin");
        DWDataWedgeEnable dwdatawedgeenable = new DWDataWedgeEnable(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwdatawedgeenable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Enabling datawedge on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Enabline datawedge on profile: " + profileName + "\n" + resultInfo);
                }
            }
            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to enable datawedge on profile: " + profileName);
            }
        });
    }

    private void datawedgeDisableWithErrorChecking()
    {
        addLineToResults("Disabling DataWedge Plugin");
        DWDataWedgeDisable dwdatawedgedisable = new DWDataWedgeDisable(MainActivity.this);
        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
        }};

        dwdatawedgedisable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    // We will never see this one.... only errors are reported, no success callback
                    addLineToResults("Disabling datawedge on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error Disabling datawedge on profile: " + profileName + "\n" + resultInfo);
                }
            }
            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to disable datawedge on profile: " + profileName);
            }
        });
    }
    
    private void switchScannerParamsAsync(final boolean continuousMode)
    {
        addLineToResults(continuousMode ? "Switching to Continuous mode" : "Switching to normal mode");
        if(mProfileProcessingStartDate == null)
            mProfileProcessingStartDate = new Date();
        DWProfileSwitchBarcodeParams switchContinuousMode = new DWProfileSwitchBarcodeParams(MainActivity.this);
        DWProfileSwitchBarcodeParamsSettings settings = new DWProfileSwitchBarcodeParamsSettings()
        {{
            mProfileName = MainActivity.this.mDemoProfileName;
            mTimeOutMS = MainActivity.this.mDemoTimeOutMS;
            mAggressiveContinuousMode = continuousMode;
        }};

        switchContinuousMode.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    addLineToResults("Params switched to " + (continuousMode ? "continuous mode" : "normal mode") + " on profile: " + profileName + " succeeded");
                }
                else
                {
                    addLineToResults("Error switching params to " + (continuousMode ? "continuous mode" : "normal mode") + " on profile: " + profileName + "\n" + resultInfo);
                }
                addTotalTimeToResults();
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to switching params on profile: " + profileName);
            }
        });
    }

    private void deleteProfileAsync()
    {
        //sendDataWedgeIntentWithExtra(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2, DataWedgeConstants.EXTRA_DELETE_PROFILE, mDemoProfileName);
        addLineToResults("Deleting profile " + MainActivity.mDemoProfileName);

        mProfileProcessingStartDate = new Date();
        /*
        The profile delete will delete a profile if it exists
         */
        DWProfileDelete deleteProfile = new DWProfileDelete(this);

        // Setup profile checker parameters
        DWProfileDeleteSettings profileDeleteSettings = new DWProfileDeleteSettings()
        {{
            mProfileName = MainActivity.mDemoProfileName;
            mTimeOutMS = MainActivity.mDemoTimeOutMS;
        }};

        deleteProfile.execute(profileDeleteSettings, new DWProfileDelete.onProfileCommandResult(){
                @Override
                public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                    if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                    {
                        addLineToResults("Profile: " + profileName + " delete succeeded");
                    }
                    else
                    {
                        addLineToResults("Error while trying to delete profile: " + profileName + "\n" + resultInfo);
                    }
                    addTotalTimeToResults();
                }


                @Override
                public void timeout(String profileName) {
                    addLineToResults("Timeout while trying to delete profile: " + profileName);
                }
            }
        );
    }

    private void createProfileAsync()
    {
        DWProfileCreate profileCreate = new DWProfileCreate(MainActivity.this);

        DWProfileCreateSettings profileCreateSettings = new DWProfileCreateSettings()
        {{
            mProfileName = MainActivity.mDemoProfileName;
            mTimeOutMS = MainActivity.mDemoTimeOutMS;
        }};

        profileCreate.execute(profileCreateSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    addLineToResults("Profile: " + profileName + " created with success.\nSetting config now.");
                    setProfileConfigAsync();
                }
                else
                {
                    addLineToResults("Error creating profile: " + profileName + "\n" + resultInfo);
                    addTotalTimeToResults();
                }
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to create profile: " + profileName);
            }
        });
    }

    private void setupScannerStatusChecker()
    {
        DWStatusScannerSettings profileStatusSettings = new DWStatusScannerSettings()
        {{
            mPackageName = getPackageName();
            mScannerCallback = new DWStatusScannerCallback() {
                @Override
                public void result(String status) {
                    switch(status)
                    {
                        case DataWedgeConstants.SCAN_STATUS_CONNECTED:
                            addLineToResults("Scanner is connected.");
                            break;
                        case DataWedgeConstants.SCAN_STATUS_DISABLED:
                            addLineToResults("Scanner is disabled.");
                            break;
                        case DataWedgeConstants.SCAN_STATUS_DISCONNECTED:
                            addLineToResults("Scanner is disconnected.");
                            break;
                        case DataWedgeConstants.SCAN_STATUS_SCANNING:
                            addLineToResults("Scanner is scanning.");
                            break;
                        case DataWedgeConstants.SCAN_STATUS_WAITING:
                            addLineToResults("Scanner is waiting.");
                            break;
                    }
                }
            };
        }};

        addLineToResults("Setting up scanner status checking on package : " + profileStatusSettings.mPackageName + ".");

        mScannerStatusChecker = new DWStatusScanner(MainActivity.this, profileStatusSettings);
        mScannerStatusChecker.start();
    }

    private void stopScannerStatusChecker()
    {
        mScannerStatusChecker.stop();
    }

    private void setProfileConfigAsync()
    {
        DWProfileSetConfig profileSetConfig = new DWProfileSetConfig(MainActivity.this);
        DWProfileSetConfigSettings setConfigSettings = new DWProfileSetConfigSettings()
        {{
            mProfileName = MainActivity.mDemoProfileName;
            mTimeOutMS = MainActivity.mDemoTimeOutMS;
            mAPPL_PackageName = getPackageName();
            mINT_IntentAction = MainActivity.mDemoIntentAction;
            mINT_IntentCategory = MainActivity.mDemoIntentCategory;
        }};
        profileSetConfig.execute(setConfigSettings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    addLineToResults("Set config on profile: " + profileName + " succeeded.");

                }
                else
                {
                    addLineToResults("Error setting params on profile: " + profileName + "\n" + resultInfo);
                }
                addTotalTimeToResults();
            }

            @Override
            public void timeout(String profileName) {
                addLineToResults("Timeout while trying to set params on profile: " + profileName);
            }
        });
    }

    private void setupProfileAsync()
    {
        mProfileProcessingStartDate = new Date();
        /*
        The profile checker will check if the profile already exists
         */
        DWProfileChecker checker = new DWProfileChecker(this);

        // Setup profile checker parameters
        DWProfileCheckerSettings profileCheckerSettings = new DWProfileCheckerSettings()
        {{
            mProfileName = MainActivity.mDemoProfileName;
            mTimeOutMS = MainActivity.mDemoTimeOutMS;
        }};

        // Execute the checker with the given parameters
        checker.execute(profileCheckerSettings, new DWProfileChecker.onProfileExistResult() {
            @Override
            public void result(String profileName, boolean exists) {
                // empty error... means... I let you guess....
                    // exists == true means that the profile already... exists..
                    if(exists)
                    {
                        addLineToResults("Profile " + profileName + " found in DW profiles list.\n Resetting profile to" + (mStartInContinuousMode ? " " :" not ") + "continuous mode.");
                        switchScannerParamsAsync(mStartInContinuousMode);
                    }
                    else
                    {
                        addLineToResults("Profile " + profileName + " not found in DW profiles list. Creating profile.");
                        createProfileAsync();
                    }
            }

            @Override
            public void timeOut(String profileName) {
                addLineToResults("Timeout while trying to check if profile " + profileName + " exists.");

            }
        });
    }

    private void addTotalTimeToResults()
    {
        Date current = new Date();
        long timeDiff = current.getTime() - mProfileProcessingStartDate.getTime();
        addLineToResults("Total time: " + timeDiff + "ms");
        mProfileProcessingStartDate = null;
    }

    private void addLineToResults(final String lineToAdd)
    {
        mResults += lineToAdd + "\n";
        updateAndScrollDownTextView();
    }

    private void updateAndScrollDownTextView()
    {
        if(mOptmizeRefresh)
        {
            if(mScrollDownRunnable == null)
            {
                mScrollDownRunnable = new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                et_results.setText(mResults);
                                sv_results.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        sv_results.fullScroll(ScrollView.FOCUS_DOWN);
                                    }
                                });
                            }
                        });
                    }
                };
            }
            else
            {
                // A new line has been added while we were waiting to scroll down
                // reset handler to repost it....
                mScrollDownHandler.removeCallbacks(mScrollDownRunnable);
            }
            mScrollDownHandler.postDelayed(mScrollDownRunnable, 300);
        }
        else
        {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    et_results.setText(mResults);
                    sv_results.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }

    }
}
