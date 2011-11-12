/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/server/ExecuteServiceImpl.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import de.willuhn.datasource.GenericIterator;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.ExecuteService;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.SynchronizeJob;
import de.willuhn.jameica.hbci.server.hbci.AbstractHBCIJob;
import de.willuhn.jameica.hbci.server.hbci.HBCIFactory;
import de.willuhn.jameica.hbci.server.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.messaging.TextMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;
import de.willuhn.util.ProgressMonitor;

/**
 * Implementierung des Services zur Durchfuehrung der Geschaeftsvorfaelle.
 */
public class ExecuteServiceImpl extends UnicastRemoteObject implements ExecuteService
{
  private final static I18N i18n   = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  private boolean started          = false;
  
  private GenericIterator accounts = null;
  private boolean success          = false;

  
  /**
   * ct.
   * @throws RemoteException
   */
  public ExecuteServiceImpl() throws RemoteException
  {
    super();
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.rmi.ExecuteService#run()
   */
  public void run() throws RemoteException
  {
    try
    {
      if (HBCIFactory.getInstance().inProgress())
      {
        Logger.warn("another hbci job is allready running, cancel current run");
        Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("Es läuft bereits eine HBCI-Synchronisierung"),StatusBarMessage.TYPE_ERROR));
        return;
      }
      this.accounts = SynchronizeEngine.getInstance().getSynchronizeKonten();
      Logger.info("accounts to synchronize: " + this.accounts.size());
      success = true;
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("HBCI-Synchronisierung gestartet"),StatusBarMessage.TYPE_SUCCESS));
      sync(ProgressMonitor.STATUS_NONE);
    }
    catch (RemoteException re)
    {
      error(re);
    }
  }

  /**
   * Fehlerbehandlung.
   * @param e optionaler Fehler.
   */
  private synchronized void error(Exception e)
  {
    Logger.error("error while synchronizing",e);

    try
    {
      StringBuffer text = new StringBuffer();
      if (e != null)
      {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        writer.flush();
        text.append(writer.toString());
      }

      TextMessage t = new TextMessage(i18n.tr("HBCI-Synchronisierung mit Fehler beendet. Bitte Logs prüfen"),text.toString());
      Application.getMessagingFactory().getMessagingQueue("hibiscus.server.error").sendMessage(t);
    }
    finally
    {
      this.accounts = null;
    }
  }

  /**
   * Sendet die HBCI-Jobs.
   * @param lastStatus der Statuscode des vorherigen Durchlaufs.
   */
  private void sync(int lastStatus)
  {
    // Globalen Status merken
    success &= (lastStatus == ProgressMonitor.STATUS_DONE || lastStatus == ProgressMonitor.STATUS_NONE);

    // Bei Abbruch brechen wir immer ab ;)
    if (lastStatus == ProgressMonitor.STATUS_CANCEL)
    {
      Logger.info("synchronize cancelled");
      return;
    }
    
    if (!success)
    {
      error(null);
      return;
    }

    try
    {
      if (this.accounts == null || !this.accounts.hasNext())
      {
        Logger.info("synchronize run finished");
        return;
      }
      
      Logger.info("creating hbci factory");
      HBCIFactory factory = HBCIFactory.getInstance();

      final Konto konto = (Konto) this.accounts.next();
      GenericIterator list = SynchronizeEngine.getInstance().getSynchronizeJobs(konto);
      int count = 0;
      while (list.hasNext())
      {
        SynchronizeJob sj = (SynchronizeJob) list.next();
        
        AbstractHBCIJob[] currentJobs = sj.createHBCIJobs();
        if (currentJobs != null)
        {
          for (int i=0;i<currentJobs.length;++i)
          {
            factory.addJob(currentJobs[i]);
          }
        }
        count++;
      }

      if (count == 0)
      {
        Logger.info("nothing to do for account " + konto.getLongName() + " - skipping");
        sync(ProgressMonitor.STATUS_NONE);
      }
      else
      {
        factory.executeJobs(konto,new Listener() {
          public void handleEvent(Event event)
          {
            sync(event.type);
          }
        });
      }
    }
    catch (Exception e)
    {
      error(e);
    }
  }
  
  
  /**
   * @see de.willuhn.datasource.Service#getName()
   */
  public String getName() throws RemoteException
  {
    return "Execute-Service";
  }

  /**
   * @see de.willuhn.datasource.Service#isStartable()
   */
  public boolean isStartable() throws RemoteException
  {
    return !this.isStarted();
  }

  /**
   * @see de.willuhn.datasource.Service#isStarted()
   */
  public boolean isStarted() throws RemoteException
  {
    return this.started;
  }

  /**
   * @see de.willuhn.datasource.Service#start()
   */
  public void start() throws RemoteException
  {
    if (this.isStarted())
    {
      Logger.warn("service allready started, skipping request");
      return;
    }
    this.started = true;
  }

  /**
   * @see de.willuhn.datasource.Service#stop(boolean)
   */
  public void stop(boolean arg0) throws RemoteException
  {
    if (!this.isStarted())
    {
      Logger.warn("service not started, skipping request");
      return;
    }
    this.started = false;
  }

}



/**********************************************************************
 * $Log: ExecuteServiceImpl.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.3  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.2  2010/02/24 17:54:26  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 **********************************************************************/