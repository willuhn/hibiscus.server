/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.sensors;

import java.io.IOException;

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.sensors.devices.DecimalSerializer;
import de.willuhn.jameica.sensors.devices.Device;
import de.willuhn.jameica.sensors.devices.Measurement;
import de.willuhn.jameica.sensors.devices.Sensor;
import de.willuhn.jameica.sensors.devices.Sensorgroup;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

/**
 * Implementierung des Device-Interfaces fuer die Account-Sensoren.
 */
public class AccountDeviceImpl implements Device
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
          s.setUuid(group.getUuid().hashCode() + "." + k.getChecksum());
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
    return mf.getName() + ": account statistics";
  }

  /**
   * @see de.willuhn.jameica.sensors.devices.Device#isEnabled()
   */
  public boolean isEnabled()
  {
    return settings.getBoolean("sensors.enabled",true);
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
