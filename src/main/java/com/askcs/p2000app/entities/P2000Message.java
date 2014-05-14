package com.askcs.p2000app.entities;

import java.io.Serializable;

/**
 * Created by Jordi on 6-5-14.
 */
public class P2000Message implements Serializable {
  private static final long serialVersionUID = -6605373624491202308L;

  private long timestamp = 0;
  private long arrivalTimestamp = 0;
  private String message = null;
  private String capcode = null;

  public P2000Message(long timestamp, long arrivalTimestamp, String message, String capcode) {
    setTimestamp(timestamp);
    setArrivalTimestamp(arrivalTimestamp);
    setMessage(message);
    setCapcode(capcode);
  }

  public long getTimestamp() {
    return this.timestamp;
  }

  public long getArrivalTimestamp() {
    return this.arrivalTimestamp;
  }

  public String getMessage() {
    return this.message;
  }

  public String getCapcode() {
    return this.capcode;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  public void setArrivalTimestamp(long arrivalTimestamp) {
    this.arrivalTimestamp = arrivalTimestamp;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setCapcode(String capcode) {
    this.capcode = capcode;
  }

}
