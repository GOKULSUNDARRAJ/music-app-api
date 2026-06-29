package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

public class NetworkMonitor {
    private static final String TAG = "NetworkMonitor";
    private Context context;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkAvailable = true;
    private NetworkListener listener;

    public interface NetworkListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    public NetworkMonitor(Context context) {
        this.context = context.getApplicationContext();
        this.connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void startMonitoring(NetworkListener listener) {
        this.listener = listener;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(createNetworkCallback());
        } else {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, createNetworkCallback());
        }

        checkCurrentNetwork();
    }

    private ConnectivityManager.NetworkCallback createNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                isNetworkAvailable = true;
                Log.d(TAG, "Network available");
                if (listener != null) {
                    listener.onNetworkAvailable();
                }
            }

            @Override
            public void onLost(Network network) {
                isNetworkAvailable = false;
                Log.w(TAG, "Network lost");
                if (listener != null) {
                    listener.onNetworkLost();
                }
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                if (networkCapabilities != null) {
                    boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                    boolean isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                    Log.d(TAG, "Network capabilities changed - Internet: " + hasInternet + ", Validated: " + isValidated);
                }
            }
        };
        return networkCallback;
    }

    private void checkCurrentNetwork() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                isNetworkAvailable = capabilities != null &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                isNetworkAvailable = false;
            }
        } else {
            // For older versions, use traditional method
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            isNetworkAvailable = activeNetwork != null && activeNetwork.isConnected();
        }
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public boolean isWifiConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
        } else {
            android.net.NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.isConnected() &&
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }

    public void stopMonitoring() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
            }
        } else {
            android.net.NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null &&
                    networkInfo.isConnected() &&
                    networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
        }
        return false;
    }
}