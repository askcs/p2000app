package com.askcs.p2000app.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.askcs.commons.entity.P2000Message;
import com.askcs.p2000app.R;
import com.askcs.p2000app.agent.MobileAgent;
import com.askcs.p2000app.callbacks.onEveServiceReady;
import com.askcs.p2000app.events.EveServiceStateChangeEvent;
import com.askcs.p2000app.events.LoginEvent;
import com.askcs.p2000app.events.LoginStateChangeEvent;
import com.askcs.p2000app.events.p2000MessagesStateChangeEvent;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.askcs.p2000app.util.DateFormatter;
import com.askcs.p2000app.util.SimpleP2000Processor;
import com.nhaarman.listviewanimations.ArrayAdapter;
import com.nhaarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.squareup.otto.Subscribe;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import sun.management.resources.agent;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Created by Jordi on 24-2-14.
 */
public class MainActivity extends BaseActivity {

  // Debug tag
  public static String TAG = MainActivity.class.getCanonicalName();

  // Listview
  ListView listView = null;
  private GoogleCardsAdapter mGoogleCardsAdapter;
  SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = null;

  // Reference to self
  private final MainActivity self = this;

  // P2000
  private ArrayList<P2000Message> p2000messages = new ArrayList<P2000Message>();


  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Log.w(TAG, "[MainActivity] onCreate");

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

    // TODO: The application could be make blocking from here untill the eveservice is available
    // TODO: This will make it possible to remove all 'eveService!=null' checks in the code

    // Listview - specific for this application
    listView = (ListView) findViewById(R.id.lv);

    // Placeholder for when the listview is empty
    TextView emptyText = (TextView)findViewById(R.id.lv_empty_text);
    listView.setEmptyView(emptyText);

    mGoogleCardsAdapter = new GoogleCardsAdapter(this);
    //SwingBottomInAnimationAdapter swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(new SwipeDismissAdapter(mGoogleCardsAdapter, this));
    swingBottomInAnimationAdapter = new SwingBottomInAnimationAdapter(mGoogleCardsAdapter);
    swingBottomInAnimationAdapter.setInitialDelayMillis(300);
    swingBottomInAnimationAdapter.setAbsListView(listView);

    listView.setAdapter(swingBottomInAnimationAdapter);

  }

  // From here we can do whatever  we want with the EveService object directly
  @Override
  protected void onEveServiceReady(EveService es){
    super.onEveServiceReady(es);
    Log.w(TAG, "[MainActivity] onEveServiceReady");
    loadP2000MessagesList();
  }

  /* START -- Activity specific menu adjustments */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu)
  {
    super.onPrepareOptionsMenu(menu);

    // Activity specific menu item changes
    menu.findItem(R.id.action_refresh).setVisible(true);
    menu.findItem(R.id.action_capcode_add).setVisible(false);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    // Activity specific menu item handlers
    switch (item.getItemId()) {
      case R.id.action_refresh:

        if(eveService != null){
          loadP2000MessagesList();
        }

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

  // List length properties (UI display)
  private static final int MAX_DISPLAYED_MESSAGES = 25;
  private int totalMessagesListCount = 0;
  private void loadP2000MessagesList(){
    Log.w(TAG, "[MainActivity] loadP2000MessagesList");

    startLoadingScreen(LOADING_TYPE_MESSAGES);

    try {

      // Execute the function in a separate runnable thread
      Runnable r = new Runnable() {
        @Override
        public void run() {

          // Execute the long running task (which is blocking)
          // The result is saved in the self (this) object of the thread creator
          self.p2000messages = eveService.getMobileAgent().getP2000Messages();

          // Do something with the result by starting a runnable on the UI thread
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              self.buildP2000List();
            }
          });

        }
      };

      // Start the thread with runnables
     new Thread(r).start();

    } catch (Exception e){
      Log.w(TAG, "[asyncCall] Could not get the current list of p2000 messages from the local agent state");
      Crouton.showText(this, getResources().getString(R.string.error_no_result), Style.ALERT);
      e.printStackTrace();

      // Also hide the loading screen if something went wrong
      hideLoadingScreen();
    }

  }

  private void buildP2000List(){
    // Calculate the sublist of the p2000 list
    totalMessagesListCount = p2000messages.size();

    // Get the last X (MAX_DISPLAYED_MESSAGES) messages because new items are added to the end of the list
    int startSubListIndex = totalMessagesListCount - MAX_DISPLAYED_MESSAGES;
    int endSubListIndex = totalMessagesListCount;

    Log.w(TAG, "Select subset from P2000 list: " + startSubListIndex + ", " + endSubListIndex + " ["+ (endSubListIndex-startSubListIndex) +"]");

    // Only get a subset if the start is beyond the regular start (0)
    List p2000messagesList = null;
    if(startSubListIndex > 0){
      // NOTE: Casting the result to an ArrayList didnt work, just create a new ArrayList based on the subList content
      p2000messages = new ArrayList<P2000Message>( p2000messages.subList(startSubListIndex, endSubListIndex) );
    }

    // Reverse the list of messages; Newest first (because the newest items are added at the end)
    Collections.reverse(p2000messages);

    // Clear all current items for when it's a reload of the list
    mGoogleCardsAdapter.clear();

    // Make the list animate on new incoming alarms
    swingBottomInAnimationAdapter.reset();

    // Add the p2000 messages list to the adapter
    mGoogleCardsAdapter.addAll( p2000messages );
    mGoogleCardsAdapter.notifyDataSetChanged();

    // Remove the loading screen
    hideLoadingScreen();
  }

  private static class GoogleCardsAdapter extends ArrayAdapter<P2000Message> {

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
        view = LayoutInflater.from(mContext).inflate(R.layout.activity_main_listview_item, parent, false);

        viewHolder = new ViewHolder();
        viewHolder.textViewContainer = (LinearLayout) view.findViewById(R.id.lv_message_container);
        viewHolder.textHeadingCapcode = (TextView) view.findViewById(R.id.lv_heading_capcode);
        viewHolder.textHeadingPriority = (TextView) view.findViewById(R.id.lv_heading_priority);
        viewHolder.textHeadingTime = (TextView) view.findViewById(R.id.lv_heading_time);
        viewHolder.textView = (TextView) view.findViewById(R.id.lv_message);
        view.setTag(viewHolder);
      } else {
        viewHolder = (ViewHolder) view.getTag();
      }

      P2000Message m = getItem(position);
      Long timestamp = Long.valueOf( m.getTimestamp() );
      Date messageDateObj = new Date( timestamp );

      // Get some basic info about the P2000 message
      SimpleP2000Processor.EmergencyPriority prio = SimpleP2000Processor.getPriority( m.getMessage() );
      SimpleP2000Processor.EmergencyService service = SimpleP2000Processor.getService( m.getMessage() );
      int serviceColor = SimpleP2000Processor.getServiceColor(service);

      // Set heading texts
      viewHolder.textHeadingCapcode.setText( m.getCapcode() );
      viewHolder.textHeadingPriority.setText( prio.toString() );
      viewHolder.textHeadingTime.setText( DateFormatter.formatFullTime(messageDateObj) );

      // Set the actual P2000 message
      viewHolder.textView.setText( m.getMessage() );

      // Set the color of this emergencyservice as border color of the container
      viewHolder.textViewContainer.setBackgroundColor(serviceColor);

      // Give the timestamp a color indication of how old the message is
      Integer messageColor = getRecentColor(mContext, timestamp);
      if(messageColor != null){
        viewHolder.textHeadingTime.setBackgroundColor(messageColor);
      } else {
        // Reset
        viewHolder.textHeadingTime.setBackgroundResource(android.R.color.transparent);
      }

      return view;
    }

    // Levels for color coding messages
    private static final Long NO_COLOR_LEVEL = Long.valueOf( 15 * (60 * 1000) ); // 15min
    private static final Long NEW_LEVEL_4 = Long.valueOf( 15 * (60 * 1000) ); // 15min
    private static final Long NEW_LEVEL_3 = Long.valueOf( 10 * (60 * 1000) ); // 10min
    private static final Long NEW_LEVEL_2 = Long.valueOf( 5 * (60 * 1000) ); // 5min
    private static final Long NEW_LEVEL_1 = Long.valueOf( 2 * (60 * 1000) ); // 2min
    private Integer getRecentColor(Context ctx, Long timestamp){

      // Timediff between now and the message
      Long diffTimeMillis = System.currentTimeMillis() - timestamp;

      //Log.e(TAG, "Time diff: " + diffTimeMillis);

      // The recent color
      Integer c = null;

      // Nothing if the message is to old
      if(diffTimeMillis >= NO_COLOR_LEVEL) return c;

      if(diffTimeMillis < NEW_LEVEL_4) c = Color.YELLOW;
      if(diffTimeMillis < NEW_LEVEL_3) c = ctx.getResources().getColor(R.color.Orange);
      if(diffTimeMillis < NEW_LEVEL_2) c = ctx.getResources().getColor(R.color.DarkOrange);
      if(diffTimeMillis < NEW_LEVEL_1) c = Color.RED;

      return c;

    }

    private static class ViewHolder {
      LinearLayout textViewContainer;
      TextView textHeadingCapcode;
      TextView textHeadingPriority;
      TextView textHeadingTime;
      TextView textView;
    }
  }

  private boolean listIsAtTop(){
    if(listView.getChildAt(0) != null){
      return listView.getChildAt(0).getTop() > 0;
    } else {
      // In case the list is empty
      return true;
    }
  }


  @Override
  public void onResume(){
    super.onResume();

    Log.w(TAG, "[MainActivity] onResume");

    // Reload list of P2000 messages
    // Only run this if the EveService is not null (onEveService ready may not have been triggered yet)
    if(eveService != null){
      loadP2000MessagesList();
    }

  }

  @Override
  public void onDestroy(){

    // Busprovider
    BusProvider.getBus().unregister(this);

    super.onDestroy();
  }

  // Listen via busprovider events for new incoming p2000 messages
  @Subscribe
  public void onNewP2000Message(p2000MessagesStateChangeEvent pmsce){

    Log.w(TAG, "[MainActivity] onNewP2000Message");

    // Only run this if the EveService is not null (onEveService ready may not have been triggered yet)
    if(eveService != null){
      if(pmsce.getState().equals(MobileAgent.EVENT_NEW_P2000_MESSAGE)){

        // Show crouton if currently not at the top of the list
        if(!listIsAtTop()){
          Crouton.cancelAllCroutons();
          Crouton.showText(this, R.string.new_p2000_message_notification, Style.INFO);
        }

        loadP2000MessagesList();
      }
    }

  }

  // Prevent that the back button brings the user back to the login screen without actually logging out
  @Override
  public void onBackPressed() {
    new AlertDialog.Builder(this)
      .setIcon(android.R.drawable.ic_dialog_alert)
      .setTitle( getResources().getString( R.string.logout_dialog_title ))
      .setMessage(getResources().getString(R.string.logout_dialog_text))
      .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          // Logout via the service
          if (eveService != null) {
            eveService.logout();
          }

          // Finish the main activity
          finish();
        }

      })
      .setNegativeButton(getResources().getString(R.string.no), null)
      .show();
  }

}