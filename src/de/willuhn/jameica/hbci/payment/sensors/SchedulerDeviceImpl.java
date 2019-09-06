/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/sensors/DeviceImpl.java,v $
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

package de.willuhn.jameica.hbci.payment.sensors;

import java.io.IOException;

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.SchedulerService;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.sensors.devices.DecimalSerializer;
import de.willuhn.jameica.sensors.devices.Device;
import de.willuhn.jameica.sensors.devices.Measurement;
import de.willuhn.jameica.sensors.devices.Sensor;
import de.willuhn.jameica.sensors.devices.Sensorgroup;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;

/**
 * Implementierung des Device-Interfaces fuer den Scheduler-Sensor.
 */
public class SchedulerDeviceImpl implements Device
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();
  private final static de.willuhn.jameica.system.Settings settings = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getSettings();
  
  /**
   * @see de.willuhn.jameica.sensors.devices.Device#collect()
   */
  public Measurement collect() throws IOException
  {
    Measurement m = new Measurement();

    //////////////////////////////////////////////////////////////////////////
    // Scheduler-Status
    {
      Long status = Long.valueOf(0);
      try
      {
        SchedulerService service = (SchedulerService) Application.getServiceFactory().lookup(Plugin.class,"scheduler");
        status = new Long(service.isStarted() ? 1 : 0);
      }
      catch (Exception e)
      {
        Logger.error("unable to check scheduler status",e);
      }
      
      Sensorgroup group = new Sensorgroup();
      group.setName(i18n.tr("Scheduler"));
      group.setUuid(getUuid() + ".scheduler");
      
      Sensor<Long> s = new Sensor<Long>();
      s.setName(i18n.tr("Scheduler Status"));
      s.setSerializer(DecimalSerializer.class);
      s.setUuid(group.getUuid() + ".status");
      s.setValue(status);
      group.getSensors().add(s);
      m.getSensorgroups().add(group);
    }
    //
    //////////////////////////////////////////////////////////////////////////
    
    return m;
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.Device#getName()
   */
  public String getName()
  {
    Manifest mf = Application.getPluginLoader().getManifest(Plugin.class);
    return mf.getName() + ": scheduler statistics";
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.Device#isEnabled()
   */
  public boolean isEnabled()
  {
    return settings.getBoolean("sensors.scheduler.enabled",true);
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.UniqueItem#getUuid()
   */
  public String getUuid()
  {
    Manifest mf = Application.getPluginLoader().getManifest(Plugin.class);
    return mf.getName() + ".scheduler.device";
  }

}
