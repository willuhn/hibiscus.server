/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Protokoll.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

/**
 * Bean fuer die Konto-Protokolle.
 */
public class Protokoll
{
  /**
   * Liefert den Status-Code fuer Erfolg.
   * @return Status-Code fuer Erfolg.
   */
  public int getSuccess()
  {
    return de.willuhn.jameica.hbci.rmi.Protokoll.TYP_SUCCESS;
  }

  /**
   * Liefert den Status-Code fuer Fehler.
   * @return Status-Code fuer Fehler.
   */
  public int getError()
  {
    return de.willuhn.jameica.hbci.rmi.Protokoll.TYP_ERROR;
  }
}



/**********************************************************************
 * $Log: Protokoll.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/