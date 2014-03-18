package com.askcs.p2000app.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import com.askcs.p2000app.R;
import com.askcs.p2000app.events.LoginEvent;
import com.askcs.p2000app.events.LoginStateChangeEvent;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.squareup.otto.Subscribe;

/**
 * Created by Jordi on 25-2-14.
 */
public class LoaderActivity extends BaseActivity {

  public static String TAG = LoaderActivity.class.getCanonicalName();

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_loading);

    // Busprovider
    BusProvider.getBus().register(this);

    /** Attempt to login the user **/

    // Start EVE service
    Intent s = new Intent(this, EveService.class);
    startService(s);

  }

  /* START -- Activity specific menu adjustments */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    // Do not call the super on purpose; Login screen doesnt need the options to logout or goto settings
    return true;
  }
   /* END -- Activity specific menu adjustments */

  @Override
  public  void onNewIntent(Intent i){
    super.onNewIntent(i);
    Log.e(TAG, "[onNewIntent] Start attempt to login");
    attemptToLogin();
  }

  @Override
  public void onResume(){
    super.onResume();
    Log.e(TAG, "[onResume] Start attempt to login");
    attemptToLogin();
  }

  @Override
  public void onDestroy(){

    // Busprovider
    BusProvider.getBus().unregister(this);

    super.onDestroy();
  }

  private boolean loggingIn = false;
  private boolean isWorking = true;
  private void attemptToLogin(){

    // Is this method working on something?
    if(loggingIn){
      loggingIn = false;
      finish();
      return;
    } else {
      loggingIn = true;
    }

    // Not yet working on something (reset)
    isWorking = false;

    Log.e(TAG, "[attemptToLogin] Start");

    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
    String username =         sp.getString(EveService.XMPP_USERNAME_KEY, "");
    String password =         sp.getString(EveService.XMPP_PASSWORD_KEY, "");
    String originalpassword = sp.getString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, "");

    // Attempt to login with these credentials
    if(username.equals("") || password.equals("")){

      Log.e(TAG, "No username/password stored, redirect to login activity with state 'nocredentials'");
      startActivity(new Intent(this, LoginActivity.class).putExtra(EveService.LOGIN_STATE_FIELDNAME, EveService.LOGIN_STATE__NOCREDENTIALS));
      finish();

    } else {

      isWorking = true;

      try{

        // LoginEvent to EveService
        BusProvider.getBus().post(new LoginEvent(username, password, originalpassword));

        Log.e(TAG, "[attemptToLogin] Posted the LoginEvent");

      }catch (Exception e){

        Log.e(TAG, "Failed login via the EveService, redirect to login activity with state 'failed'");
        e.printStackTrace();

        startActivity(new Intent(this, LoginActivity.class).putExtra(EveService.LOGIN_STATE_FIELDNAME, EveService.LOGIN_STATE__FAILED));
        finish();
      }

      Log.e(TAG, "[attemptToLogin] Finished");

    }

    // Stop this activity if it's not doing anything (e.g. backbutton [in actionbar] is used while the user is not logged in]
    if(!isWorking){
      finish();
    }

    // TODO: Create a force logout button; stop the login process, make sure no Busprovider events are send later which causes a redirect to main anyway

  }

  @Subscribe
  public void onLoginStateChange(LoginStateChangeEvent lsce){
    Log.w(TAG, "[LoaderActivity] onLoginStateChange: " + lsce.getState().toString());

    // Incoming message of failed login (from EveService)
    if(lsce.getState().equals( EveService.LOGIN_STATE__SUCCESS) ){
      goToMain();
    }

    // TODO: Remove/Adjust/Check these. Currently you can receive a LOGIN_STATE_FAILED
    // Incoming message of failed login (from EveService)
    if(lsce.getState().equals( EveService.LOGIN_STATE__FAILED) ||
            lsce.getState().equals( EveService.LOGIN_STATE__FAILED_RECONNECT) ||
            lsce.getState().equals( EveService.LOGIN_STATE__FAILED_INIT_AGENT) ){
      gotToLogin();
    }

  }

  private void goToMain(){
    Log.w(TAG, "[LoaderActivity] Going to Main");

    Intent mainActivity = new Intent(this, MainActivity.class);
    mainActivity.putExtra( EveService.LOGIN_STATE_FIELDNAME,  EveService.LOGIN_STATE__SUCCESS);
    mainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Clear all other actities + don't run again (onCreate) if it's already running
    startActivity( mainActivity );

    finish();
  }

  private void gotToLogin(){

    Log.w(TAG, "[LoaderActivity] Going to Login");

    Intent loginActivity = new Intent(this, LoginActivity.class);
    loginActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Clear all other actities + don't run again (onCreate) if it's already running
    startActivity( loginActivity );

    finish();
  }

}