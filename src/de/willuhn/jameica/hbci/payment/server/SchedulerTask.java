/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/server/SchedulerTask.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn.webdesign
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.server;

import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.Settings;
import de.willuhn.jameica.hbci.payment.rmi.ExecuteService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Enthaelt den Code, der zyklisch ausgefuehrt wird.
 */
public class SchedulerTask extends TimerTask
{
  /**
   * ct.
   */
  public SchedulerTask()
  {
    Logger.info("starting scheduler task");
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run()
  {
    try
    {
      if (!canRun(new Date()))
      {
        Logger.info("skip synchronize, between exclude time [" + Settings.getSchedulerExcludeFrom() + " - " + Settings.getSchedulerExcludeTo() + " 'o clock]");
        return;
      }
      
      ExecuteService execute = (ExecuteService) Application.getServiceFactory().lookup(Plugin.class,"execute");
      execute.run();
    }
    catch (Exception e)
    {
      Logger.error("error while executing scheduler task: " + e.getMessage(),e);
    }
  }
  
  /**
   * Prueft, ob der Aufruf innerhalb der Ausschluss-Zeit stattfindet.
   * @param check die zu pruefende Zeit.
   * @return true, wenn der Aufruf NICHT in der Ausschluss-Zeit stattfindet und laufen darf.
   */
  static boolean canRun(Date check)
  {
    Calendar cal = Calendar.getInstance();
    cal.setTime(check);

    // Ausschluss-Tag?
    if (Settings.getSchedulerExcludeDay(cal.get(Calendar.DAY_OF_WEEK)))
      return false;

    // Uhrzeit checken
    int from = Settings.getSchedulerExcludeFrom();
    int to   = Settings.getSchedulerExcludeTo();

    // Von/Bis ist identisch. Dann setzt er nachts nie aus
    if (from == to)
      return true;

    // Checken, ob wir uns innerhalb des Ausschluss-Fensters befinden
    int run = cal.get(Calendar.HOUR_OF_DAY);
    if (to < from) // Tageswechsel dazwischen?
      return run < from && run >= to;

    return run < from || run >= to;
  }
}


/*********************************************************************
 * $Log: SchedulerTask.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.13  2010/05/17 12:44:30  willuhn
 * @N Einzelne Wochentage koennen nun von der Synchronisierung ausgeschlossen werden. Ist z.Bsp. sinnvoll, wenn die Bank am Wochenende eher schlecht/gar nicht erreichbar ist
 *
 * Revision 1.12  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 * Revision 1.11  2008/09/23 12:23:30  willuhn
 * @C Stacktrace in Message mitsenden
 *
 * Revision 1.10  2007/10/02 08:42:36  willuhn
 * #88
 *
 * Revision 1.9  2007/10/02 00:10:17  willuhn
 * @B Fehler in Ausschluss-Zeit
 *
 * Revision 1.8  2007/08/31 13:33:35  willuhn
 * @B Tageswechsel bei Ausschluss-Zeit besser beruecksichtigen
 * @N TAN-Geschaeftsvorfaelle bei PIN/TAN ausblenden
 *
 * Revision 1.7  2007/06/12 10:57:44  willuhn
 * #35
 * @N Konfigurierbarkeit eines Ausschluss-Zeitfensters
 *
 * Revision 1.6  2007/06/05 13:34:16  willuhn
 * @B typo
 *
 * Revision 1.5  2007/06/05 13:09:02  willuhn
 * @N Message im Fehlerfall versenden
 * @N Scheduler im Fehlerfall anhalten
 *
 * Revision 1.4  2007/06/04 15:12:07  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2007/05/16 16:26:04  willuhn
 * @N Webfrontend
 *
 * Revision 1.2  2007/05/16 14:16:43  willuhn
 * @N nextExecutionTime
 * @N Scheduler cleanup
 *
 * Revision 1.1  2007/05/15 17:21:08  willuhn
 * @N Added Scheduler
 *
 **********************************************************************/