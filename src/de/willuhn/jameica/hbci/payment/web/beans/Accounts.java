/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Accounts.java,v $
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
import java.util.List;

import de.willuhn.datasource.pseudo.PseudoIterator;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.PassportRegistry;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.rmi.HBCIDBService;
import de.willuhn.jameica.hbci.rmi.Konto;

/**
 * Bean fuer den Zugriff auf die Konten-Liste.
 */
public class Accounts
{
  /**
   * Liefert die Liste der Konten.
   * @return Liste der Konten.
   * @throws Exception
   */
  public List<Konto> getAccounts() throws Exception
  {
    HBCIDBService s = de.willuhn.jameica.hbci.Settings.getDBService();
    DBIterator it = s.createList(Konto.class);
    it.setOrder("order by " + s.getSQLTimestamp("saldo_datum") + " desc"); // blz,kontonummer,unterkonto
    return PseudoIterator.asList(it);
  }
  
  /**
   * Liefert den Passport zum angegebenen Konto.
   * @param k das Konto.
   * @return der Passport.
   * @throws Exception
   */
  public Passport getPassport(Konto k) throws Exception
  {
    String driver = k.getPassportClass();
    return driver == null ? null : PassportRegistry.findByClass(driver);
  }

  /**
   * Liefert die Synchronisationseinstellungen fuer das Konto.
   * @param k das Konto.
   * @return Synchronisationseinstellungen fuer das Konto.
   * @throws RemoteException
   */
  public SynchronizeOptions getOptions(Konto k) throws RemoteException
  {
    return new SynchronizeOptions(k);
  }

}



/**********************************************************************
 * $Log: Accounts.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.2  2010/02/26 16:19:43  willuhn
 * @N Konten loeschen
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/