package com.askcs.p2000app.app;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import com.askcs.p2000app.R;
import com.askcs.p2000app.callbacks.onEveServiceReady;
import com.askcs.p2000app.service.EveService;

/**
 * Created by Jordi on 24-2-14.
 */
public class BaseActivity extends ActionBarActivity {

  public static String BASETAG = BaseActivity.class.getCanonicalName();

  // Reference to self
  private BaseActivity self = this;

  /* Service connection */
  public EveService eveService;
  private ServiceConnection mConnection;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Hide string title from actionbar
    getSupportActionBar().setDisplayShowTitleEnabled(false);

  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    menu.clear();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
      break;
      case R.id.action_setting_general:

        startActivity(new Intent(self, GeneralSettingsActivity.class));

      break;
      case R.id.action_setting_capcodes:

        startActivity(new Intent(self, CapcodeActivity.class));

      break;
      case R.id.action_setting_alarms:

        startActivity(new Intent(self, AlarmActivity.class));

        break;
      case R.id.action_logout:

        // Logout via the service
        if (eveService != null) {
          eveService.logout();
        }

       break;
    }
    return  true;
  }

  @Override
  public void onResume() {
    super.onResume();

  }

  @Override
  public void onPause(){
    super.onPause();
  }

  @Override
  public void onDestroy() {
    unbindService();

    super.onDestroy();
  }


  /* Service binding */
  private void bindService(){

    mConnection = new ServiceConnection() {

      public void onServiceConnected(ComponentName className, IBinder binder) {
        EveService.EveServiceBinder b = (EveService.EveServiceBinder) binder;
        eveService = b.getService();
        // Toast.makeText(BaseActivity.this, "Connected", Toast.LENGTH_SHORT).show();

        Log.e(BASETAG, "[onServiceConnected] Binding successful, service set");

        // Stop the loading screen
        onStopServiceRequest();

        // Trigger callback with eveservice as parameter
        self.oers.onEveServiceReady(eveService);
      }

      public void onServiceDisconnected(ComponentName className) {
        Log.e(BASETAG, "[onServiceDisconnected] Binding stopped, service reset");
        eveService = null;
      }
    };

    // Service binding
    Log.e(BASETAG, "[bindService] Binding the EveService to this activity");
    Intent intent = new Intent(this, EveService.class);
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

  }

  /* Service unbinding */
  private void unbindService(){

    if(mConnection != null){
      // Unbind the service
      Log.e(BASETAG, "[unbindService] Unbinding the EveService from this activity");
      unbindService(mConnection);
    }

    mConnection = null;

  }

  /* Callback for activities to implement */
  protected void onEveServiceReady(EveService es){}

  /* EveService request */
  private onEveServiceReady oers = null;
  public void requestService(onEveServiceReady callback){

    // Start loading screen
    onStartServiceRequest();

    // Set this as our callback for when the service is bound
    oers = callback;

    if(eveService!=null){
      // Stop the loading screen
      hideLoadingScreen();

      // If we already have the service ready, trigger the callback directly with eveservice as parameter
      oers.onEveServiceReady(eveService);

    } else {
      // Otherwise start the binding process (which will trigger the callback later)
      bindService();
    }

  }

  /* Loading screen types [may be used for using different loading strings with description texts] */
  public static final int LOADING_TYPE_SERVICE_REQUEST = 1;
  public static final int LOADING_TYPE_MESSAGES = 2;
  public static final int LOADING_TYPE_SUBSCRIPTIONS = 3;
  public static final int LOADING_TYPE_ADD_CAPCODE = 4;
  public static final int LOADING_TYPE_REMOVE_CAPCODE = 5;
  public static final int LOADING_TYPE_ALARMS = 6;
  public static final int LOADING_TYPE_DELETE_ALARM = 7;
  public static final int LOADING_TYPE_ADD_ALARM = 8;
  public static final int LOADING_TYPE_LINK_ALARM_CAPCODE = 9;

  private Dialog dialog = null;
  private int activeLoadingScreens = 0;
  public void startLoadingScreen(int type){
    activeLoadingScreens++;

    Log.w(BASETAG, "LoadingScreen startLoadingScreen: " + type);

    String loadingMessage = getResources().getString(R.string.loading_dialog);

    switch (type){
      case LOADING_TYPE_SERVICE_REQUEST:
        // Type specific loading message
        //loadingMessage = getResources().getString(R.string.loading_dialog_something_more_specific);
        break;
      case LOADING_TYPE_MESSAGES:
        loadingMessage = getResources().getString(R.string.loading_dialog_p2000_messages);
        break;
      case LOADING_TYPE_SUBSCRIPTIONS:
        loadingMessage = getResources().getString(R.string.loading_dialog_p2000_subscriptions);
        break;
      case LOADING_TYPE_ADD_CAPCODE:
        loadingMessage = getResources().getString(R.string.loading_dialog_p2000_add_subscription);
        break;
      case LOADING_TYPE_REMOVE_CAPCODE:
        loadingMessage = getResources().getString(R.string.loading_dialog_p2000_remove_subscription);
        break;
      case LOADING_TYPE_ALARMS:
        loadingMessage = getResources().getString(R.string.loading_dialog_alarms);
        break;
      case LOADING_TYPE_DELETE_ALARM:
        loadingMessage = getResources().getString(R.string.loading_dialog_delete_alarm);
        break;
      case LOADING_TYPE_ADD_ALARM:
        loadingMessage = getResources().getString(R.string.loading_dialog_add_alarm);
        break;
      case LOADING_TYPE_LINK_ALARM_CAPCODE:
        loadingMessage = getResources().getString(R.string.loading_dialog_link_alarm_capcode);
        break;
    }

    // If we currently only have this dialog started, create it. Otherwise re-use existing dialog and set new loading text
    if(activeLoadingScreens == 1){
      dialog = new Dialog(this);
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
      dialog.setContentView(R.layout.dialog_loading_overlay);
      dialog.setCanceledOnTouchOutside(false);
    }

    // Set the loadingMessage in the overlay layout
    TextView tv = (TextView) dialog.findViewById(R.id.loading_message);
    tv.setText(loadingMessage);

    // Only run this code if the activity is currently not finishing
    if(!((BaseActivity) this).isFinishing()){
      dialog.show();
    }
  }

  private static final boolean FORCE_LOADING_OVERLAY_DELAY = false; // NOTE: May cause issues; not recommended.
  public void hideLoadingScreen(){


    // NOTE: Even though this message will be kept, it does display another ('old') loading message in the layout
    // NOTE: When adding a new loading screen it will update the message to the most recent; this method doesn't
    // 'revert' this and assumes loading screens are called right after each other in a waterfall order.

    // Don't hide it if other loading screens are still active
    if(activeLoadingScreens > 1){
      activeLoadingScreens--;
      return;
    }

    Log.w(BASETAG, "LoadingScreen hideLoadingScreen");
    // If enabled (hardcoded) it will shown the loading dialog for 1 more second
    // This can be usefull if the flickering of the loading will get annoying (user unable to see what the flash was)
    // Most of the time this service binding is so fast that the user won't even see the layout.

    if(FORCE_LOADING_OVERLAY_DELAY){

      Handler h = new Handler();
      h.postDelayed(new Runnable() {
        public void run(){

          if(dialog != null){
            Log.w(BASETAG, "LoadingScreen Dialog dismissed");
            dialog.dismiss();
            //dialog = null;
          } else {
            Log.w(BASETAG, "LoadingScreen Dialog is null, not dismissed");
          }

        }
      }, 1000);

    } else {

      Log.w(BASETAG, "LoadingScreen Dialog dismissed");
      if(dialog != null){
        dialog.dismiss();
      }
      //dialog = null;

    }

    activeLoadingScreens--;

  }

  /* EveService binding specific loading screen implementations */
  protected void onStartServiceRequest(){
    startLoadingScreen(LOADING_TYPE_SERVICE_REQUEST);
  }

  protected void onStopServiceRequest(){
    hideLoadingScreen();
  }

}