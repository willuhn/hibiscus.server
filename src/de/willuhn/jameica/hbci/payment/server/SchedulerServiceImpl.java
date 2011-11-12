/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/server/SchedulerServiceImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Timer;

import de.willuhn.jameica.hbci.payment.Settings;
import de.willuhn.jameica.hbci.payment.messaging.SchedulerErrorMessageConsumer;
import de.willuhn.jameica.hbci.payment.rmi.SchedulerService;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Implementierung des Scheduler-Services.
 */
public class SchedulerServiceImpl extends UnicastRemoteObject implements SchedulerService
{
  private Timer timer        = null;
  private SchedulerTask task = null;
  private SchedulerErrorMessageConsumer mc = null;
  private long period        = 1;

  /**
   * ct.
   * @throws RemoteException
   */
  public SchedulerServiceImpl() throws RemoteException
  {
    super();
  }

  /**
   * @see de.willuhn.datasource.Service#getName()
   */
  public String getName() throws RemoteException
  {
    return "Scheduler-Service";
  }

  /**
   * @see de.willuhn.datasource.Service#isStartable()
   */
  public boolean isStartable() throws RemoteException
  {
    return !isStarted();
  }

  /**
   * @see de.willuhn.datasource.Service#isStarted()
   */
  public boolean isStarted() throws RemoteException
  {
    return this.timer != null;
  }

  /**
   * @see de.willuhn.datasource.Service#start()
   */
  public void start() throws RemoteException
  {
    if (isStarted())
    {
      Logger.warn("service allready started, skipping request");
      return;
    }
    
    this.mc = new SchedulerErrorMessageConsumer();
    Application.getMessagingFactory().getMessagingQueue("hibiscus.server.error").registerMessageConsumer(this.mc);
    
    Logger.info("starting scheduler service");
    int interval = Settings.getSchedulerInterval();
    Logger.info("scheduler interval: " + interval + " minutes");

    this.timer  = new Timer();
    this.task   = new SchedulerTask();
    this.period = interval * 60 * 1000l;
    
    timer.schedule(this.task,period,period);
  }

  /**
   * @see de.willuhn.datasource.Service#stop(boolean)
   */
  public void stop(boolean arg0) throws RemoteException
  {
    if (!isStarted())
    {
      Logger.warn("service not started, skipping request");
      return;
    }

    if (this.timer == null)
    {
      Logger.info("skip stop request. Scheduler not running");
      return;
    }

    try
    {
      if (this.mc != null)
        Application.getMessagingFactory().getMessagingQueue("hibiscus.server.error").unRegisterMessageConsumer(this.mc);

      Logger.info("stopping scheduler service");
      this.task.cancel();
      this.timer.cancel();
    }
    finally
    {
      this.task  = null;
      this.timer = null;
    }
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.rmi.SchedulerService#getNextExecution()
   */
  public Date getNextExecution() throws RemoteException
  {
    if (this.task == null)
      return null;

    long current = this.task.scheduledExecutionTime();
    Date d = null;
    // Wir suchen nach der naechsten Ausfuehrungszeit
    for (int i=0;i<10000;++i)
    {
      current += this.period;
      d = new Date(current);
      if (SchedulerTask.canRun(d))
        return d;
    }
    Logger.error("exclude window too large, scheduler will ne run");
    Application.getMessagingFactory().sendMessage(new StatusBarMessage("Zeitfenster für Ausschluss zu groß, Synchronisierung würde nie starten", StatusBarMessage.TYPE_ERROR));
    return null;
  }

}


/*********************************************************************
 * $Log: SchedulerServiceImpl.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.12  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.11  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 * Revision 1.10  2007/10/02 08:43:05  willuhn
 * #88
 *
 * Revision 1.9  2007/10/02 00:10:17  willuhn
 * @B Fehler in Ausschluss-Zeit
 *
 * Revision 1.8  2007/06/19 09:57:10  willuhn
 * @N Lizenz-Check
 *
 * Revision 1.7  2007/06/12 10:57:44  willuhn
 * #35
 * @N Konfigurierbarkeit eines Ausschluss-Zeitfensters
 *
 * Revision 1.6  2007/05/21 23:25:27  willuhn
 * *** empty log message ***
 *
 * Revision 1.5  2007/05/21 00:19:06  willuhn
 * @N key upload
 *
 * Revision 1.4  2007/05/16 16:49:42  willuhn
 * @N display ongoing hbci jobs
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