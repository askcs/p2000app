package com.askcs.p2000app.events;

/**
 * Created by Jordi on 24-2-14.
 */
public class LoginEvent {

  private String	username =          "";
  private String  password =          "";
  private String  originalpassword =  "";

  public LoginEvent(String username, String password, String originalpassword) {
    setUsername(username);
    setPassword(password);
    setOriginalPassword(originalpassword);
  }

  public String getUsername() {
    return this.username;
  }

  public String getPassword() {
    return this.password;
  }

  public String getOriginalPassword() {
    return this.originalpassword;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setOriginalPassword(String originalpassword) {
    this.originalpassword = originalpassword;
  }

}

