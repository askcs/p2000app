package com.askcs.p2000app.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.almende.eve.agent.AgentHost;
import com.almende.eve.agent.callback.AsyncCallback;
import com.almende.eve.scheduler.ClockSchedulerFactory;
import com.almende.eve.state.FileStateFactory;
import com.almende.eve.transport.xmpp.XmppService;
import com.almende.util.ClassUtil;
import com.askcs.commons.agent.AskAgent;
import com.askcs.commons.entity.P2000Message;
import com.askcs.p2000app.R;
import com.askcs.p2000app.agent.MobileAgent;
import com.askcs.p2000app.app.LoaderActivity;
import com.askcs.p2000app.app.LoginActivity;
import com.askcs.p2000app.app.MainActivity;
import com.askcs.p2000app.events.LoginEvent;
import com.askcs.p2000app.events.LoginStateChangeEvent;
import com.askcs.p2000app.util.BusProvider;
import com.squareup.otto.Subscribe;
import org.apache.http.MethodNotSupportedException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by Jordi on 24-2-14.
 */
@SuppressWarnings({"unused", "JavaDoc"})
public class EveService extends BaseEveService {

  /**
   * Tag for logging.
   */
  static private final String TAG = EveService.class.getCanonicalName();

  /**
   * Service thread
   */
  public static final HandlerThread serviceHandlerThread = new HandlerThread(EveService.class.getCanonicalName());

  /**
   * (Global) Agent host
   */
  static private AgentHost _ah = null;

  /**
   * Context
   */
  private static Context _ctx;

  /**
   * (Global) Our main agent.
   */
  static public MobileAgent _a = null;

  @Override
  public void onCreate() {
    Log.d( TAG, "onCreate" );
    super.onCreate();

    // Register on the BusProvider
    BusProvider.getBus().register(this);

  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d( TAG, "onDestroy" );
  }

  @Override
  public int onStartCommand( Intent intent, int flags, int startId ) {
    Log.d(TAG, "onStartCommand");

    // Only start it if it's currently not alive
    if(serviceHandlerThread != null && !serviceHandlerThread.isAlive()){
      serviceHandlerThread.start();
    }

    try {
      initHost( getApplication() );
    } catch (Exception e) {
      Log.w(TAG, "Could not call initHost from onStartCommand");
      e.printStackTrace();
    }

    return START_STICKY;
  }

  /* Binding to this servive */
  private final IBinder eveBinder = new EveServiceBinder();      // interface for clients that bind
  private boolean mAllowRebind; // indicates whether onRebind should be used

  public class EveServiceBinder extends Binder {
    public EveService getService() {
      return EveService.this;
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    // A client is binding to the service with bindService()
    return eveBinder;
  }
  @Override
  public boolean onUnbind(Intent intent) {
    // All clients have unbound with unbindService()
    return mAllowRebind;
  }
  @Override
  public void onRebind(Intent intent) {
    // A client is binding to the service with bindService(),
    // after onUnbind() has already been called
  }

  /**
   * Initialize our AgentHost
   * @param context
   */

  public static String  MOBILE_AGENT =                "MobileAgent_"; // TODO: Change this to fit your specific local agent name
  public static String  MOBILE_AGENT_RESOURCE =       "android";
  private static String AGENTS_DIR =                  "/.eveagents";
  private static String XMPP_SERVER_HOST =            "xmpp.ask-cs.com";
  private static int    XMPP_SERVER_PORT =            5222;

  // Same as the ones in strings.xml
  public static String XMPP_USERNAME_KEY =           "xmpp_username_key";
  public static String XMPP_PASSWORD_KEY =           "xmpp_password_key";
  public static String XMPP_ORIGINAL_PASSWORD_KEY =  "xmpp_original_password_key";

  private final EveService self =                     this;

  // Make this a class property, otherwise the XMPP connection won't automatically reconnect (asmack bug) [Keep a reference to the static call]
  // Info smack bug: http://community.igniterealtime.org/thread/48139
  // Take the variable outside the runnable; based on Standby (where we used this method without knowing)
  private XmppService xmppService;

  private Handler serviceThreadHandler;
  private void initHost( final Context context ) {

    _ah = AgentHost.getInstance();

    // Save context
    _ctx = context;

    serviceThreadHandler = new Handler(serviceHandlerThread.getLooper());
    serviceThreadHandler.post(new Runnable() {
      public void run() {
        System.err.println("Eve Service ThreadId: " + Thread.currentThread().getId());

        // Give the MobileAgent access to the app context
        MobileAgent.setContext(_ctx);

        try {

          Map<String, Object> params = new HashMap<String, Object>();
          params.put("path", _ctx.getFilesDir().getAbsolutePath() + AGENTS_DIR);
          _ah.setStateFactory(new FileStateFactory(params));

        } catch (Exception e) {
          System.err.println("Couldn't set StateFactory");
          e.printStackTrace();
        }

        // Add XMPP as transport service
        xmppService = new XmppService(_ah, XMPP_SERVER_HOST, XMPP_SERVER_PORT, XMPP_SERVER_HOST);
        _ah.addTransportService(xmppService);

        // Set the ClockScedularFactory as the SchedulerFactory
        _ah.setSchedulerFactory(new ClockSchedulerFactory(_ah, new HashMap<String, Object>()));


				/* Init Mobile Agent */
        // Try to re-connect the exisiting agent if there is data
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String username =         sp.getString(EveService.XMPP_USERNAME_KEY, "");
        String password =         sp.getString(EveService.XMPP_PASSWORD_KEY, "");
        String originalpassword = sp.getString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, "");

        // Attempt to login with these credentials
        if(username != "" && password != ""){
           self.connect( new LoginEvent(username, password, originalpassword) );
        }


      }
    });

  }

  /* Referer login states */
  public static final String LOGIN_STATE_FIELDNAME =          "loginrefer";

  public static final String LOGIN_STATE__SUCCESS =           "loginsuccessful";
  public static final String LOGIN_STATE__FAILED =            "loginfailed";
  public static final String LOGIN_STATE__FAILED_RECONNECT =  "loginfailedreconnect";
  public static final String LOGIN_STATE__FAILED_INIT_AGENT = "loginfailedinitagent";
  public static final String LOGIN_STATE__NOCREDENTIALS =     "loginnocredetials";
  public static final String LOGIN_STATE__LOGOUT =            "logout";


  @Subscribe
  public void connect(LoginEvent le){

    Log.w(TAG, "[EveServive] Connect LoginEvent");

    // If they are set, try to find
    if(!le.getUsername().equals("") && !le.getPassword().equals("")){

      final LoginEvent fle = le;

      // Construct agent name for this user
      final String agentName = EveService.MOBILE_AGENT + le.getUsername();

      Log.e(TAG, "Trying to find MobileAgent: " + agentName);

      serviceThreadHandler.post(new Runnable() {
        public void run() {

          // Try to get the agenthost and check if this user already has an agent
          boolean agentCheck = false;
          AgentHost ah = null;

          try{

            ah = AgentHost.getInstance();
            agentCheck = ah.hasAgent(agentName);

          }catch(Exception e){

            Log.w(TAG, "Could not get the agenthost or check if it has agent: " + agentName);
            e.printStackTrace();

            // Don't send this one; It will try something else (create agent) and send an event if that didn't work
            //BusProvider.getBus().post( new LoginStateChangeEvent( EveService.LOGIN_STATE__FAILED ) );

          }

          if(agentCheck){

            Log.e(TAG, "MobileAgent: " + agentName + " exists, reconnecting...");

            try{
              // Agent already exists, get and reconnect it if the given password is correct
              _a = (MobileAgent) ah.getAgent(agentName);

              String agentPassword = _a.getPassword();
              String inputPassword = fle.getPassword();
              if(agentPassword.equals(inputPassword) ){

                // Now reconnect, only if we are not already connected
                if(!_a.isConnected()){
                  Log.e(TAG, "MobileAgent reconnecting...");
                  _a.reconnect();
                }

                BusProvider.getBus().post( new LoginStateChangeEvent( EveService.LOGIN_STATE__SUCCESS ) );
                startForeground( fle.getUsername() );

               } else {

                Log.e(TAG, "Failed to reconnect MobileAgent " + agentName + " due to a wrong password. [" + _a.getPassword() + " / " + fle.getPassword() + "]");
                throw new Exception("Failed to reconnect MobileAgent " + agentName + " due to a wrong password. [" + _a.getPassword() + " / " + fle.getPassword() + "]");

              }

            } catch (Exception e){
              Log.w(TAG, "Failed to reconnect the existing agent ["+agentName+"] for this user");
              e.printStackTrace();
              BusProvider.getBus().post( new LoginStateChangeEvent( EveService.LOGIN_STATE__FAILED_RECONNECT ) );
            }

          } else {

            Log.e(TAG, "MobileAgent: " + agentName + " doesn't exist: create, setAccount and reconnect");

            try{

              // Agent doesn't exist (anymore), create and connect it
              _a = (MobileAgent) ah.createAgent(MobileAgent.class, agentName);
              _a.connect(fle.getUsername(), fle.getPassword(), fle.getOriginalPassword());

              BusProvider.getBus().post( new LoginStateChangeEvent( EveService.LOGIN_STATE__SUCCESS ) );

            } catch (Exception e){
              Log.w(TAG, "Failed to create and/or connect the new agent ["+agentName+"] for this user");
              e.printStackTrace();
              BusProvider.getBus().post( new LoginStateChangeEvent( EveService.LOGIN_STATE__FAILED_INIT_AGENT ) );
            }

          }

        }
      });

    }

  }

  // Run this service in the foreground; display long-running notification in the status bar
  private void startForeground(String username) {
    Log.e(TAG, "startForeground");

    Intent mainActivityIntent = new Intent( this, LoaderActivity.class );
    mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

    PendingIntent pi = PendingIntent.getActivity( this, 0 /* !? */, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT );

    Notification notification = new NotificationCompat.Builder( this )
            .setContentTitle( getResources().getString(R.string.foreground_service_notification_title) )
            .setContentText( getResources().getString(R.string.foreground_service_notification_text) + ": " + username )
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pi)
            .build();


    startForeground(1337, notification);
  }

   public void logout(){

    // Logout user/agent

    // Remove shared prefs passwords; keep username to display in the login screen
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(self);
    SharedPreferences.Editor spe = sp.edit();
    spe.putString(EveService.XMPP_PASSWORD_KEY, "");
    spe.putString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, "");
    spe.commit();

    String test = sp.getString(EveService.XMPP_ORIGINAL_PASSWORD_KEY, "");
    Log.w(TAG, "[TEST] String: " + test);

    // Logout in the EveService
    disconnect( new LoginStateChangeEvent(EveService.LOGIN_STATE__LOGOUT) );

    // Stop foreground (removed the status bar notification)
    stopForeground(true);

    // Redirect to the login screen
    Intent loginActivity = new Intent(self, LoginActivity.class);
    loginActivity.putExtra(EveService.LOGIN_STATE_FIELDNAME, EveService.LOGIN_STATE__LOGOUT);
    loginActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(loginActivity);

  }

  @Subscribe
  public void disconnect(LoginStateChangeEvent lsce){

    if(lsce.getState().equals( EveService.LOGIN_STATE__LOGOUT )){

      serviceThreadHandler.post(new Runnable() {
        public void run() {
          try{
            _a.logout();
            _a.disconnect();
          }catch(Exception e){
            Log.w(TAG, "Could not properly logout...");
            e.printStackTrace();
          }
        }
      });

    }

  }

  // Make the MobileAgent public for the EveService when its bound to an Activity
  public MobileAgent getMobileAgent(){
    return _a;
  }


  /*
  // Calls on the MobileAgent should be done trough this function
  public static final int PROXY_TIMEOUT_INTERVAL = 5;
  public static final TimeUnit PROXY_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;

  private final ExecutorService taskExecutor = Executors.newFixedThreadPool(4);
  private final CompletionService<Object> pool = new ExecutorCompletionService<Object>(taskExecutor);

  public Future<?> call(String functionName, final Object... args) throws NoSuchMethodException{
    ArrayList<Class> classes = new ArrayList<Class>(args.length);
    for (Object obj : args){
      classes.add(obj.getClass());
    }
    final Method method = ClassUtil.searchForMethod(_a.getClass(), functionName, classes.toArray(new Class[0]));

    return new DecoratedFuture(pool.submit(new Callable<Object>(){
      @Override
      public Object call() throws IllegalAccessException, InvocationTargetException {
        return method.invoke(_a, args);
      }
    }),ClassUtil.wrap(method.getReturnType()));

  }
  class DecoratedFuture<V> implements Future<V>{
    private Future<?> future;
    private Class<V> myType;

    DecoratedFuture(Future<?> future, Class<V> type){
      this.future=future;
      this.myType=type;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
      return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
      return future.isCancelled();
    }

    @Override
    public boolean isDone() {
      return future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
      return myType.cast(future.get());
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException,
            ExecutionException, TimeoutException {
      return myType.cast(future.get(timeout, unit));
    }


  }
  */

}
