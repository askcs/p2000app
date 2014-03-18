package com.askcs.p2000app.entities;

import java.io.Serializable;

/**
 * Created by Jordi on 6-3-14.
 */
public class CapcodeSubscription implements Comparable {

  static final long serialVersionUID = 8070196401188864652L;

  private String capcode = null;
  private String asName = null;

  public CapcodeSubscription(String capcode, String asName) {
    setCapcode(capcode);
    setAsName(asName);
  }

  public String getCapcode() {
    return capcode;
  }

  public void setCapcode(String capcode) {
    this.capcode = capcode;
  }

  public String getAsName() {
    return asName;
  }

  public void setAsName(String asName) {
    this.asName = asName;
  }

  //
  public String toString(){
    if(getAsName() != null && !getAsName().equals("")){
      return getCapcode() + " (AlarmSetup: "+getAsName()+")";
    } else {
      return getCapcode();
    }
  }

  @Override
  public int compareTo(Object another) {
    return this.getCapcode().compareTo( ((CapcodeSubscription) another).getCapcode());
  }
}
