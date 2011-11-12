/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/server/TANTestServiceImpl.java,v $
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

import de.willuhn.jameica.hbci.payment.rmi.TANTestService;
import de.willuhn.logging.Logger;

/**
 * TAN-Testservice.
 */
public class TANTestServiceImpl extends UnicastRemoteObject implements
    TANTestService
{
  
  private boolean started = false;

  /**
   * ct.
   * @throws RemoteException
   */
  public TANTestServiceImpl() throws RemoteException
  {
    super();
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.rmi.TANTestService#getTAN(java.lang.String, java.lang.String)
   */
  public String getTAN(String text, String kontoID) throws RemoteException
  {
    Logger.info("*** TAN TEST-SERVICE *** Text    : " + text);
    Logger.info("*** TAN TEST-SERVICE *** Konto-ID: " + kontoID);
    Logger.info("*** TAN TEST-SERVICE *** Returning Test-TAN: 11111111");
    return "11111111";
  }

  /**
   * @see de.willuhn.datasource.Service#getName()
   */
  public String getName() throws RemoteException
  {
    return "TAN-Testservice";
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
    return this.started;
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
    this.started = true;
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
    this.started = false;
  }

}


/*********************************************************************
 * $Log: TANTestServiceImpl.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/