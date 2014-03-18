package com.askcs.p2000app.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: Jordi
 * Date: 17-10-13
 * Time: 10:38
 * To change this template use File | Settings | File Templates.
 */
public class Cryptography {

  /**
   * Get the MD5 hash of a given string
   * @param String str
   * @return String hash
   */
  public static String md5(String str){

    MessageDigest m = null;
    try {
      m = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }

    m.update(str.getBytes(), 0, str.length());
    String hash = new BigInteger(1, m.digest()).toString(16);
    return hash;
  }
}
