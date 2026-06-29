package com.Saalai.SalaiMusicApp.Receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {

    public static final String ACTION_INTERNET_STATUS = "com.Saalai.SalaiMusicApp.INTERNET_STATUS";
    public static final String EXTRA_IS_CONNECTED = "is_connected";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = isNetworkAvailable(context);

            // Broadcast the internet status to all activities/fragments
            Intent internetStatusIntent = new Intent(ACTION_INTERNET_STATUS);
            internetStatusIntent.putExtra(EXTRA_IS_CONNECTED, isConnected);
            context.sendBroadcast(internetStatusIntent);

            Log.d("NetworkChangeReceiver", "Internet connected: " + isConnected);
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        }
        return false;
    }
}