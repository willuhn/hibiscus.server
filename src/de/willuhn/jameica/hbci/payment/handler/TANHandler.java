/**********************************************************************
 *
 * Copyright (c) 2019 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
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
