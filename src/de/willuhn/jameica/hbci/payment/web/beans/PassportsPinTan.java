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

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.manager.HBCIHandler;

import de.willuhn.datasource.GenericIterator;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.gui.action.PassportDeleteBPD;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.passports.pintan.PinTanConfigFactory;
import de.willuhn.jameica.hbci.passports.pintan.PtSecMech;
import de.willuhn.jameica.hbci.passports.pintan.PtSecMechDeleteSettings;
import de.willuhn.jameica.hbci.passports.pintan.rmi.PinTanConfig;
import de.willuhn.jameica.hbci.passports.pintan.server.PassportHandleImpl;
import de.willuhn.jameica.hbci.passports.pintan.server.PassportImpl;
import de.willuhn.jameica.hbci.passports.pintan.server.PinTanConfigImpl;
import de.willuhn.jameica.hbci.payment.Settings;
import de.willuhn.jameica.hbci.payment.handler.TANHandler;
import de.willuhn.jameica.hbci.payment.handler.TANHandlerRegistry;
import de.willuhn.jameica.hbci.payment.messaging.TrustMessageConsumer;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.Base64;
import de.willuhn.util.Session;

/**
 * Controller fuer PIN/TAN-Passports.
 */
public class PassportsPinTan extends AbstractPassports
{
  
  @Request
  private HttpServletRequest request = null;
  
  private PinTanConfig config = null;
  
  /**
   * Eine Session, in der wir die Benutzereingaben kurz zwischenspeichern,
   * damit sie vom HBCI4Java-Callback abgefragt werden koennen.
   */
  public final static Session SESSION = new Session(1000l * 60); // 1 Minute
  
  /**
   * @see de.willuhn.jameica.hbci.payment.web.beans.AbstractPassports#getImplementationClass()
   */
  Class<? extends Passport> getImplementationClass()
  {
    return PassportImpl.class;
  }

  /**
   * Action zum Laden der angegebenen PIN/TAN-Config.
   * @throws Exception
   */
  public void load() throws Exception
  {
    String path = request.getParameter("config");
    if (path == null || path.length() == 0)
      return;
    
    File f = new File(path);
    if (!f.exists() || !f.canRead())
    {
      Logger.error("pin/tan config " + f.getAbsolutePath() + " is not readable, skipping");
      return;
    }
    
    this.config = new PinTanConfigImpl(PinTanConfigFactory.load(f),f);
  }
  
  /**
   * Action zum Aktualisieren der zugeordneten Konten zu einer PIN/TAN-Config.
   */
  public void update()
  {
    try
    {
      if (this.config == null)
        throw new ApplicationException(i18n.tr("Keine PIN/TAN-Konfiguration ausgewählt"));

      Logger.info("create passport handle");
      PassportHandle handle = new PassportHandleImpl(this.config);

      Logger.info("fetch accounts");
      Konto[] konten = readKonten(handle.open());
      if (konten != null)
        this.config.setKonten(konten); // Konten fest verknuepfen
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Konten aktualisiert."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while updating accounts for pin/tan config",e);
        msg = i18n.tr("Fehler beim Aktualisieren der Konten: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      // Geladene Config loeschen, damit wir nicht in die Detail-View wechseln
      this.config = null;
    }
  }
  
  /**
   * Action zum Neu-Synchronisieren des Bankzugangs.
   */
  public void sync()
  {
    try
    {
      if (this.config == null)
        throw new ApplicationException(i18n.tr("Keine PIN/TAN-Konfiguration ausgewählt"));

      Logger.info("create passport handle");
      
      new PtSecMechDeleteSettings().handleAction(this.config);
      
      PassportHandle handle = new PassportHandleImpl(this.config);
      HBCIHandler handler = handle.open();
      new PassportDeleteBPD().handleAction(handler.getPassport());
      handler.sync(true);
      handle.close(); // nein, nicht im finally, denn wenn das Oeffnen fehlschlaegt, ist nichts zum Schliessen da

      Logger.info("sync passport");
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Bankzugang synchronisiert."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while synchronizing pin/tan config",e);
        msg = i18n.tr("Fehler beim Synchronisieren: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      // Geladene Config loeschen, damit wir nicht in die Detail-View wechseln
      this.config = null;
    }
  }

  /**
   * Loescht die aktuelle PIN/TAN-Config.
   * @throws Exception
   */
  public void delete() throws Exception
  {
    try
    {
      if (this.config == null)
        throw new ApplicationException(i18n.tr("Keine zu löschende PIN/TAN-Konfiguration ausgewählt"));

      // Synchronisierung der Konten komplett deaktivieren - wuerde sonst nur Fehler liefern, weil
      // das Sicherheitsmedium nicht mehr da ist
      Konto[] konten = this.config.getKonten();
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
      PinTanConfigFactory.delete(this.config);
      
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("PIN/TAN-Konfiguration gelöscht"),StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while deleting pin/tan config",e);
        msg = i18n.tr("Fehler beim Löschen der PIN/TAN-Konfiguration: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      this.config = null;
    }
  }
  
  /**
   * Action zum Speichern einer PIN/TAN-Config.
   */
  public void store()
  {
    boolean update = this.config != null;
    TrustMessageConsumer tmc = new TrustMessageConsumer();

    String blz = request.getParameter("blz");

    try
    {
      /////////////////////////////////////////////////////////////////
      // PIN checken
      String password   = request.getParameter("pin");
      String password2  = request.getParameter("pin2");

      if (!update && (password == null || password.length() == 0))
        throw new ApplicationException(i18n.tr("Keine PIN angegeben"));

      if (!update && (password2 == null || password2.length() == 0))
        throw new ApplicationException(i18n.tr("Bitte gib die PIN zur Kontrolle ein zweites Mal ein"));

      if (password != null && password.length() > 0 &&
          password2 != null && password2.length() > 0 &&
          !password.equals(password2))
        throw new ApplicationException(i18n.tr("PIN-Eingaben stimmen nicht überein."));
      //
      /////////////////////////////////////////////////////////////////

      /////////////////////////////////////////////////////////////////
      // Benutzereingaben checken
      String bezeichnung     = request.getParameter("bezeichnung");
      String benutzerkennung = request.getParameter("benutzerkennung");
      String kundenkennung   = request.getParameter("kundenkennung");
      String url             = request.getParameter("url");
      String version         = request.getParameter("version");
      String secmech         = request.getParameter("secmech");
      String tanmedia        = request.getParameter("tanmedia");
      
      if (bezeichnung == null || bezeichnung.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib eine Bezeichnung für diese PIN/TAN-Konfiguration ein"));

      if (benutzerkennung == null || benutzerkennung.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib eine Benutzerkennung ein"));
      
      if (kundenkennung == null || kundenkennung.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib eine Kundenkennung ein"));
      
      if (url == null || url.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib eine URL ein"));
      
      if (url.startsWith("https://"))
      {
        Logger.info("removing leading https:// from " + url);
        url = url.substring(8);
      }
      
      if (blz == null || blz.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib eine BLZ ein"));

      if (version == null || version.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte wähle eine HBCI-Version"));
      //
      /////////////////////////////////////////////////////////////////


      
      /////////////////////////////////////////////////////////////////
      // Config-File vorbereiten
      File f = null;
      if (update)
      {
        f = new File(this.config.getFilename());
        Logger.info("updating pin/tan config " + f.getAbsolutePath());
      }
      else
      {
        f = PinTanConfigFactory.createFilename();
        Logger.info("created new pin/tan file " + f.getAbsolutePath());

        Logger.info("creating random passport key");
        byte[] pass = new byte[8];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(pass);
        Settings.setHBCIPassword(f.getName(),Base64.encode(pass));
      }
      //
      /////////////////////////////////////////////////////////////////


      /////////////////////////////////////////////////////////////////
      // Passwort speichern
      if (password != null && password.length() > 0)
      {
        Logger.info("saving pin in wallet");
        Settings.setHBCIPassword(f.getName() + "." + HBCICallback.NEED_PT_PIN,password);
      }
      //
      /////////////////////////////////////////////////////////////////


      Application.getMessagingFactory().registerMessageConsumer(tmc);


      if (!update)
      {
        Logger.info("preparing callback session");
        SESSION.put(new Integer(HBCICallback.NEED_USERID),      benutzerkennung);
        SESSION.put(new Integer(HBCICallback.NEED_CUSTOMERID),  kundenkennung);
        SESSION.put(new Integer(HBCICallback.NEED_HOST),        url);
        SESSION.put(new Integer(HBCICallback.NEED_BLZ),         blz);
        SESSION.put(new Integer(HBCICallback.NEED_COUNTRY),     "DE");
        SESSION.put(new Integer(HBCICallback.NEED_PORT),        "443");
        SESSION.put(new Integer(HBCICallback.NEED_FILTER),      "Base64");
        SESSION.put(new Integer(HBCICallback.NEED_PT_SECMECH),  secmech);
        SESSION.put(new Integer(HBCICallback.NEED_PT_TANMEDIA), tanmedia);

        Logger.info("creating pin/tan config");
        config = new PinTanConfigImpl(PinTanConfigFactory.load(f),f);
      }
      else
      {
        // Nur beim Update direkt speichern - nicht bei der Neuanlage
        // Bei der Neuanlage landen die Daten im Cache und werden dann
        // im Callback gespeichert
        config.setStoredSecMech(PtSecMech.createFailsafe(secmech));
        config.setTanMedia(tanmedia);
      }
      
      // Die werden nicht via Callback abgefragt
      config.setBezeichnung(bezeichnung);
      config.setHBCIVersion(version);
        
      Logger.info("save pin/tan config");
      PinTanConfigFactory.store(config);

      Logger.info("applying TAN handler");
      String th = request.getParameter("tanhandler");
      if (th != null && th.length() > 0)
      {
        TANHandler tanHandler = this.getCurrentTanHandler();
        if (tanHandler == null)
          tanHandler = TANHandlerRegistry.createTANHandler(th,new Format().escapePath(this.config.getFilename()));

        Iterator keys = request.getParameterMap().keySet().iterator();
        while (keys.hasNext())
        {
          String name = (String) keys.next();
          if (!name.startsWith(th + "."))
            continue;
          String shortname = name.substring(th.length()+1);
          tanHandler.set(shortname,request.getParameter(name));
        }
      }
      
      if (!update)
      {
        Logger.info("create passport handle");
        PassportHandle handle = new PassportHandleImpl(config);

        Logger.info("fetch accounts");
        Konto[] konten = readKonten(handle.open());
        if (konten != null)
          config.setKonten(konten); // Konten fest verknuepfen
        Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Konten angelegt."), StatusBarMessage.TYPE_SUCCESS));
      }

      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Einstellungen gespeichert"), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      // Sonderrolle ING. Siehe https://homebanking-hilfe.de/forum/topic.php?p=170637#real170637
      // Deren FinTS-Server ist so kaputt, dass das Anlegen des Bankzugangs im ersten Schritt
      // immer scheitert. Daher können wir deren Bankzugänge beim Neuanlegen nicht löschen
      // Im Fehlerfall die Config wieder loeschen. Aber nur bei Neuanlage
      boolean isIng = (Objects.equals(blz,"50010517"));
        
      if (!update && config != null && !isIng)
      {
        try
        {
          PinTanConfigFactory.delete(config);
        }
        catch (Exception e1)
        {
          Logger.error("unable to delete config",e1);
        }
      }
      this.config = null;
      
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while saving pin/tan config",e);
        msg = i18n.tr("Fehler beim Speichern der PIN/TAN-Einstellungen: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      Application.getMessagingFactory().unRegisterMessageConsumer(tmc);
    }
  }
  
  /**
   * Liefert die aktuell geladene PIN/TAN-Config oder NULL wenn keine geladen ist.
   * @return die aktuell geladene PIN/TAN-Config oder NULL wenn keine geladen ist.
   */
  public PinTanConfig getCurrentConfig()
  {
    return this.config;
  }

  /**
   * Liefert den TAN-Handler der aktuellen PIN/TAN-Config.
   * @return TAN-Handler der aktuellen PIN/TAN-Config.
   * @throws Exception
   */
  public TANHandler getCurrentTanHandler() throws Exception
  {
    if (this.config == null)
      return null;
    return TANHandlerRegistry.getTANHandler(new Format().escapePath(this.config.getFilename()));
  }
  
  /**
   * Prueft, ob der uebergebene TAN-Handler der aktuelle fuer die geladene Config ist. 
   * @param h zu testender TAN-Handler.
   * @return true, wenn es der aktuelle ist.
   * @throws Exception
   */
  public boolean isCurrentTanHandler(TANHandler h) throws Exception
  {
    if (h == null || this.config == null)
      return false;
    TANHandler test = this.getCurrentTanHandler();
    if (test == null)
      return false;
    
    return test.getClass().getName().equals(h.getClass().getName());
  }

  
  /**
   * Liefert eine Liste der vorhandenen PIN/TAN-Konfigurationen.
   * @return Liste der PIN/TAN-Konfigurationen.
   * @throws Exception
   */
  public List<PinTanConfig> getConfigs() throws Exception
  {
    List<PinTanConfig> list = new ArrayList<PinTanConfig>();

    GenericIterator it = PinTanConfigFactory.getConfigs();
    while (it.hasNext())
    {
      list.add((PinTanConfig)it.next());
    }
    return list;
  }

  /**
   * Liefert eine Liste der verfuegbaren TAN-Handler.
   * @return Liste der verfuegbaren TAN-Handler.
   * @throws Exception
   */
  public List<TANHandler> getTanHandlers() throws Exception
  {
    TANHandler[] list = TANHandlerRegistry.getTANHandler();
    if (this.config != null)
    {
      String path = new Format().escapePath(this.config.getFilename());
      for (TANHandler h:list)
      {
        h.setConfig(path);
      }
    }
    List<TANHandler> result = new ArrayList(Arrays.asList(list));
    Collections.sort(result,new Comparator<TANHandler>() {
      /**
       * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
       */
      @Override
      public int compare(TANHandler o1, TANHandler o2)
      {
        return o1.getName().compareTo(o2.getName());
      }
    });
    return result;
  }
}

