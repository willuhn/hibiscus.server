/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Jobs.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.util.Date;
import java.util.List;

import de.willuhn.datasource.pseudo.PseudoIterator;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.SchedulerService;
import de.willuhn.jameica.hbci.rmi.SynchronizeJob;
import de.willuhn.jameica.hbci.server.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

/**
 * Zeigt Infos zu den aktuellen Jobs an.
 */
public class Jobs
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  /**
   * Liefert Datum und Uhrzeit der naechsten planmaessigen Synchronisierung.
   * @return Datum und Uhrzeit der naechsten planmaessigen Synchronisierung.
   * Oder NULL, wenn der Scheduler-Service nicht laeuft oder es zu einem Fehler kam.
   */
  public String getNextExecution()
  {
    try
    {
      SchedulerService s = (SchedulerService) Application.getServiceFactory().lookup(Plugin.class,"scheduler");
      if (!s.isStarted())
        return null;

      Date d = s.getNextExecution();
      return d == null ? i18n.tr("NIE") : HBCI.LONGDATEFORMAT.format(d);
    }
    catch (Exception e)
    {
      Logger.error("unable to determine next execution time",e);
      return null; // Oder besser Exception werfen?
    }
  }
  
  /**
   * Liefert eine Liste der anstehenden Jobs.
   * @return Liste der anstehenden Jobs oder NULL keine keine Jobs vorliegen.
   */
  public List<SynchronizeJob> getJobs()
  {
    try
    {
      List<SynchronizeJob> list = PseudoIterator.asList(SynchronizeEngine.getInstance().getSynchronizeJobs());
      return list.size() > 0 ? list : null;
    }
    catch (Exception e)
    {
      Logger.error("unable to determine next execution time",e);
      return null; // Oder besser Exception werfen?
    }
  }
}



/**********************************************************************
 * $Log: Jobs.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.3  2010/03/24 12:09:47  willuhn
 * @N GUI-Polishing
 *
 * Revision 1.2  2010/02/26 17:00:04  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/