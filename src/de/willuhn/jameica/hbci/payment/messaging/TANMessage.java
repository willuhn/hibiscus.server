/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/messaging/TANMessage.java,v $
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

package de.willuhn.jameica.hbci.payment.messaging;

import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.Message;

/**
 * Message, die vom HBCICallback verschickt wird, wenn eine TAN benoetigt wird.
 */
public class TANMessage implements Message
{
  private String text           = null;
  private HBCIPassport passport = null;
  private Konto konto           = null;
  private String tan            = null;

  /**
   * ct.
   * @param text der anzuzeigende Text.
   * @param passport der zugehoerige Passport.
   * @param konto das ggf vorhandene Konto. Kann <code>null</code> sein.
   */
  public TANMessage(String text, HBCIPassport passport, Konto konto)
  {
    this.text     = text;
    this.passport = passport;
    this.konto    = konto;
  }

  /**
   * Liefert den zugehoerigen Passport.
   * @return the passport
   */
  public HBCIPassport getPassport()
  {
    return this.passport;
  }

  /**
   * @return the konto
   */
  public Konto getKonto()
  {
    return this.konto;
  }

  /**
   * Liefert die vom Handler zurueckgelieferte TAN.
   * @return the tan
   */
  public String getTAN()
  {
    return this.tan;
  }
  
  /**
   * Speichert die zu verwendende TAN.
   * @param tan die zu verwendende TAN.
   */
  public void setTAN(String tan)
  {
    this.tan = tan;
  }

  /**
   * Der anzuzeigende Text.
   * @return the text
   */
  public String getText()
  {
    return this.text;
  }

}


/*********************************************************************
 * $Log: TANMessage.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/