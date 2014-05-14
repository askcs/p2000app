package com.askcs.p2000app.service;

import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import com.askcs.p2000app.util.BusProvider;

/**
 * Superclass of all StandBy services. At the moment there is only one,
 * MainService. If that stays the way it is, this superclass can be factored
 * out.
 */
public class BaseEveService extends Service {

  /**
   * Tag for logging
   */
  static private final String TAG = BaseEveService.class.getCanonicalName();


  /**
   * Must implement this, but we always return null.
   * @param intent
   * @return
   */
  @Override
  public IBinder onBind( Intent intent ) {
    return null;
  }

  /**
   * onCreate: register with Otto.
   */
  @Override
  public void onCreate() {
    Log.d( TAG, "onCreate" );
    super.onCreate();
    BusProvider.getBus().register( this );
  }

  /**
   * onCreate: unregister with Otto.
   */
  @Override
  public void onDestroy() {
    super.onDestroy();
    BusProvider.getBus().unregister( this );
  }
}
