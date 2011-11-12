/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/handler/TANHandler.java,v $
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

package de.willuhn.jameica.hbci.payment.handler;

import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.util.ApplicationException;


/**
 * Interface fuer einen Handler, der TAN-Anfragen beantworten kann.
 */
public interface TANHandler
{
  /**
   * Legt den Namen der Config fest, fuer die dieser Haendler zustaendig ist.
   * @param config eindeutiger Name der Config. Damit wird
   * dem Haendler ermoeglicht, mehrere Konfigurationen parallel
   * zu verwalten.
   */
  public void setConfig(String config);
  
  /**
   * Liefert eine Liste von Config-Parametern fuer den Handler.
   * @return Liste der Parameter. 
   */
  public Parameter[] getParameters();
  
  /**
   * Liefert einen sprechenden Namen fuer den Handler.
   * @return Name des Handlers.
   */
  public String getName();
  
  /**
   * Speichert einen Parameter.
   * @param id ID des Parameters.
   * @param value Wert des Parameters.
   * @throws ApplicationException wenn der Parameter einen ungueltigen Wert besitzt.
   */
  public void set(String id, String value) throws ApplicationException;
  
  /**
   * Liefert die noetige TAN.
   * @param text der von der Bank gelieferte Abfragetext.
   * Dieser enthaelt bei iTAN u.a. die Nummer der einzugebenden TAN.
   * @param konto ggf vorhandene Konto-Information. Kann <code>null</code> sein.
   * @return die zu verwendende TAN.
   * @throws Exception
   */
  public String getTAN(String text, Konto konto) throws Exception;
}


/*********************************************************************
 * $Log: TANHandler.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/