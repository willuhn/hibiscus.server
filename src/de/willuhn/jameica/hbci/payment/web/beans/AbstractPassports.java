/**********************************************************************
 *
 * Copyright (c) 2019 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.util.ArrayList;
import java.util.List;

import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.jameica.hbci.PassportRegistry;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.Converter;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;


/**
 * Abstrakte Basis-Bean zum Administrieren der Sicherheitsmedien.
 */
public abstract class AbstractPassports
{
  protected final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  /**
   * Liefert den Namen des Passport-Typs.
   * @return Name des Passport-Typs.
   * @throws Exception
   */
  public String getName() throws Exception
  {
    return PassportRegistry.findByClass(getImplementationClass().getName()).getName();
  }

  /**
   * Liefert die Klasse der Passport-Implementierung.
   * @return Klasse der Passport-Implementierung.
   */
  abstract Class<? extends Passport> getImplementationClass();

  /**
   * Liest die Konten aus dem Handler.
   * @param handler
   * @throws Exception
   */
  protected Konto[] readKonten(HBCIHandler handler) throws Exception
  {
    boolean state = false;
    try
    {
      // Das Pruefen der Kontonummern deaktivieren wir voruebergehend - fuer den Fall, dass die Bank ungueltige Bankverbindungen liefert
      state = Settings.getKontoCheck();
      Settings.setKontoCheck(false);
      
      HBCIPassport passport = handler.getPassport();
      org.kapott.hbci.structures.Konto[] konten = passport.getAccounts();
      if (konten == null || konten.length == 0)
      {
        Logger.error("no accounts found, passport will be unusable within payment server");
        return null;
      }

      List<Konto> list = new ArrayList<Konto>();
      
      for (int i=0;i<konten.length;++i)
      {
        Konto k = Converter.HBCIKonto2HibiscusKonto(konten[i], getImplementationClass());

        Logger.debug("found account " + k.getKontonummer());

        Logger.info("checking if already exists");
        if (k.isNewObject())
        {
          // Konto neu anlegen
          Logger.info("saving new konto");
          try
          {
            k.setPassportClass(getImplementationClass().getName()); // wir speichern den ausgewaehlten Passport.
            k.store();
            Logger.info("konto saved successfully");
          }
          catch (Exception e)
          {
            // Wenn ein Konto fehlschlaegt, soll nicht gleich der ganze Vorgang abbrechen
            Logger.error("error while storing konto " + k.getKontonummer(),e);
            continue;
          }
        }
        else
        {
          Logger.info("already exists");
        }
        list.add(k);
      }
      return list.toArray(new Konto[list.size()]);
    }
    finally
    {
      Settings.setKontoCheck(state);
      if (handler != null)
      {
        try
        {
          handler.close();
        }
        catch (Exception e)
        {
          throw new ApplicationException(i18n.tr("Fehler beim Schliessen des HBCI-Handlers"),e);
        }
      }
    }
  }
}
