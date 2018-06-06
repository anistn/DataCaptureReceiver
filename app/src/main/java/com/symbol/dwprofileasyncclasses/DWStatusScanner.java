package com.symbol.dwprofileasyncclasses;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class DWStatusScanner {
    private Context mContext;
    private dataWedgeScannerStatusReceiver mStatusBroadcastReceiver = null;
    private DWStatusScannerSettings mStatusSettings = null;

    public DWStatusScanner(Context aContext, DWStatusScannerSettings settings) {
        mContext = aContext;
        mStatusSettings = settings;
        mStatusBroadcastReceiver = new dataWedgeScannerStatusReceiver();
    }

    public void start()
    {
        /*
        Register notification broadcast receiver
         */
        registerNotificationReceiver();

        /*
        Register for status callcack
         */
        registerForScannerStatus(mStatusSettings);
    }

    public void stop()
    {
        unRegisterNotificationReceiver();
        unRegisterForScannerStatus(mStatusSettings);
    }

    protected class dataWedgeScannerStatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(DataWedgeConstants.NOTIFICATION_ACTION)){
                // handle notification
                if(intent.hasExtra(DataWedgeConstants.EXTRA_RESULT_NOTIFICATION)) {
                    Bundle b = intent.getBundleExtra(DataWedgeConstants.EXTRA_RESULT_NOTIFICATION);
                    String NOTIFICATION_TYPE  = b.getString(DataWedgeConstants.EXTRA_RESULT_NOTIFICATION_TYPE);
                    if(NOTIFICATION_TYPE!= null) {
                        switch (NOTIFICATION_TYPE) {
                            case DataWedgeConstants.NOTIFICATION_TYPE_SCANNER_STATUS:
                                String status = b.getString(DataWedgeConstants.EXTRA_KEY_VALUE_NOTIFICATION_STATUS);
                                if(status!=null){
                                    mStatusSettings.mScannerCallback.result(status);
                                }
                                break;
                        }
                    }
                }
            }
        }
    };

    public void registerForScannerStatus(DWStatusScannerSettings settings) {
        Bundle b = new Bundle();
        b.putString(DataWedgeConstants.EXTRA_KEY_APPLICATION_NAME, settings.mPackageName.isEmpty() ? mContext.getPackageName() : settings.mPackageName);
        b.putString(DataWedgeConstants.EXTRA_KEY_NOTIFICATION_TYPE, DataWedgeConstants.NOTIFICATION_TYPE_SCANNER_STATUS);
        Intent i = new Intent();
        i.setAction(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2);
        i.putExtra(DataWedgeConstants.ACTION_EXTRA_REGISTER_FOR_NOTIFICATION, b);
        mContext.getApplicationContext().sendBroadcast(i);
    }

    public void unRegisterForScannerStatus(DWStatusScannerSettings settings) {
        Bundle b = new Bundle();
        b.putString(DataWedgeConstants.EXTRA_KEY_APPLICATION_NAME, settings.mPackageName.isEmpty() ? mContext.getPackageName() : settings.mPackageName);
        b.putString(DataWedgeConstants.EXTRA_KEY_NOTIFICATION_TYPE, DataWedgeConstants.NOTIFICATION_TYPE_SCANNER_STATUS);
        Intent i = new Intent();
        i.setAction(DataWedgeConstants.ACTION_DATAWEDGE_FROM_6_2);
        i.putExtra(DataWedgeConstants.ACTION_EXTRA_UNREGISTER_FOR_NOTIFICATION, b);
        mContext.getApplicationContext().sendBroadcast(i);
    }

    void registerNotificationReceiver() {
        //to register the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(DataWedgeConstants.NOTIFICATION_ACTION);
        mContext.getApplicationContext().registerReceiver(mStatusBroadcastReceiver, filter);//Android method
    }

    void unRegisterNotificationReceiver() {
        //to unregister the broadcast receiver
        mContext.getApplicationContext().unregisterReceiver(mStatusBroadcastReceiver); //Android method
    }
}
