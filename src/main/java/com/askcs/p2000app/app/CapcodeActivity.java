package com.askcs.p2000app.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.askcs.p2000app.R;
import com.askcs.p2000app.callbacks.onEveServiceReady;
import com.askcs.p2000app.entities.AlarmSetup;
import com.askcs.p2000app.entities.CapcodeSubscription;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import java.util.ArrayList;

/**
 * Created by Jordi on 24-2-14.
 */
public class CapcodeActivity extends BaseActivity {

  // Debug tag
  public static String TAG = MainActivity.class.getCanonicalName();

  // Listview
  ListView listView = null;
  private GoogleCardsAdapter mGoogleCardsAdapter;
  SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = null;

  // Reference to self
  private final CapcodeActivity self = this;

  // P2000
  private ArrayList<CapcodeSubscription> subscriptions = new ArrayList<CapcodeSubscription>();

  // Alarm Setups
  private ArrayList<AlarmSetup> alarmSetups = new ArrayList<AlarmSetup>();

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Busprovider
    BusProvider.getBus().register(this);

    // Request service binding
    requestService(new onEveServiceReady() {
      @Override
      public void onEveServiceReady(EveService es) {
        // Notify our local event listener - eveservice now available to use
        self.onEveServiceReady(es);
      }
    });

    // TODO: The application could be make blocking from here until the eveservice is available
    // TODO: This will make it possible to remove all 'eveService!=null' checks in the code

    // Listview - specific for this application
    listView = (ListView) findViewById(R.id.lv);

    mGoogleCardsAdapter = new GoogleCardsAdapter(this);
    //SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mGoogleCardsAdapter, this));
    swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mGoogleCardsAdapter);
    swingBottomInAnimationAdapter.setInitialDelayMillis(300);
    swingBottomInAnimationAdapter.setAbsListView(listView);

    listView.setAdapter(swingBottomInAnimationAdapter);

  }

  @Override
  public void onDestroy(){

    // Busprovider
    BusProvider.getBus().unregister(this);

    super.onDestroy();
  }

  // From here we can do whatever  we want with the EveService object directly
  @Override
  protected void onEveServiceReady(EveService es){
    super.onEveServiceReady(es);
    getCurrentSubscriptions();
  }

  /* START -- Activity specific menu adjustments */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    super.onPrepareOptionsMenu(menu);

    // Activity specific menu item changes
    menu.findItem(R.id.action_refresh).setVisible(true);
    menu.findItem(R.id.action_capcode_add).setVisible(true);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // Activity specific menu item handlers
    switch (item.getItemId()) {
      case R.id.action_refresh:

        getCurrentSubscriptions();

        break;
      case R.id.action_capcode_add:

        showDialogAddCapcode();

        break;
    }

    // Otherwise fallback to BaseActivity menu items handlers
    return  super.onOptionsItemSelected(item);
  }
  /* END -- Activity specific menu adjustments */

  /* It's possible to do things here for this specific activity while the service is being bound to the activity */
  /* START -- Service bound events */
  public void onStartServiceRequest(){
    super.onStartServiceRequest();
    Log.e(TAG, "onStartServiceRequest");
  }

  public void onStopServiceRequest(){
    Log.e(TAG, "onStopServiceRequest");
    super.onStopServiceRequest();
  }
  /* END -- Service bound events */


  /* START -- Activity specific functionalities */

  public void getCurrentSubscriptions(){

    // Show loading screen when starting long-running code
    startLoadingScreen(LOADING_TYPE_SUBSCRIPTIONS);

    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          try {
            self.subscriptions = eveService.getMobileAgent().getSubscriptions();
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              self.buildSubscriptionsList();
            }
          });

        }
      };

      // Start the thread with runnables
      new Thread(r).start();

    } catch (Exception e){
      Log.w(TAG, "[Runnable] Could not execute the requested function on the local agent in the eveservice");
      Crouton.showText(this, getResources().getString(R.string.error_no_result), Style.ALERT);
      e.printStackTrace();

      // Also hide the loading screen if something went wrong
      hideLoadingScreen();
    }

  }

  private void buildSubscriptionsList(){
   // Make the list animate (when reloaded) again
    mGoogleCardsAdapter.clear(); // Empty the list
    swingBottomInAnimationAdapter.reset();

    // Add the subscriptions list to the adapter
    mGoogleCardsAdapter.addAll( subscriptions ); // Add the subscriptions
    mGoogleCardsAdapter.notifyDataSetChanged(); // Notify that the list is changed

    // Hide loading screen if it's all done
    hideLoadingScreen();
  }


  private static class GoogleCardsAdapter extends ArrayAdapter<CapcodeSubscription> {

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
        view = LayoutInflater.from(mContext).inflate(R.layout.activity_settings_listview_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.textViewContainer = (RelativeLayout) view.findViewById(R.id.lv_message_container);
        viewHolder.textViewCapcode = (TextView) view.findViewById(R.id.tv_capcode);
        viewHolder.textViewAlarmSetup = (TextView) view.findViewById(R.id.tv_alarm_setup_name);
        viewHolder.buttonEditCapcode = (Button) view.findViewById(R.id.btn_capcode_edit);
        viewHolder.buttonDeleteCapcode = (Button) view.findViewById(R.id.btn_capcode_delete);

        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder) view.getTag();
      }

      // Subscription
      CapcodeSubscription ccs = getItem(position);
      final String cc = ccs.getCapcode();
      viewHolder.textViewCapcode.setText( ccs.getCapcode() );

      // Determine the alarmsetup name for this capcode
      String alarmSetupName = "";
      if(ccs.getAsName() != null && !ccs.getAsName().equals("")){
        alarmSetupName = ccs.getAsName();
      } else {
        alarmSetupName = mContext.getResources().getString(R.string.lv_text_helper_alarmsetup_none);
      }

      // Show (if any) which alarmsetup is linked to this capcode
      viewHolder.textViewAlarmSetup.setText( mContext.getResources().getString(R.string.lv_text_helper_alarmsetup) + " " + alarmSetupName );

      // Attach clickhandlers

      // Edit capcode
      viewHolder.buttonEditCapcode.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Log.w(TAG, "Open the edit dialog for capcode: " + cc);

          // Load the AlarmSetups first
          ((CapcodeActivity) mContext).loadAlarmSetupsList(cc);
        }
      });

      // Delete capcode
      viewHolder.buttonDeleteCapcode.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Log.w(TAG, "Ask for confirmation to delete capcode: " + cc);

          //Ask the user if they want to quit
          String confirmDialogMessage = String.format(mContext.getResources().getString(R.string.settings_dialog_confirm_unsubscribe_description), cc);
          new AlertDialog.Builder(mContext)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.settings_dialog_confirm_unsubscribe_title)
            .setMessage(confirmDialogMessage)
            .setPositiveButton(R.string.settings_dialog_yes_unsubscribe, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {

                // Now start unsubscribing
                ((CapcodeActivity) mContext).unsubscribeCapcode(cc);

              }

            })
            .setNegativeButton(R.string.no, null)
            .show();

         }
      });

      return view;
    }

    private static class ViewHolder {
      String capcode;
      RelativeLayout textViewContainer;
      TextView textViewCapcode;
      TextView textViewAlarmSetup;
      Button buttonEditCapcode;
      Button buttonDeleteCapcode;
    }
  }

  private void unsubscribeCapcode(final String capcode){

    startLoadingScreen(LOADING_TYPE_REMOVE_CAPCODE);

    // Ubsubscribe the capcode
    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          try {
            eveService.getMobileAgent().unsubscribe(capcode);
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {

              // Hide the loading screen
              hideLoadingScreen();

              // Reload the current list of subscriptions
              getCurrentSubscriptions();

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

  private Dialog dialog = null;
  private void showDialogAddCapcode(){
    dialog = new Dialog(this);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    dialog.setContentView(R.layout.dialog_setting_add_capcode);
    dialog.setCanceledOnTouchOutside(true);

    // Get the capcode from the UI
    final AutoCompleteTextView et = (AutoCompleteTextView) dialog.findViewById(R.id.et_capcode); // AutoCompleteTextView

    // Get the string array
    String[] capcodes = getResources().getStringArray(R.array.capcodes_array);

    // Create the adapter and set it to the AutoCompleteTextView
    android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<String>(this, R.layout.activity_settings_capcodes_listview_item, capcodes);

    et.setAdapter(adapter);
    // Submit button listener
    Button submitAddCapcode = (Button) dialog.findViewById(R.id.btn_submit_add_capcode);
    submitAddCapcode.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.w(TAG, "CLICK!");

        // Check if this is a 'pure capcode' or if it also contains names
        // Only get the capcode (everything before the : if any)
        String capcodeInput = et.getText().toString();
        String capcodeString = "";
        if(capcodeInput.contains(": ")){
          String[] capcodeStrings = capcodeInput.split(":");
          capcodeString = capcodeStrings[0].toString();
        } else {
          capcodeString = capcodeInput;
        }

        // Make it final for use in the runnable
        final String capcode = capcodeString;

        if( capcode.length() < 6 || capcode.length() > 7 || !capcode.matches("[0-9]+") ){
          // To short, to long or non-numeric
          et.setError(getResources().getString(R.string.settings_dialog_invalid_capcode));
          return;
        }

        // Close the form in the dialog
        hideDialogAddCapcode();

        startLoadingScreen(LOADING_TYPE_ADD_CAPCODE);

        // Add the capcode as a subscription
        try {

          // Execute the function in a separate runnable thread
          Runnable r = new Runnable() {
            @Override
            public void run() {

              // Execute the long running task (which is blocking)
              // The result is saved in the self (this) object of the thread creator
              try {
                eveService.getMobileAgent().subscribe(capcode);
              } catch (Exception e) {
                e.printStackTrace();
              }

              // Do something with the result by starting a runnable on the UI thread
              runOnUiThread(new Runnable() {
                @Override
                public void run() {

                  // Hide the loading screen
                  hideLoadingScreen();

                  // Reload the current list of subscriptions
                  getCurrentSubscriptions();

                }
              });

            }
          };

          // Start the thread with runnables
          new Thread(r).start();

        } catch (Exception e){
          Log.w(TAG, "[Runnable] Could not execute the requested function on the local agent in the evenervice");
          Crouton.showText(self, getResources().getString(R.string.error_no_result), Style.ALERT);
          e.printStackTrace();

          // Also hide the loading screen if something went wrong
          hideLoadingScreen();
        }

      }
    });


    dialog.show();

  }

  private void hideDialogAddCapcode(){
    if(dialog != null){
      dialog.dismiss();
    }
  }

  // List length properties (UI display)
  private void loadAlarmSetupsList(final String capcode){
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
              hideLoadingScreen();
              self.showAlarmSetupChooser(capcode);
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

  private void showAlarmSetupChooser(final String capcode){

    // Show dialog with all the alarm setups
    final Dialog linkDialog = new Dialog(this);
    linkDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    linkDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    linkDialog.setContentView(R.layout.dialog_link_alarmsetup_to_capcode);
    linkDialog.setCanceledOnTouchOutside(true);

    // Update the title to also show the capcode
    TextView tvDialogTitle = (TextView) linkDialog.findViewById(R.id.tv_capcode_alarmsetup_link_title);
    tvDialogTitle.setText( tvDialogTitle.getText() + " " + capcode );

    // Put the loaded Alarm Setups into the listview
    final Spinner sp = (Spinner) linkDialog.findViewById(R.id.spinner_alarmsetups);

    android.widget.ArrayAdapter spinnerArrayAdapter = new android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, alarmSetups);

    sp.setAdapter(spinnerArrayAdapter);

    // Listener on the submit (link) button
    Button b = (Button) linkDialog.findViewById(R.id.btn_submit_link_alarmsetup_to_capcode);

    // Disable the button if there are no setups to display
    if(alarmSetups.size() == 0){
      b.setEnabled(false);
      Crouton.showText(this, getResources().getString(R.string.dialog_no_alarm_setups), Style.INFO);
    } else {

      // If there are one or more items, keep the button enabled and attach this click listener
      b.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {

          // Dismiss the dialog with the linking-form
          linkDialog.dismiss();

          // Get the chosen result and link it to the earlier chosen capcode
          AlarmSetup as = (AlarmSetup) sp.getSelectedItem();
          Log.w(TAG, "Chosen: " + as.getName() + " for capcode " + capcode);
          linkAlarmSetupToCapcode(as, capcode);
        }
      });

    }

    linkDialog.show();

  }

  private void linkAlarmSetupToCapcode(final AlarmSetup as, final String capcode){

    startLoadingScreen(LOADING_TYPE_LINK_ALARM_CAPCODE);

    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          try {
            eveService.getMobileAgent().addCapcodeLinkAlarmSetup(as, capcode);
          } catch (Exception e) {
            e.printStackTrace();
          }

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              hideLoadingScreen();
              getCurrentSubscriptions(); // Reload the list to reflect the change
            }
          });

        }
      };

      // Start the thread with runnables
      new Thread(r).start();

    } catch (Exception e){
      Log.w(TAG, "[asyncCall] Could not updat the list of capcode links to alarm setups from the local agent state");
      Crouton.showText(this, getResources().getString(R.string.error_no_result), Style.ALERT);
      e.printStackTrace();

      // Also hide the loading screen if something went wrong
      hideLoadingScreen();
    }

  }

}