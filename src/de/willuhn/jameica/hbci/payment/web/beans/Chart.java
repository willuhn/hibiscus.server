/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Chart.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.rmi.RemoteException;

/**
 * Bean fuer die Charts.
 */
public class Chart
{
  /**
   * Liefert einen Timestamp mit dem letzten Monat.
   * @return Timestamp mit dem Monat.
   * @throws RemoteException
   */
  public String getMonth() throws RemoteException
  {
    long now = System.currentTimeMillis() / 1000L;
    now -= (60 * 60 * 24 * 30);
    return String.valueOf(now);
  }
}



/**********************************************************************
 * $Log: Chart.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/03/23 14:02:56  willuhn
 * @N Sensor-Charts im Webfrontend anzeigen
 * @N Hilfe-Texte
 *
 **********************************************************************/