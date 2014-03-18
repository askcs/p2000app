package com.askcs.p2000app.events;

/**
 * Created by Jordi on 25-2-14.
 */
public class LoginStateChangeEvent {

    private String	state;

    public LoginStateChangeEvent(String state) {
      setState(state);
    }

    public String getState() {
      return this.state;
    }

    public void setState(String state) {
      this.state = state;
    }

}

