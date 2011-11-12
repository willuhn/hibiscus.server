/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/rmi/SchedulerService.java,v $
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
import java.util.Date;

import de.willuhn.datasource.Service;

/**
 * Interface fuer den Scheduler-Service des Payment-Servers.
 */
public interface SchedulerService extends Service
{
  /**
   * Liefert Datum und Uhrzeit der naechsten Ausfuehrung.
   * @return Datum und Uhrzeit der naechsten Ausfuehrung.
   * @throws RemoteException
   */
  public Date getNextExecution() throws RemoteException;
}


/*********************************************************************
 * $Log: SchedulerService.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.2  2007/05/16 14:16:43  willuhn
 * @N nextExecutionTime
 * @N Scheduler cleanup
 *
 * Revision 1.1  2007/05/15 16:40:04  willuhn
 * @N Initial checkin for "Hibiscus Payment-Server"
 *
 **********************************************************************/