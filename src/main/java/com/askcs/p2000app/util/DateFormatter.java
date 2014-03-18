package com.askcs.p2000app.util;

import android.content.Context;
import com.askcs.p2000app.R;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.text.DateFormat.MEDIUM;
import static java.text.DateFormat.SHORT;

/**
 * Some utilities for dealing with dates and times.
 */
public class DateFormatter {

  public static final DateFormat DATEFORMAT = DateFormat.getDateInstance( MEDIUM );
  public static final DateFormat TIMEFORMAT = DateFormat.getTimeInstance( SHORT );
  public  static final DateFormat TIMEFORMATFULL = DateFormat.getTimeInstance( MEDIUM );


  /**
   * Format a single point in time.
   * @param context
   * @param date
   * @return
   */
  static public String formatDateTime( Context context, Date date ) {
    return formatDay( context, date ) + ", "
        + TIMEFORMAT.format( date );
  }

  /**
   * Format an interval.
   * @param context
   * @param start
   * @param end
   * @return
   */
  static public String formatDateTime( Context context, Date start, Date end ) {
    String startDay = formatDay( context, start );
    String endDay = formatDay( context, end );

    String startTime = formatTime( start );
    String endTime = formatTime( end );

    // Add seconds to the time if the start date/time and end date/time are within the same minute
    if(startDay.equals(endDay) && startTime.equals(endTime)){
      startTime = formatTimeFull( start );
      endTime = formatTimeFull( end );
    }

    return formatDay( context, start ) + " " + startTime + " - "
        + (startDay.equals( endDay ) ? "" : endDay) + " " + endTime;
  }


  static public String formatFullTime( Date d) {
    return formatTimeFull( d );
  }
  
  /**
   * Format date (day) with special handling of yesterday, today, tomorrow
   */
  static public String formatDay( Context context, Date date ) {
    long stamp = System.currentTimeMillis();
    Date[] checks = new Date[] {
        new Date( stamp - 24L * 60L * 60L * 1000L ),
        new Date( System.currentTimeMillis() ),
        new Date( stamp + 24L * 60L * 60L * 1000L )
    };
    int[] nameResIds = new int[] {
        R.string.yesterday,
        R.string.today,
        R.string.tomorrow
    };
    Calendar calendar1 = Calendar.getInstance();
    calendar1.setTime( date );
    for ( int i = 0; i < checks.length; i++ ) {
      Calendar calendar2 = Calendar.getInstance();
      calendar2.setTime( checks[ i ] );
      if ( calendar1.get( Calendar.ERA ) == calendar2.get( Calendar.ERA )
          && calendar1.get( Calendar.YEAR ) == calendar2.get( Calendar.YEAR )
          && calendar1.get( Calendar.DAY_OF_YEAR ) == calendar2.get( Calendar.DAY_OF_YEAR ) ) {
        return context.getResources().getString(  nameResIds[ i ] );
      }
    }
    return DateFormatter.DATEFORMAT.format( date );
  }

  static public String formatTime( Date date ) {
    return TIMEFORMAT.format( date );
  }

  static public String formatTimeFull(Date date){
    return TIMEFORMATFULL.format( date );
  }

}
