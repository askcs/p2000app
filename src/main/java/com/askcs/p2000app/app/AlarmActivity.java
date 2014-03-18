package com.askcs.p2000app.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.askcs.commons.entity.P2000Message;
import com.askcs.p2000app.R;
import com.askcs.p2000app.callbacks.onEveServiceReady;
import com.askcs.p2000app.entities.AlarmSetup;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Jordi on 6-3-14.
 */
public class AlarmActivity extends BaseActivity {

  // Reference to self/this
  private AlarmActivity self = this;

  // Debug tag
  public static String TAG = AlarmActivity.class.getCanonicalName();

  // Listview
  ListView listView = null;
  private GoogleCardsAdapter mGoogleCardsAdapter;
  SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = null;

  // Alarm Setups
  private ArrayList<AlarmSetup> alarmSetups = new ArrayList<AlarmSetup>();

  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.actvitiy_alarm);

    Log.w(TAG, "[AlarmActivity] onCreate");

    // Request service binding
    requestService(new onEveServiceReady() {
      @Override
      public void onEveServiceReady(EveService es) {
        // Notify our local event listener - eveservice now available to use
        self.onEveServiceReady(es);
      }
    });

    // Listview - specific for this application
    listView = (ListView) findViewById(R.id.lv_alarm_setups);

    // Placeholder for when the listview is empty
    TextView emptyText = (TextView)findViewById(R.id.lv_empty_text);
    listView.setEmptyView(emptyText);

    mGoogleCardsAdapter = new GoogleCardsAdapter(this);
    //SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mGoogleCardsAdapter, this));
    swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mGoogleCardsAdapter);
    swingBottomInAnimationAdapter.setInitialDelayMillis(300);
    swingBottomInAnimationAdapter.setAbsListView(listView);

    listView.setAdapter(swingBottomInAnimationAdapter);

    // Add alarm button
    Button btnAddAlarm = (Button) findViewById(R.id.btn_add_alarm);
    btnAddAlarm.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity( new Intent(self, AddAlarmActivity.class) );
      }
    });

  }

  @Override
  public void onResume(){
    super.onResume();

    Log.w(TAG, "[AlarmActivity] onResume");

    // Reload list of P2000 messages
    // Only run this if the EveService is not null (onEveService ready may not have been triggered yet)
    if(eveService != null){
      loadAlarmSetupsList();
    }

  }

  // From here we can do whatever  we want with the EveService object directly
  @Override
  protected void onEveServiceReady(EveService es){
    super.onEveServiceReady(es);
    Log.w(TAG, "[AlarmActivity] onEveServiceReady");
    loadAlarmSetupsList();
  }

  /* START -- Activity specific functionalities */

  // List length properties (UI display)
  private void loadAlarmSetupsList(){
    Log.w(TAG, "[AlarmActivity] loadAlarmSetupsList");

    startLoadingScreen(LOADING_TYPE_ALARMS);

    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          try {

            // TESTING
            //eveService.getMobileAgent().addAlarmSetup(new AlarmSetup("Test", true, true, true, true, true) );

            self.alarmSetups = eveService.getMobileAgent().getAlarmSetups();
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              self.buildAlarmSetupsList();
            }
          });

        }
      };

      // Start the thread with runnables
      new Thread(r).start();

    } catch (Exception e){
      Log.w(TAG, "[asyncCall] Could not get the current list of alarm setups from the local agent state");
      Crouton.showText(this, getResources().getString(R.string.error_no_result), Style.ALERT);
      e.printStackTrace();

      // Also hide the loading screen if something went wrong
      hideLoadingScreen();
    }

  }

  private void buildAlarmSetupsList(){

    // Clear all current items for when it's a reload of the list
    mGoogleCardsAdapter.clear();

    // Make the list animate on new incoming alarms
    swingBottomInAnimationAdapter.reset();

    // Add the p2000 messages list to the adapter
    mGoogleCardsAdapter.addAll( alarmSetups );
    mGoogleCardsAdapter.notifyDataSetChanged();

    // Remove the loading screen
    hideLoadingScreen();
  }

  private static class GoogleCardsAdapter extends ArrayAdapter<AlarmSetup> {

    private Context mContext;

    public GoogleCardsAdapter(final Context context) {
      mContext = context;
    }

    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
      ViewHolder viewHolder;
      View view = convertView;

      Log.e(TAG, "Getting view for position: " + position );

      if (view == null) {
        view = LayoutInflater.from(mContext).inflate(R.layout.activity_alarm_listview_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.textViewContainer = (RelativeLayout) view.findViewById(R.id.lv_message_container);
        viewHolder.textView = (TextView) view.findViewById(R.id.lv_message);
        viewHolder.buttonViewAlarmSetup = (Button) view.findViewById(R.id.btn_alarmsetup_view);
        viewHolder.buttonDeleteAlarmSetup = (Button) view.findViewById(R.id.btn_alarmsetup_delete);

        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder) view.getTag();
      }

      // Subscription
      final AlarmSetup as = getItem(position);
      final int id = position;
      viewHolder.textView.setText( as.getName()  );

      // Attach clickhandlers

      // View alarm setup
      viewHolder.buttonViewAlarmSetup.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

          // Show the Alarm Setup in a dialog
          Dialog dialog = new Dialog(mContext);
          dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
          dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
          dialog.setContentView(R.layout.dialog_alarm_setup);
          dialog.setCanceledOnTouchOutside(true);

          // Set all the data
          TextView tv = (TextView) dialog.findViewById(R.id.tv_alarm_setup);

          String alarmSetupString = "";
          alarmSetupString += "Name: " + as.getName() + "\n";
          alarmSetupString += "Filter word: " + as.getFilterWord() + "\n";
          alarmSetupString += "Notification enabled: " + as.getNotification() + "\n";
          alarmSetupString += "Sound enabled: " + as.getSound() + "\n";
          alarmSetupString += "Vibrate enabled: " + as.getVibrate() + "\n";
          alarmSetupString += "LED enabled: " + as.getBlinkLED() + "\n";
          alarmSetupString += "ScreenOn enabled: " + as.getTurnOnScreen() + "\n";

          // Set the generated string as content of the textview in the dialog
          tv.setText(alarmSetupString);

          dialog.show();

        }
      });

      // Delete alarm setup
      viewHolder.buttonDeleteAlarmSetup.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Log.w(TAG, "Ask for confirmation to delete alarm setup: " + id);

          //Ask the user if they want to quit
          String confirmDialogMessage = String.format(mContext.getResources().getString(R.string.settings_dialog_confirm_remove_alarm_setup_description), String.valueOf(id) );
          new AlertDialog.Builder(mContext)
                  .setIcon(android.R.drawable.ic_dialog_alert)
                  .setTitle(R.string.settings_dialog_confirm_remove_alarm_setup_title)
                  .setMessage(confirmDialogMessage)
                  .setPositiveButton(R.string.settings_dialog_yes_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                      // Now start unsubscribing
                      ((AlarmActivity) mContext).removeAlarmSetup(id);

                    }

                  })
                  .setNegativeButton(R.string.no, null)
                  .show();

        }
      });

      return view;
    }

    private static class ViewHolder {
      int id;
      RelativeLayout textViewContainer;
      TextView textView;
      Button buttonDeleteAlarmSetup;
      Button buttonViewAlarmSetup;
    }
  }

  private void removeAlarmSetup(final int id){

    Log.w(TAG, "removeAlarmSetup: " + id);

    startLoadingScreen(LOADING_TYPE_DELETE_ALARM);

    // Remove the alarm setup
    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          try {
            Log.w(TAG, "Start delete alarm setup: " + id);
            eveService.getMobileAgent().deleteAlarmSetup(id);
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {

              // Hide the loading screen
              hideLoadingScreen();

              // Reload the current list of alarms
              loadAlarmSetupsList();

            }
          });

        }
      };

      // Start the thread with runnables
      new Thread(r).start();

    } catch (Exception e){
      Log.w(TAG, "[Runnable] Could not execute the requested function on the local agent in the eveservice");
      Crouton.showText(self, getResources().getString(R.string.error_no_result), Style.ALERT);
      e.printStackTrace();

      // Also hide the loading screen if something went wrong
      hideLoadingScreen();
    }

  }

}