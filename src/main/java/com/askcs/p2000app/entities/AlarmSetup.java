package com.askcs.p2000app.entities;

import java.io.Serializable;

/**
 * Created by Jordi on 6-3-14.
 */
public class AlarmSetup implements Serializable {

  static final long serialVersionUID = 9070196401188864652L;

  private String name = null;
  private String filterWord = null;

  private Boolean sound = null;
  //private String soundFile = null;

  private boolean notification = false;
  //private String notificationTextTemplate = null;

  private boolean vibrate = false;
  //private int[] vibratePattern = null;

  private boolean blinkLED = false;
  //private int[] blinkPattern = null;

  private boolean turnOnScreen = false;

  public AlarmSetup(
          String name,
          String filterWord,
          boolean sound,
          boolean notification,
          boolean vibrate,
          boolean blinkLED,
          boolean turnOnScreen) {

    setName(name);
    setFilterWord(filterWord);
    setSound(sound);
    setNotification(notification);
    setVibrate(vibrate);
    setBlinkLED(blinkLED);
    setTurnOnScreen(turnOnScreen);

  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getFilterWord() { return filterWord; }

  public void setFilterWord(String filterWord) { this.filterWord = filterWord; }

  public Boolean getSound() {
    return sound;
  }

  public void setSound(Boolean sound) {
    this.sound = sound;
  }

  public boolean getNotification() {
    return notification;
  }

  public void setNotification(boolean notification) {
    this.notification = notification;
  }

  public boolean getVibrate() {
    return vibrate;
  }

  public void setVibrate(boolean vibrate) {
    this.vibrate = vibrate;
  }

  public boolean getBlinkLED() {
    return blinkLED;
  }

  public void setBlinkLED(boolean blinkLED) {
    this.blinkLED = blinkLED;
  }

  public boolean getTurnOnScreen() {
    return turnOnScreen;
  }

  public void setTurnOnScreen(boolean turnOnScreen) {
    this.turnOnScreen = turnOnScreen;
  }

  // Used for the Spinner with AlarmSetups
  public String toString(){
    if(getFilterWord() != null && !getFilterWord().equals("")){
      return getName() + " (Filter: "+getFilterWord()+")";
    } else {
      return getName();
    }
  }
}
