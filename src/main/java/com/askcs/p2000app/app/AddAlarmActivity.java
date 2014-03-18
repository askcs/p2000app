package com.askcs.p2000app.app;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import com.askcs.p2000app.R;
import com.askcs.p2000app.callbacks.onEveServiceReady;
import com.askcs.p2000app.entities.AlarmSetup;
import com.askcs.p2000app.service.EveService;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

/**
 * Created by Jordi on 6-3-14.
 */
public class AddAlarmActivity extends BaseActivity {

  // Reference to self/this
  private AddAlarmActivity self = this;

  // Debug tag
  public static String TAG = AddAlarmActivity.class.getCanonicalName();

  // Submit button
  private Button submitButton = null;

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_add_alarm);

    Log.w(TAG, "[AddAlarmActivity] onCreate");

    // Request service binding
    requestService(new onEveServiceReady() {
      @Override
      public void onEveServiceReady(EveService es) {
        // Notify our local event listener - eveservice now available to use
        self.onEveServiceReady(es);
      }
    });

    // Clickhandler on save button
    submitButton = (Button) findViewById(R.id.btn_create_alarm_setup);

    submitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        startLoadingScreen(LOADING_TYPE_ADD_ALARM);
        // Collect all AlarmSetup data
        // Name
        EditText nameField = (EditText) findViewById(R.id.name);
        String name = nameField.getText().toString();

        // Filter word
        EditText filterField = (EditText) findViewById(R.id.filter_word);
        String filterWord = filterField.getText().toString();

        // Enable sound
        CheckBox cbSound = (CheckBox) findViewById(R.id.check_sound);
        boolean sound = cbSound.isChecked();

        // Enable notification
        CheckBox cbNotification = (CheckBox) findViewById(R.id.check_notification);
        boolean notification = cbNotification.isChecked();

        // Enable vibrate
        CheckBox cbVibrate = (CheckBox) findViewById(R.id.check_vibrate);
        boolean vibrate = cbVibrate.isChecked();

        // Enable LED
        CheckBox cbLED = (CheckBox) findViewById(R.id.check_led);
        boolean led = cbLED.isChecked();

        // Enable ScreenOn
        CheckBox cbScreen = (CheckBox) findViewById(R.id.check_screenon);
        boolean screen = cbScreen.isChecked();

        try {
          eveService.getMobileAgent().addAlarmSetup( new AlarmSetup(name, filterWord, sound, notification, vibrate, led, screen) );

          hideLoadingScreen();
          self.finish();

        } catch (Exception e) {
          e.printStackTrace();
          Crouton.showText(self, getResources().getString(R.string.error_no_result), Style.ALERT);
          hideLoadingScreen();
        }

      }
    });
  }

  // From here we can do whatever  we want with the EveService object directly
  @Override
  protected void onEveServiceReady(EveService es){
    super.onEveServiceReady(es);
    Log.w(TAG, "[AlarmActivity] onEveServiceReady");

    // Enable the submit button
    submitButton.setEnabled(true);

    // Hide spinner
    findViewById(R.id.loading_spinner_dialog).setVisibility(View.GONE);
  }

}