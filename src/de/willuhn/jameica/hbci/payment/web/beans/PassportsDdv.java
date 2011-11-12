/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/PassportsDdv.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.passports.ddv.DDVConfig;
import de.willuhn.jameica.hbci.passports.ddv.DDVConfigFactory;
import de.willuhn.jameica.hbci.passports.ddv.rmi.Reader;
import de.willuhn.jameica.hbci.passports.ddv.server.CustomReader;
import de.willuhn.jameica.hbci.passports.ddv.server.PassportHandleImpl;
import de.willuhn.jameica.hbci.passports.ddv.server.PassportImpl;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;

/**
 * Controller fuer Chipkarte.
 */
public class PassportsDdv extends AbstractPassports
{
  @Request
  private HttpServletRequest request = null;
  
  private DDVConfig config = null;

  /**
   * Action zum Laden der angegebenen DDV-Config.
   * @throws Exception
   */
  public void load() throws Exception
  {
    String id = request.getParameter("config");
    if (id != null && id.length() > 0)
    {
      List<DDVConfig> configs = this.getConfigs();
      for (DDVConfig c:configs)
      {
        if (c.getId().equals(id))
        {
          this.config = c;
          return;
        }
      }
    }
  }

  /**
   * Action-Methode zum Loeschen einer DDV-Config.
   * @throws Exception
   */
  public void delete() throws Exception
  {
    try
    {
      if (this.config == null)
        throw new ApplicationException(i18n.tr("Keine zu löschende Kartenleser-Konfiguration ausgewählt"));

      // Synchronisierung der Konten komplett deaktivieren - wuerde sonst nur Fehler liefern, weil
      // das Sicherheitsmedium nicht mehr da ist
      List<Konto> konten = this.config.getKonten();
      if (konten != null)
      {
        for (Konto k:konten)
        {
          Logger.info("disable all synchronize options for account [id: " + k.getID() + "]");
          SynchronizeOptions options = new SynchronizeOptions(k);
          options.setAll(false);
        }
      }
      
      // Konfiguration loeschen
      DDVConfigFactory.delete(this.config);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Kartenleser-Konfiguration gelöscht"),StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while deleting ddv config",e);
        msg = i18n.tr("Fehler beim Löschen der Kartenleser-Konfiguration: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      this.config = null;
    }
  }
  
  /**
   * Action-Methode zum Speichern der Einstellungen.
   * @throws Exception
   */
  public void store() throws Exception
  {
    try
    {
      if (this.config == null)
      {
        Logger.info("creating ddv config");
        this.config = DDVConfigFactory.create();
      }
      
      //////////////////////////////////////////////////////////////////////////
      // nicht konfigurierbare Werte
      this.config.setSoftPin(true); // Eingabe niemals ueber Kartenleser
      //
      //////////////////////////////////////////////////////////////////////////

      //////////////////////////////////////////////////////////////////////////
      // Sonstige Parameter
      
      // Kartenleser
      String reader = request.getParameter("reader");
      if (reader == null || reader.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte wähle ein Kartenleser-Modell aus"));

      List<Reader> readers = this.getReaders();
      for (Reader r:readers)
      {
        if (r.getClass().getName().equals(reader))
        {
          this.config.setReaderPreset(r);
          this.config.setCTAPIDriver(r.getCTAPIDriver());
          break;
        }
      }
      
      if (this.config.getReaderPreset() == null)
        throw new ApplicationException(i18n.tr("Bitte wähle ein Kartenleser-Modell aus"));
      if (this.config.getCTAPIDriver() == null)
        throw new ApplicationException(i18n.tr("Das Kartenleser-Modell wird vom Betriebssystem des Servers nicht unterstützt"));
      
      // Bezeichnung
      this.config.setName(request.getParameter("name"));
      
      // Port
      String port = request.getParameter("port");
      if (port == null || port.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte wähle einen Port aus"));
      this.config.setPort(port);
      
      // HBCI-Version
      String hbciVersion = request.getParameter("hbciversion");
      if (hbciVersion == null || port.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte wähle eine HBCI-Version aus"));
      this.config.setHBCIVersion(hbciVersion);

      // CT-Number
      try {
        this.config.setCTNumber(Integer.parseInt(request.getParameter("ctnumber")));
      }
      catch (Exception e) {
        throw new ApplicationException(i18n.tr("Bitte wähle einen Wert für den Index des Lesers aus"));
      }
      
      // Entry-Index
      try {
        this.config.setEntryIndex(Integer.parseInt(request.getParameter("entryindex")));
      }
      catch (Exception e) {
        throw new ApplicationException(i18n.tr("Bitte wähle einen Wert für den Index des HBCI-Zugangs aus"));
      }
      //
      //////////////////////////////////////////////////////////////////////////

      
      //////////////////////////////////////////////////////////////////////////
      // PIN-Checks
      String password  = request.getParameter("pin");
      String password2 = request.getParameter("pin2");

      if (password == null || password.length() == 0)
        throw new ApplicationException(i18n.tr("Keine PIN angegeben"));

      if (password2 == null || password2.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib die PIN zur Kontrolle zweimal ein"));

      if (!password.equals(password2))
        throw new ApplicationException(i18n.tr("PIN-Eingaben stimmen nicht überein."));

      Logger.info("saving pin in wallet");
      de.willuhn.jameica.hbci.payment.Settings.setHBCIPassword(this.config.getId(),password);
      //
      //////////////////////////////////////////////////////////////////////////

      
      Logger.info("create passport handle");
      PassportHandle handle = new PassportHandleImpl(this.config);
      
      Logger.info("fetch accounts");
      Konto[] konten = readKonten(handle.open());
      if (konten != null)
        this.config.setKonten(Arrays.asList(konten)); // Konten fest verknuepfen
      DDVConfigFactory.store(this.config); // Config registrieren
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Chipkarten-Einstellungen gespeichert und Konten angelegt."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while saving ddv config",e);
        msg = i18n.tr("Fehler beim Speichern der Chipkarten-Einstellungen: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }

  }
  
  /**
   * @see de.willuhn.jameica.hbci.payment.web.beans.AbstractPassports#getImplementationClass()
   */
  Class<? extends Passport> getImplementationClass()
  {
    return PassportImpl.class;
  }

  /**
   * Liefert die aktuell geladene DDV-Config oder NULL wenn keine geladen ist.
   * @return die aktuell geladene DDV-Config oder NULL wenn keine geladen ist.
   */
  public DDVConfig getCurrentConfig()
  {
    return this.config;
  }

  /**
   * Liefert die Liste der moeglichen Ports.
   * @return Liste der moeglichen Ports.
   */
  public List<String> getPorts()
  {
    return Arrays.asList(de.willuhn.jameica.hbci.passports.ddv.DDVConfig.PORTS);
  }

  /**
   * Liefert eine Liste der vorhandenen Kartenleser-Konfigurationen.
   * @return Liste der Kartenleser-Konfigurationen.
   * @throws Exception
   */
  public List<DDVConfig> getConfigs() throws Exception
  {
    return DDVConfigFactory.getConfigs();
  }


  /**
   * Liefert die Liste der Kartenleser-Presets.
   * @return Liste der Kartenleser-Presets.
   */
  public List<Reader> getReaders()
  {
    List<Reader> list = DDVConfigFactory.getReaderPresets();
    List<Reader> result = new ArrayList<Reader>();
    for (Reader r:list)
    {
      if (r.getClass().getName().equals(CustomReader.class.getName()))
        continue; // Der ist im Payment-Server nicht zulaessig, weil hier der Benutzer
                  // den CTAPI-Treiber manuell angeben muesste. Dazu muesste er aber
                  // im Dateisystem des Servers browsen koennen. Und das ist mir
                  // zu umstaendlich.
      
      if (!r.isSupported())
        continue; // Wird vom OS nicht unterstuetzt
      result.add(r);
    }
    return result;
  }
}



/**********************************************************************
 * $Log: PassportsDdv.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.11  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.10  2011/09/02 07:38:50  willuhn
 * @R Biometrie-Support bei Kartenlesern entfernt - wurde nie benutzt
 *
 * Revision 1.9  2010/10/07 12:20:28  willuhn
 * @N Lizensierungsumfang (Anzahl der zulaessigen Konten) konfigurierbar
 *
 * Revision 1.8  2010/09/10 16:01:09  willuhn
 * @B Moegliche NPE
 *
 * Revision 1.7  2010/09/10 15:58:10  willuhn
 * @B NPE
 *
 * Revision 1.6  2010/09/08 15:04:18  willuhn
 * @B Bugfixing
 *
 * Revision 1.5  2010/09/08 14:54:03  willuhn
 * @N Umstellung auf Multi-DDV-Support
 *
 * Revision 1.4  2010/07/23 11:35:21  willuhn
 * @R externe hbci_passport_*.jar Dateien nicht mehr noetig
 * @R JNI-Lib nicht mehr aenderbar
 *
 * Revision 1.3  2010/03/04 16:13:31  willuhn
 * @N Kartenleser-Konfiguration
 *
 * Revision 1.2  2010/02/24 17:39:29  willuhn
 * @N Synchronisierung kann nun auch manuell gestartet werden
 * @B kleinere Bugfixes
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/