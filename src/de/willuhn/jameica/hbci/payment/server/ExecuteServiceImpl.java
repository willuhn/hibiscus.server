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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.List;

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.ExecuteService;
import de.willuhn.jameica.hbci.payment.web.beans.Welcome;
import de.willuhn.jameica.hbci.synchronize.SynchronizeBackend;
import de.willuhn.jameica.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.hbci.synchronize.jobs.SynchronizeJob;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.QueryMessage;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.OperationCanceledException;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;
import de.willuhn.util.ProgressMonitor;

/**
 * Implementierung des Services zur Durchfuehrung der Geschaeftsvorfaelle.
 */
public class ExecuteServiceImpl extends UnicastRemoteObject implements ExecuteService
{
  private final static I18N i18n   = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  private boolean started                   = false;
  private MessageConsumer mc                = new MyMessageConsumer();
  private Iterator<SynchronizeBackend> list = null;

  
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
    if (list != null || new Welcome().isRunning())
    {
      Logger.warn("synchronization already running, cancel current run");
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("Synchronisierung läuft bereits"),StatusBarMessage.TYPE_ERROR));
      return;
    }
    
    try
    {
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("Synchronisierung läuft"),StatusBarMessage.TYPE_SUCCESS));
      
      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
      SynchronizeEngine engine = service.get(SynchronizeEngine.class);
      List<SynchronizeBackend> backends = engine.getBackends();
      this.list = backends.iterator();

      Logger.info("synchronizing " + backends.size() + " backends");
      
      // Auf die Events registrieren, um die Folge-Backends zu starten
      Application.getMessagingFactory().getMessagingQueue(SynchronizeBackend.QUEUE_STATUS).registerMessageConsumer(this.mc);

      this.sync();
    }
    catch (RuntimeException re)
    {
      done();
      throw re;
    }
  }

  /**
   * Startet den naechsten Durchlauf.
   * @throws ApplicationException
   */
  private void sync()
  {
    if (!this.list.hasNext())
    {
      done();
      Logger.info("no more backends. synchronization done");
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("Synchronisierung beendet"),StatusBarMessage.TYPE_SUCCESS));
      return;
    }
    
    try
    {
      // Sonst naechste Iteration starten
      SynchronizeBackend backend = this.list.next();
      List<SynchronizeJob> jobs = backend.getSynchronizeJobs(null);
      Logger.info("synchronizing backend " + backend.getName() + " with " + jobs.size() + " jobs");
      backend.execute(jobs);
    }
    catch (ApplicationException ae)
    {
      done();
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(ae.getMessage(),StatusBarMessage.TYPE_ERROR));
    }
    catch (OperationCanceledException oce)
    {
      done();
      Application.getMessagingFactory().sendSyncMessage(new StatusBarMessage(i18n.tr("Synchronisierung abgebrochen"),StatusBarMessage.TYPE_ERROR));
    }
  }
  
  /**
   * Uebernimmt Aufraeumarbeiten nach Beendigung der Synchronisierung.
   */
  private void done()
  {
    this.list = null;
    Application.getMessagingFactory().getMessagingQueue(SynchronizeBackend.QUEUE_STATUS).unRegisterMessageConsumer(this.mc);
  }
  
  /**
   * Wird ueber die Status-Events der Backends benachrichtigt und startet dann das naechste
   */
  private class MyMessageConsumer implements MessageConsumer
  {
    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
     */
    public Class[] getExpectedMessageTypes()
    {
      return new Class[]{QueryMessage.class};
    }

    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
     */
    public void handleMessage(Message message) throws Exception
    {
      QueryMessage msg = (QueryMessage) message;
      Object data = msg.getData();
      if (!(data instanceof Integer))
      {
        Logger.warn("got unknown data: " + data);
        return;
      }
      
      int status = ((Integer) data).intValue();
      switch (status)
      {
        case ProgressMonitor.STATUS_DONE:
          sync(); // Erfolgreich. Weiter zum naechsten Backend
          break;
          
        case ProgressMonitor.STATUS_CANCEL:
        case ProgressMonitor.STATUS_ERROR:
          done(); // Fehler. Aufhoeren
          break;
      }
    }

    /**
     * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
     */
    public boolean autoRegister()
    {
      return false;
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
    this.list = null;
  }

}
