/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Welcome.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import javax.servlet.http.HttpServletResponse;

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.ExecuteService;
import de.willuhn.jameica.hbci.payment.rmi.SchedulerService;
import de.willuhn.jameica.hbci.server.hbci.HBCIFactory;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Response;

/**
 * Controller-Bean fuer die Startseite.
 */
public class Welcome
{
  @Response
  private HttpServletResponse response = null;
  
  /**
   * Action zum manuellen Ausfuehren der faelligen Jobs.
   * @throws Exception
   */
  public void execute() throws Exception
  {
    ExecuteService es = (ExecuteService) Application.getServiceFactory().lookup(Plugin.class,"execute");
    es.run();
  }
  
  /**
   * Action zum Starten des Scheduler-Services.
   * @throws Exception
   */
  public void start() throws Exception
  {
    SchedulerService s = (SchedulerService) Application.getServiceFactory().lookup(Plugin.class,"scheduler");
    if (s.isStartable())
      s.start();
    
    // Wir senden ein Redirect, um den Action-Parameter aus der URL zu entfernen
    response.sendRedirect("index.html");
  }
  
  /**
   * Liefert true, wenn Synchronisierungsjobs vorhanden sind.
   * @return true, wenn ynchronisierungsjobs vorhanden sind.
   */
  public boolean haveJobs()
  {
    return (new Jobs().getJobs() != null);
  }
  
  /**
   * Liefert true, wenn die Synchronisierung gerade laeuft.
   * @return true, wenn die Synchronisierung gerade laeuft.
   */
  public boolean isRunning()
  {
    return HBCIFactory.getInstance().inProgress();
  }

}



/**********************************************************************
 * $Log: Welcome.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.8  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.7  2011/04/11 08:55:46  willuhn
 * @N TICKET #128
 *
 * Revision 1.6  2010/11/04 18:17:53  willuhn
 * @B Lizenz-Upload wurde auch dann noch angezeigt, wenn sie schon hochgeladen wurde, aber die Zustimmung der Lizenzbedingungen noch fehlte
 *
 * Revision 1.5  2010/11/04 17:25:28  willuhn
 * @N Upload des Lizenzschluessels uebers Webfrontend
 *
 * Revision 1.4  2010/11/04 13:28:26  willuhn
 * @N Lizenzbedingungen muessen nun explizit im Browser akzeptiert werden
 *
 * Revision 1.3  2010/03/24 12:09:47  willuhn
 * @N GUI-Polishing
 *
 * Revision 1.2  2010/02/26 17:00:04  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 **********************************************************************/