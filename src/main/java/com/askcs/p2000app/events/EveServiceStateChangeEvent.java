package com.askcs.p2000app.events;

/**
 * Created by Jordi on 26-2-14.
 */
public class EveServiceStateChangeEvent {

    private Boolean	state;

    public EveServiceStateChangeEvent(Boolean state) {
      setState(state);
    }

    public Boolean getState() {
      return this.state;
    }

    public void setState(Boolean state) {
      this.state = state;
    }


}
