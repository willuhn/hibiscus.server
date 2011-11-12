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

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.rmi.SchedulerService;
import de.willuhn.jameica.hbci.rmi.Konto;
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
 * Implementierung des Device-Interfaces zur Uebergabe an das Sensor-Framework.
 */
public class DeviceImpl implements Device
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();
  
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
    
    //////////////////////////////////////////////////////////////////////////
    // Salden der Konten
    {
      DBIterator it = Settings.getDBService().createList(Konto.class);
      if (it.hasNext())
      {
        Sensorgroup group = new Sensorgroup();
        group.setName(i18n.tr("Saldo"));
        group.setUuid(getUuid() + ".accounts");

        it.setOrder("ORDER BY blz, kontonummer");
        while (it.hasNext())
        {
          Konto k = (Konto) it.next();
          final Sensor<Double> s = new Sensor<Double>();
          s.setName(k.getLongName());
          s.setSerializer(DecimalSerializer.class);
          s.setUuid(group.getUuid() + "." + k.getChecksum());
          s.setValue(k.getSaldo());
          group.getSensors().add(s);
        }
        m.getSensorgroups().add(group);
      }
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
    return mf.getName() + ": statistics";
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.Device#isEnabled()
   */
  public boolean isEnabled()
  {
    return true;
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.UniqueItem#getUuid()
   */
  public String getUuid()
  {
    Manifest mf = Application.getPluginLoader().getManifest(Plugin.class);
    return mf.getName() + ".device";
  }

}


/**********************************************************************
 * $Log: DeviceImpl.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.4  2010/03/23 18:35:32  willuhn
 * @N Mail-Benachrichtigung (jameica.sensors) via Webfrontend konfigurierbar
 *
 * Revision 1.3  2010/03/23 14:02:56  willuhn
 * @N Sensor-Charts im Webfrontend anzeigen
 * @N Hilfe-Texte
 *
 * Revision 1.2  2010/03/19 12:45:08  willuhn
 * @N Konto-Salden ebenfalls in jameica.sensors anzeigen
 *
 * Revision 1.1  2010/03/19 12:26:14  willuhn
 * @N Anbindung an jameica.sensors
 *
 **********************************************************************/