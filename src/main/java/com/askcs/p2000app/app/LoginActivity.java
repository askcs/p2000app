package com.askcs.p2000app.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.askcs.p2000app.R;
import com.askcs.p2000app.events.LoginStateChangeEvent;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.askcs.p2000app.util.Cryptography;
import com.squareup.otto.Subscribe;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LoginActivity extends BaseActivity {

  public static String TAG = LoginActivity.class.getCanonicalName();
  private static Context ctx;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Busprovider
    BusProvider.getBus().register(this);

    setContentView(R.layout.activity_login);

    // Copy context
    ctx = this;

    // Set last known username and password in the edittext field
    final EditText fieldUsername = (EditText) findViewById(R.id.input_username);
    final EditText fieldPassword = (EditText) findViewById(R.id.input_password);

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    fieldUsername.setText( prefs.getString( EveService.XMPP_USERNAME_KEY, "") );
    fieldPassword.setText( prefs.getString( EveService.XMPP_ORIGINAL_PASSWORD_KEY, "") ); // Not the MD5 hash, but the original password

    String test = prefs.getString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, "");
    Log.w(TAG, "[TEST] String 2: " + test);

    Button button = (Button) findViewById(R.id.btn_login);
    button.setOnClickListener(new View.OnClickListener() {

      public void onClick(View v) {

        System.err.println("[onClick] Login");

        String username = fieldUsername.getText().toString();
        String originalpassword = fieldPassword.getText().toString();
        String password = Cryptography.md5(originalpassword);

        if(username.equals("") || originalpassword.equals("")){
          Log.w(TAG, "Failed - No username and/or password given via the login form");
          Crouton.cancelAllCroutons();
          Crouton.showText((LoginActivity) ctx, "Vul een gebruikersnaam en wachtwoord in", Style.ALERT);
        } else {

          // Save the userdetails in the sharedprefs
          SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
          SharedPreferences.Editor spe = sp.edit();
          spe.putString(EveService.XMPP_USERNAME_KEY, username);
          spe.putString(EveService.XMPP_PASSWORD_KEY, password);
          spe.putString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, originalpassword);
          spe.commit();

          // Lets see if the LoaderActivity can log us in with these new details
          Intent loadingActivity = new Intent(ctx, LoaderActivity.class);
          loadingActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
          startActivity( loadingActivity );

          // NOTE: Important to finish. Otherwise this activity will be 'recycled' on logout and thus showing the password again
          // NOTE: Alternative: Empty the passwordfield in checkStartUpStuff() when the LOGIN_STATE__LOGOUT is triggered
          finish();

        }
      }
    });

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
  protected void onNewIntent(Intent intent) {

    System.err.println("Incoming Intent...");
    super.onNewIntent(intent);

    checkStartUpStuff(intent);
  }

  private void checkStartUpStuff(Intent intent) {

    System.out.println("checkStartUpStuff LoginActivity");

    // Incoming intent stuff
    Bundle extras = intent.getExtras();
    if(extras != null){
      // On incoming failed login
      if(extras.containsKey( EveService.LOGIN_STATE_FIELDNAME ) == true && extras.getString( EveService.LOGIN_STATE_FIELDNAME ).equals( EveService.LOGIN_STATE__FAILED)){
        Log.w(TAG, "[checkStartUpStuff] Extra " + EveService.LOGIN_STATE_FIELDNAME + " = " + EveService.LOGIN_STATE__FAILED + ".");
        loginFailed();
      }

      // On incoming logout
      if(extras.containsKey( EveService.LOGIN_STATE_FIELDNAME ) == true && extras.getString( EveService.LOGIN_STATE_FIELDNAME ).equals( EveService.LOGIN_STATE__LOGOUT)){
        Log.w(TAG, "[checkStartUpStuff] Extra " + EveService.LOGIN_STATE_FIELDNAME + " = " + EveService.LOGIN_STATE__LOGOUT + ".");
      }
    }

  }

  @Subscribe
  public void onLoginStateChange(LoginStateChangeEvent lsce){

    // Incoming message of failed login (from EveService)
    if(lsce.getState().equals( EveService.LOGIN_STATE__FAILED ) ||
            lsce.getState().equals( EveService.LOGIN_STATE__FAILED_RECONNECT) ||
            lsce.getState().equals( EveService.LOGIN_STATE__FAILED_INIT_AGENT)
            ){
      Log.w(TAG, "[onLoginStateChange] State: " + lsce.getState() );
      loginFailed();
    }

    // ...
  }

  /* Loginstate actions */
  private void loginFailed(){
    Log.w(TAG, "[Crouton] Login failed");
    Crouton.cancelAllCroutons();
    Crouton.showText(this, "Foutieve login (gebruikersnaam/wachtwoord), probeer het opnieuw", Style.ALERT); // TODO: Make string resource
  }

  @Override
  public void onResume() {
    super.onResume();

    checkStartUpStuff(getIntent());
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }
}


