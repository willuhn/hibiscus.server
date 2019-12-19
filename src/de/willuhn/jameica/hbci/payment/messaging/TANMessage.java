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
  /**
   * Klassifiziert den Typ der TAN-Abfrage.
   */
  public enum TANType
  {
    /**
     * Einfache TAN-Verfahren wie smsTAN oder PushTAN, bei denen keine zusätzlichen Informationen benötigt werden.
     */
    NORMAL,
    
    /**
     * ChipTAN optisch/USB, also ein TAN-Verfahren bei dem ein Flickercode noetig ist.
     * Der Flickercode wird im Payload uebertragen.
     */
    CHIPTAN,
    
    /**
     * PhotoTAN. Das Bild mit der PhotoTAN wird Base64-codiert im Payload uebertragen.
     */
    PHOTOTAN,
    
    /**
     * QR-TAN. Das Bild mit dem QR-Code wird Base64-codiert im Payload uebertragen.
     */
    QRTAN
  }
  
  private String text           = null;
  private HBCIPassport passport = null;
  private Konto konto           = null;
  private String tan            = null;
  
  private TANType type          = null;
  private String payload        = null;

  /**
   * ct.
   * @param text der anzuzeigende Text.
   * @param passport der zugehoerige Passport.
   * @param konto das ggf vorhandene Konto. Kann <code>null</code> sein.
   * @param type der Typ des TAN-Verfahrens.
   * @param payload der Payload.
   */
  public TANMessage(String text, HBCIPassport passport, Konto konto, TANType type, String payload)
  {
    this.text     = text;
    this.passport = passport;
    this.konto    = konto;
    this.type     = type;
    this.payload  = payload;
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
  
  /**
   * Liefert den Typ des TAN-Verfahrens.
   * @return type der Typ des TAN-Verfahrens.
   */
  public TANType getType()
  {
    return type;
  }
  
  /**
   * Der Payload.
   * @return payload der Payload.
   */
  public String getPayload()
  {
    return payload;
  }

}
