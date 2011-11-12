/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/rmi/TANTestService.java,v $
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

package de.willuhn.jameica.hbci.payment.rmi;

import java.rmi.RemoteException;

import de.willuhn.datasource.Service;

/**
 * Ein Service zum Testen der TAN-Abfragen.
 * Der Service liefert immer 11111111 als TAN zurueck.
 */
public interface TANTestService extends Service
{
  /**
   * Liefert die Test-TAN "11111111".
   * @param text vom Handler uebergebener Text.
   * @param kontoID die ID des Kontos.
   * @return die TAN "11111111".
   * @throws RemoteException
   */
  public String getTAN(String text, String kontoID) throws RemoteException;

}


/*********************************************************************
 * $Log: TANTestService.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/