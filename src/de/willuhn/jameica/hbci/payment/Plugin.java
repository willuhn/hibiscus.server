/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/Plugin.java,v $
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

package de.willuhn.jameica.hbci.payment;

import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Plugin-Holder fuer den Payment-Server.
 */
public class Plugin extends AbstractPlugin
{
  /**
   * @see de.willuhn.jameica.plugin.AbstractPlugin#init()
   */
  public void init() throws ApplicationException
  {
    Logger.info("reinit hbci callback for server usage");
    HBCI hbci = (HBCI) Application.getPluginLoader().getPlugin(HBCI.class);
    hbci.initHBCI(HBCICallbackServer.class.getName());
  }
}


/*********************************************************************
 * $Log: Plugin.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.10  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.9  2010/09/10 15:52:29  willuhn
 * @N Umstellung auf Multi-DDV-Support - Migration der Passport-Dateien
 *
 * Revision 1.8  2010/02/11 11:25:41  willuhn
 * @N log license status
 *
 * Revision 1.7  2009/02/18 12:02:08  willuhn
 * @B Jameica 1.8 fixes
 *
 * Revision 1.6  2008/07/28 09:40:39  willuhn
 * @B Registrierung des Consumers vergessen
 *
 * Revision 1.5  2007/12/14 13:59:10  willuhn
 * *** empty log message ***
 *
 * Revision 1.4  2007/06/19 09:57:10  willuhn
 * @N Lizenz-Check
 *
 * Revision 1.3  2007/05/16 14:16:43  willuhn
 * @N nextExecutionTime
 * @N Scheduler cleanup
 *
 * Revision 1.2  2007/05/15 17:21:08  willuhn
 * @N Added Scheduler
 *
 * Revision 1.1  2007/05/15 16:40:04  willuhn
 * @N Initial checkin for "Hibiscus Payment-Server"
 *
 **********************************************************************/