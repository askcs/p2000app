package com.askcs.p2000app.agent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.almende.eve.agent.proxy.AsyncProxy;
import com.almende.eve.rpc.annotation.Access;
import com.almende.eve.rpc.annotation.AccessType;
import com.almende.eve.rpc.annotation.Name;
import com.almende.eve.rpc.jsonrpc.jackson.JOM;
import com.almende.util.TypeUtil;
import com.askcs.commons.agent.AskAgent;
import com.askcs.commons.agent.intf.*;
import com.askcs.commons.entity.DataSource;
import com.askcs.commons.entity.P2000Message;
import com.askcs.p2000app.R;
import com.askcs.p2000app.app.LoaderActivity;
import com.askcs.p2000app.entities.AlarmSetup;
import com.askcs.p2000app.entities.CapcodeSubscription;
import com.askcs.p2000app.events.p2000MessagesStateChangeEvent;
import com.askcs.p2000app.service.EveService;
import com.askcs.p2000app.util.BusProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by Jordi on 24-2-14.
 */
public class MobileAgent extends AskAgent {

  private static final String	VERSION	= "1";
  private static Context context	= null;

  public static String TAG = MobileAgent.class.getCanonicalName();

  /* State fields */
  private static final String STATE_FIELD_P2000_MESSAGE = "p2000messages";
  private static final String STATE_FIELD_ALARM_SETUPS = "alarmSetups";
  private static final String STATE_FIELD_ALARM_SETUPS_LINK_CAPCODE = "alarmSetupsLinkedCapcode";

  /* Busprovider states */
  public static final String EVENT_NEW_P2000_MESSAGE = "eventNewP2000Message";

  public static void setContext(Context context) {
    MobileAgent.context = context;
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  /* XMPP Connection properties*/
  private String username = "";
  private String password = "";
  private String passwordOriginal = "";

  public void connect(String usernameInput, String passwordInput, String passwordOriginalInput) throws Exception {

    this.username = usernameInput;
    this.password = passwordInput;
    this.passwordOriginal = passwordOriginalInput;

    System.out.println("AlarmAgent : connectUser -> " + this.username + ", " + this.password);

    // Save these values to the (existing) preferences
    // This way the service can reconnect when the application is inactive or even closed.
    // It will get the username and password from the preferences/local state and setup the connection again

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = settings.edit();
    editor.putString(context.getString(R.string.sp_xmpp_username_key), username);
    editor.putString(context.getString(R.string.sp_xmpp_password_key), password);
    editor.putString(context.getString(R.string.sp_xmpp_original_password_key), passwordOriginalInput);
    editor.commit();

    // Couple this account to this agent
    setAccount(username, password, EveService.MOBILE_AGENT_RESOURCE);

    // If this fails, directly remove the account since it's not working
    try{
      this.reconnect();
    }catch(Exception e){
      removeAccount(username, password);
      throw e;
    }
  }

  public void reconnect() throws Exception {

    System.err.println("Reconnect method is called.");

    String uName = getUsername();
    String uPassword = getPassword();

    System.out.println("MobileAgent : reconnect -> " + uName + ", " + uPassword);

    // Set the account of this agent
    System.out.println("MobileAgent: setAccount + register: " + uName + ", " + uPassword + ",  android");

    // Connect to XMPP
    connect();

  }

  // Called by the EveService in disconnect
  public void logout(){
    // Do some last minute cleanup
  }

  public String getUsername(){
    return super.getUsername();
  }

  public String getSharedUsername(){
    if(username.equals("") || username == null){
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      this.username = prefs.getString( context.getString(R.string.sp_xmpp_username_key), "");
    }
    System.out.println("[getUsername] Username : " + username);
    return username;
  }

  public String getPassword(){
    return super.getPassword();
  }

  public String getSharedPassword(){
    if(password.equals("") || password == null){
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      this.password = prefs.getString( context.getString(R.string.sp_xmpp_password_key), "");
    }
    System.out.println("[getPassword] Password : " + password);
    return password;
  }

  public String getSharedOriginalPassword(){
    if(passwordOriginal.equals("") || passwordOriginal == null){
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      this.passwordOriginal = prefs.getString( context.getString(R.string.sp_xmpp_original_password_key), "");
    }
    System.out.println("[getOriginalPassword] Password Original : " + passwordOriginal);
    return passwordOriginal;
  }

  /* Incoming calls (external) */
  private static final String P2000_MESSAGE_HANDLER = "onP2000Message";
  private static final int MAX_LOCAL_P2000_MESSAGES = 150;

  @Access(AccessType.PUBLIC)
  public void onP2000Message(@Name("capcode") String capcode, @Name("message") String message, @Name("timestamp") String timestamp) {

    Log.e(TAG, "[" + capcode + "] " + message + " (" + timestamp + ")");

    // Save this message
    ArrayList<P2000Message> p2000Messages = getState().get(STATE_FIELD_P2000_MESSAGE, new TypeUtil<ArrayList<P2000Message>>() {});
    if(p2000Messages == null) p2000Messages = new ArrayList<P2000Message>();

    p2000Messages.add( new P2000Message(timestamp, message, capcode) );

    // Shorten the p2000Messages array if it's beyond MAX_LOCAL_P2000_MESSAGES
    int p2000MessagesCount = p2000Messages.size();
    if(p2000MessagesCount >= MAX_LOCAL_P2000_MESSAGES){
      Log.w(TAG, "Locally stored p2000 messages list shortened: " + (p2000MessagesCount - MAX_LOCAL_P2000_MESSAGES) + ", " + p2000MessagesCount);
      p2000Messages = new ArrayList<P2000Message>( p2000Messages.subList( (p2000MessagesCount - MAX_LOCAL_P2000_MESSAGES), p2000MessagesCount) );
    }

    getState().put(STATE_FIELD_P2000_MESSAGE, p2000Messages);

    // Alert the user front-end
    BusProvider.getBus().post( new p2000MessagesStateChangeEvent(EVENT_NEW_P2000_MESSAGE) );


    // {{{{{{{{{{{{{{{{{{{{{{{{{{
    // {{{{{{{{{{{{{{{{{{{{{{{{{{
    // TODO: Replace with the AlarmSetup system; check for setups for this capcode; get the actual setup; run the setup/preset
    // Depending on what sound be done it could be executed from here or from within the MainActivity (startActivity with extras)
    // TODO: Before this can be done: Create interface to link Alarm setups to a capcode

    // Get the shared prefs field with filtering words (Only accept messages matching one or multiple given filter words)
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences( context );
    String filtering = sp.getString("general_filter", "");

    // Default values if filtering is not explicitly enabled
    boolean useFiltering = false;
    boolean isMatch = false;

    // Only use filtering
    if(!filtering.equals("")){

      //Log.w(TAG, "Notifications - Filtering enabled");

      // Yes please, filtering on from now on
      useFiltering = true;

      // Get every single given filter word
      String[] filterWords = filtering.split(";");

      //Log.w(TAG, "Notifications - Filter words: " + filterWords.toString());

      // Lower the whole string to make the contains() filtering check case-insensitive
      String m = message.toLowerCase();

      //Log.w(TAG, "Notifications - Lowered message: " + m);

      // Loop over P2000 message and search for matches
      for (String s : filterWords){

        // Convert to lowercase string
        s = s.toLowerCase();

        //Log.w(TAG, "Notifications - Check word: " + s);
        if( m.contains(s) ){
          // Hit!
          //Log.w(TAG, "Notifications - The lowered messages contains the filter word: " + s);
          isMatch = true;
        }

      }

    } else {
      //Log.w(TAG, "Notifications - Filtering disabled");
    }

    //Log.w(TAG, "Notifications - useFiltering: " + useFiltering + ", isMatch: " + isMatch);

    // Only process this message further if filtering is disabled (none given) or if it's enabled, it should match
    if(useFiltering == false || (useFiltering == true && isMatch == true) ){

      // Push a notification
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
      Intent intent = new Intent(context, LoaderActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);

      // Build notification
      Notification noti = new NotificationCompat.Builder(context)
              .setContentTitle("New P2000 message ["+capcode+"]")
              .setContentText("["+capcode+"] " + message).setSmallIcon(R.drawable.ic_launcher)
              .setPriority(Notification.PRIORITY_MAX)
              .setUsesChronometer(true)
              .setContentIntent(pIntent).build();

      // Hide the notification after its selected
      noti.flags |= Notification.FLAG_AUTO_CANCEL; // Auto remove notification when clicked

      // Light
      noti.ledOnMS  = 800;    //Set led blink (Off in ms)
      noti.ledOffMS = 600;    //Set led blink (Off in ms)
      noti.ledARGB = 0xff00ff00;   //Set led color
      noti.flags |= Notification.FLAG_SHOW_LIGHTS;


      //RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      //RingtoneManager.getDefaultUri(1);

      // Sound
      noti.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

      notificationManager.notify(10, noti);
    }

    // }}}}}}}}}}}}}}}}}}}}}}}}}}
    // }}}}}}}}}}}}}}}}}}}}}}}}}}

  }

  // TODO: Daily scheduler to remove messages older than 30 days from local state

  /* Outgoing calls (internal) */
  public ArrayList<P2000Message> getP2000Messages(){
    ArrayList<P2000Message> m = getState().get(STATE_FIELD_P2000_MESSAGE, new TypeUtil<ArrayList<P2000Message>>() {});
    if(m==null) return new ArrayList<P2000Message>();
    return m;
  }

  private static final String REMOTE_GET_MESSAGES_FUNCTION = "getSubscriptions";
  public ArrayList<CapcodeSubscription> getSubscriptions() throws Exception {

    // Create a proxy to the P2000 agent
    AsyncProxy<P2000AgentIntf> p2000Agent = getAsyncP2000Agent();

    Future<?> asyncCall = p2000Agent.call(REMOTE_GET_MESSAGES_FUNCTION);
    HashMap<String, HashMap<String,String>> subs = (HashMap<String, HashMap<String,String>>) asyncCall.get(); // TODO: Add timeouts: EveService.PROXY_TIMEOUT_INTERVAL, EveService.PROXY_TIMEOUT_TIMEUNIT);

    // Also get the link between capcodes and AlarmSetups
    HashMap<String, String> links = getCapcodesLinkAlarmSetups();

    ArrayList<CapcodeSubscription> ownSubscriptions = new ArrayList<CapcodeSubscription>();
    String ownXmppUrl = getXmppUrl();
    for(String capcode : subs.keySet()) {
      for(String url : subs.get(capcode).keySet()) {
        if(url.equalsIgnoreCase(ownXmppUrl)){

          // Just a check, if may not be neccasary, but for debugging purposes it's useful
          if(links.containsKey(capcode)){
            ownSubscriptions.add( new CapcodeSubscription(capcode, links.get(capcode)) );
          } else {
            ownSubscriptions.add( new CapcodeSubscription(capcode, null) );
          }
        }
      }
    }

    // Sort the values so there is some order instead of random (which it is when taken from the state)
    Collections.sort(ownSubscriptions);

    return ownSubscriptions;

  }

  // Alarm Setups
  public ArrayList<AlarmSetup> getAlarmSetups() throws Exception {

    ArrayList<AlarmSetup> m = getState().get(STATE_FIELD_ALARM_SETUPS, new TypeUtil<ArrayList<AlarmSetup>>() {});
    if(m==null) return new ArrayList<AlarmSetup>();
    return m;

  }

  public boolean addAlarmSetup(AlarmSetup as) throws Exception {
    ArrayList<AlarmSetup> ass = getAlarmSetups();

    if(ass.contains(as)) return false; // Duplicate

    // -------------
    // TODO: Check all the AlarmSetups for the same name; for the linking to capcodes to work correctly EVERY SETUP NEEDS A UNIQUE NAME
    // -------------

    // Add the new setup
    ass.add(as);

    // Write it all back to the state
    setAlarmSetups(ass);

    return true;
  }

  public boolean deleteAlarmSetup(int id) throws Exception {
    ArrayList<AlarmSetup> ass = getAlarmSetups();

    if(ass.get(id) == null) return false; // Non existing id

    Log.w(TAG, "Delete alarm setup: " + id);

    // Remove the exisiting setup
    ass.remove(id);

    // Write it all back to the state
    setAlarmSetups(ass);

    return true;
  }

  private void setAlarmSetups(ArrayList<AlarmSetup> ass){
    getState().put(STATE_FIELD_ALARM_SETUPS, ass);
  }


  // Alarm Setups <-> Capcodes
  public HashMap<String, String> getCapcodesLinkAlarmSetups() throws Exception {

    HashMap<String, String> l = getState().get(STATE_FIELD_ALARM_SETUPS_LINK_CAPCODE, new TypeUtil<HashMap<String, String>>() {});
    if(l==null) return new HashMap<String, String>();
    return l;

  }

  public boolean addCapcodeLinkAlarmSetup(AlarmSetup as, String capcode) throws Exception {
    HashMap<String, String> links = getCapcodesLinkAlarmSetups();

    // Create new link or overwrite exisiting
    links.put(capcode, as.getName());

    // Write it all back to the state
    setCapcodesLinkAlarmSetups(links);

    return true;
  }

  /*
  // NOTE: Currently unused; the value can only be overwritten by (e.g. setting an empty Alarm Setup)
  public boolean deleteCapcodeLinkAlarmSetup(String alarmSetupName, String capcode) throws Exception {
    HashMap<String, String> links = getCapcodesLinkAlarmSetups();

    if(links.containsKey(capcode) == null) return false; // Non existing capcode link

    Log.w(TAG, "Delete capcode link alarm setup: " + alarmSetupName + " for capcode: " + capcode);

    // Remove the exisiting setup
    links.remove(capcode);

    // Write it all back to the state
    setCapcodesLinkAlarmSetups(links);

    return true;
  }
  */

  private void setCapcodesLinkAlarmSetups(HashMap<String, String> links){
    getState().put(STATE_FIELD_ALARM_SETUPS_LINK_CAPCODE, links);
  }



  private static final String REMOTE_SUBSCRIBE_FUNCTION = "subscribeCapcode";
  public boolean subscribe(String capcode) throws Exception {

    // Create a proxy to the P2000 agent
    AsyncProxy<P2000AgentIntf> p2000Agent = getAsyncP2000Agent();

    Log.w(TAG, "Subscribing " + getXmppUrl() + " for capcode " + capcode + " on function " + P2000_MESSAGE_HANDLER);

    Future<?> asyncCall = p2000Agent.call(REMOTE_SUBSCRIBE_FUNCTION, capcode, getXmppUrl(), P2000_MESSAGE_HANDLER);
    asyncCall.get(); // TODO: Add timeouts: EveService.PROXY_TIMEOUT_INTERVAL, EveService.PROXY_TIMEOUT_TIMEUNIT);

    return true;

  }

  private static final String REMOTE_UNSUBSCRIBE_FUNCTION = "unsubscribeCapcode";
  public boolean unsubscribe(String capcode) throws Exception {

    // Create a proxy to the P2000 agent
    AsyncProxy<P2000AgentIntf> p2000Agent = getAsyncP2000Agent();

    Log.w(TAG, "Unsubscribe " + getXmppUrl() + " from capcode " + capcode + " on function " + P2000_MESSAGE_HANDLER);

    Future<?> asyncCall = p2000Agent.call(REMOTE_UNSUBSCRIBE_FUNCTION, capcode, getXmppUrl(), P2000_MESSAGE_HANDLER);
    asyncCall.get(); // TODO: Add timeouts: EveService.PROXY_TIMEOUT_INTERVAL, EveService.PROXY_TIMEOUT_TIMEUNIT);

    return true;

  }


  /* Getting remote agents */

  /* P2000 Agent */
  protected AsyncProxy getAsyncP2000Agent() throws Exception {

    // Get the domainagent
    DomainAgentIntf d = (DomainAgentIntf) getDomainAgent();

    // Create a proxy to the P2000 agent via the P2000 agent url from the Domainagent
    String p2000AgentUrl = d.getP2000AgentUrl();

    return getAgentHost().createAsyncAgentProxy(this, URI.create( p2000AgentUrl ), P2000AgentIntf.class);
  }

  /* Personal Agent */
  protected String getPersonalAgentUrl() throws Exception {
    String url = getState().get( "personalAgentUrl", String.class );
    if(url==null) {
      List<DataSource> dss = this.findDataSource( this.getUsername(), "cloud" );
      for(DataSource ds : dss)
      {
        url = ds.getAgentUrl();
        getState().put("personalAgentUrl", url);
        return url;
      }
    }

    return url;
  }

  public PersonalAgentIntf getPersonalAgent() throws Exception {
    return this.getAgentHost().createAgentProxy( this, URI.create(getPersonalAgentUrl()), PersonalAgentIntf.class );
  }

  /* Domain Agent */
  protected String getDomainAgentUrl() throws Exception {
    String url = getState().get("domainAgentUrl", String.class);
    if(url==null) {
      List<DataSource> dss = this.findDataSource( getPersonalAgent().getDomainAgentId(), "domain" );
      for(DataSource ds : dss)
      {
        url = ds.getAgentUrl();
        getState().put("domainAgentUrl", url);
        return url;
      }
    }

    return url;
  }

  public DomainAgentIntf getDomainAgent() throws Exception {
    return this.getAgentHost().createAgentProxy( this, URI.create(getDomainAgentUrl()), DomainAgentIntf.class );
  }

}
