package com.askcs.p2000app.util;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.squareup.otto.Bus;

/**
 * Maintains a singleton instance for obtaining the event bus over
 * which messages are passed from UI components (such as Activities
 * and Fragments) to Services, and back.
 */
public final class BusProvider {

  static private final String TAG = BusProvider.class.getCanonicalName();

  // The singleton of the Bus instance which can be used from
  // any thread in the app.
  private static final Bus gBus = new Bus() {


    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    public void register(Object subscriber) {
      Log.d(TAG, "register: " + subscriber);
      super.register( subscriber );
    }

    @Override
    public void unregister(Object subscriber) {
      Log.d( TAG, "unregister: " + subscriber);
      super.unregister( subscriber );
    }

    @Override
    public void post( final Object event ) {

      Log.d( TAG, "post: " + event );
      if ( event == null ) {
        throw new NullPointerException();
      }

      if ( Looper.myLooper() == Looper.getMainLooper() ) {
        super.post( event );
      } else {
        uiHandler.post( new Runnable() {
          @Override
          public void run() {
            post( event );
          }
        } );
      }
    }
  };

  /**
   * Returns a singleton instance for obtaining the event bus over
   * which messages are passed from UI components to Services, and
   * back.
   *
   * @return a singleton instance for obtaining the event bus over
   * which messages are passed from UI components to Services, and
   * back.
   */
  public static Bus getBus() {
    return gBus;
  }

  // No need to instantiate this class.
  private BusProvider() {
  }
}
