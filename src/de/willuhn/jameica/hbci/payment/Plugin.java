/**********************************************************************
 *
 * Copyright (c) 2024 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment;

import java.util.List;

import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.passports.pintan.PinTanMigrationService;
import de.willuhn.jameica.hbci.passports.pintan.PinTanMigrationService.VerificationEntry;
import de.willuhn.jameica.plugin.AbstractPlugin;
import de.willuhn.jameica.services.BeanService;
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
    
    if (Settings.isAutoMigratePinTan())
    {
      try
      {
        final BeanService bs = Application.getBootLoader().getBootable(BeanService.class);
        final PinTanMigrationService ps = bs.get(PinTanMigrationService.class);
        final List<VerificationEntry> list = ps.getConfigs();
        if (list != null && !list.isEmpty())
        {
          ps.migrate(list);
        }
      }
      catch (Exception e)
      {
        Logger.error("unable to auto-migrate pin/tan passports",e);
      }
    }
    
  }
}
