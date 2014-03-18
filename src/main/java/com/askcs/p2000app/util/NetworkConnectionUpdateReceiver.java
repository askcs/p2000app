package com.askcs.p2000app.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import com.squareup.otto.Bus;

/**
 * Created with IntelliJ IDEA.
 * User: Jordi
 * Date: 21-10-13
 * Time: 11:37
 * To change this template use File | Settings | File Templates.
 */
public class NetworkConnectionUpdateReceiver extends BroadcastReceiver {


  @Override
  public void onReceive(Context context, Intent intent) {

    Bus bus = BusProvider.getBus();
    boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

    if (noConnectivity){
      Log.w("NET", "Not connected: (no connection = " + noConnectivity + ")");
    } else {
      Log.w("NET", "Connected: (no connection = " + noConnectivity + ")");
    }

    // Send event
    bus.post( new NetworkConnectionUpdateEvent( noConnectivity ) );
  }


  /**
   * Otto event, represents a Connection Update Change.
   * Sender: NetworkConnectionUpdateReceiver
   * Receiver: Unknown, multiple
   */
  public static class NetworkConnectionUpdateEvent {
    private boolean connectionStatus;

    public NetworkConnectionUpdateEvent( boolean connectionStatus ) {
      // Note: Reverse the status since the original value is based on 'no connection = true', 'connected = false'
      // (When reverted it can be used as: if(isConnected) with this value in the isConnected var)
      connectionStatus = !connectionStatus;
    }

    public boolean getConnectionStatus() {
      return connectionStatus;
    }
  }

}
