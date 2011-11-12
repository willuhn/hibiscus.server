/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/rmi/ExecuteService.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.rmi;

import java.rmi.RemoteException;

import de.willuhn.datasource.Service;

/**
 * Service, der die Ausfuehrung der Geschaeftsvorfaelle uebernimmt.
 */
public interface ExecuteService extends Service
{
  /**
   * Fuehrt alle faelligen Geschaeftsvorfaelle aus.
   * @throws RemoteException
   */
  public void run() throws RemoteException;
}



/**********************************************************************
 * $Log: ExecuteService.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 **********************************************************************/