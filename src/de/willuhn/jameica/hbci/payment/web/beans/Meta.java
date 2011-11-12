/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Meta.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.system.Application;

/**
 * Bean fuer Meta-Infos.
 */
public class Meta
{
  /**
   * Liefert die Versionsbezeichnung.
   * @return die Versionsbezeichnung.
   */
  public String getVersion()
  {
    Manifest mf = Application.getPluginLoader().getManifest(Plugin.class);
    String version = mf.getVersion().toString();
      
    String bn = mf.getBuildnumber();
    if (bn != null && bn.length() > 0)
      version += " [Build: " + bn + "]";
    
    return version;
  }
  
  /**
   * Liefert die Default-Waehrungsbezeichnung.
   * @return Default-Waehrungsbezeichnung.
   */
  public String getCurrency()
  {
    return HBCIProperties.CURRENCY_DEFAULT_DE;
  }
}



/**********************************************************************
 * $Log: Meta.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.6  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.5  2010/11/10 11:31:40  willuhn
 * @N GUI poliert
 * @N Versionsnummer auf 1.4.0 erhoeht
 *
 * Revision 1.4  2010/11/04 17:25:29  willuhn
 * @N Upload des Lizenzschluessels uebers Webfrontend
 *
 * Revision 1.3  2010/11/04 13:28:26  willuhn
 * @N Lizenzbedingungen muessen nun explizit im Browser akzeptiert werden
 *
 * Revision 1.2  2010/10/07 12:20:28  willuhn
 * @N Lizensierungsumfang (Anzahl der zulaessigen Konten) konfigurierbar
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/