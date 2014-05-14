package com.askcs.p2000app.util;

import android.graphics.Color;
import org.omg.CORBA.UNKNOWN;

/**
 * Created by Jordi on 5-3-14.
 */
public class SimpleP2000Processor {

  public enum EmergencyService {
    AMBULANCE,
    FIREDEPARTMENT,
    LIFELINER,
    POLICE,
    KNRM, // Not detected, will be marked as FIREDEPARTMENT due to 'prio ' use
    TEST,
    UNKNOWN
  }

  public static EmergencyService getService(String msg){

    // Basic checks based on words. Real message + service matching should be done serverside based on the capcode

    // Full message uppercase
    msg = msg.toUpperCase();

    if(msg.contains(" LFL") || msg.contains("MMT")){ // At the top, because lifeliners also have 'AMBU' in their message sometimes
      return EmergencyService.LIFELINER;
    }

    if(msg.contains(" AMBU ")){
      return EmergencyService.AMBULANCE;
    }

    if(msg.contains("PRIO ")){
      return EmergencyService.FIREDEPARTMENT;
    }

    if(msg.contains(" TEST") || msg.contains("TEST ")){
      return EmergencyService.TEST;
    }

    if(msg.contains(" AANRIJDING") || msg.contains("AANRIJDING ") || msg.contains("LETSEL ") || msg.contains(" LETSEL")){
      return EmergencyService.POLICE;
    }

    // Default unknown
    return EmergencyService.UNKNOWN;
  }

  public static int getServiceColor(EmergencyService es){

    if(es.equals(EmergencyService.AMBULANCE)){
      return Color.YELLOW;
    }

    if(es.equals(EmergencyService.FIREDEPARTMENT)){
      return Color.RED;
    }

    if(es.equals(EmergencyService.POLICE)){
      return Color.BLUE;
    }

    if(es.equals(EmergencyService.LIFELINER)){
      return Color.GREEN;
    }

    // Default to GRAY
    return Color.GRAY;
  }

  public enum EmergencyPriority {
    HIGH,
    NORMAL,
    LOW,
    UNKNOWN
  }


  public static EmergencyPriority getPriority(String msg){

    // Basic checks based on words. Real priority detectinon should be done serverside

    if(msg.contains("PRIO 1") || msg.contains("A1") || msg.contains("GRIP")){
      return EmergencyPriority.HIGH;
    }

    if(msg.contains("PRIO 2") || msg.contains("A2")){
      return EmergencyPriority.NORMAL;
    }

    if(msg.contains("PRIO 3") || msg.contains("B1") || msg.contains("B2")){
      return EmergencyPriority.LOW;
    }

    if(msg.contains("TEST") || msg.contains("POSTEN ")){
      return EmergencyPriority.UNKNOWN;
    }

    // Default unknown
    return EmergencyPriority.UNKNOWN;
  }

}
