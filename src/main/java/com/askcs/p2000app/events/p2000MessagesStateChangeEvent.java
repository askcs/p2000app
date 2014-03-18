package com.askcs.p2000app.events;

/**
 * Created by Jordi on 27-2-14.
 */
public class p2000MessagesStateChangeEvent {

  // TODO: Possibly create an generic class with the string and/or boolean state which these classes can extend from

  private String	state;

  public p2000MessagesStateChangeEvent(String state) {
    setState(state);
  }

  public String getState() {
    return this.state;
  }

  public void setState(String state) {
    this.state = state;
  }

}
