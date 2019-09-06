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

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Implementierung eines TAN-Haendlers fuer die Eingabe ueber die Konsole.
 */
public class ConsoleTANHandler implements TANHandler
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  /**
   * @see de.willuhn.jameica.hbci.payment.handler.TANHandler#setConfig(java.lang.String)
   */
  public void setConfig(String config)
  {
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.handler.TANHandler#getParameters()
   */
  public Parameter[] getParameters()
  {
    // Wir brauchen hier keine Parameter
    return new Parameter[0];
  }
  
  /**
   * @see de.willuhn.jameica.hbci.payment.handler.TANHandler#getTAN(java.lang.String, de.willuhn.jameica.hbci.rmi.Konto)
   */
  public String getTAN(String text, Konto konto) throws Exception
  {
    String q = null;
    if (konto != null)
      q = i18n.tr("{0} [Konto {1}]",text,konto.getLongName());
    else
      q = text;
      
    return Application.getCallback().askUser(q,i18n.tr("TAN"));
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.handler.TANHandler#set(java.lang.String, java.lang.String)
   */
  public void set(String id, String value) throws ApplicationException
  {
  }

  /**
   * @see de.willuhn.jameica.hbci.payment.handler.TANHandler#getName()
   */
  public String getName()
  {
    return "Console Handler";
  }
}
