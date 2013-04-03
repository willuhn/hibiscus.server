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
import de.willuhn.jameica.hbci.synchronize.SynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.services.BeanService;
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
    BeanService service = Application.getBootLoader().getBootable(BeanService.class);
    SynchronizeEngine engine = service.get(SynchronizeEngine.class);
    for (SynchronizeBackend backend:engine.getBackends())
    {
      if (backend.getCurrentSession() != null)
        return true;
    }
    return false;
  }
}
