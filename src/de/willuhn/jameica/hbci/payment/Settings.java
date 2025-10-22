/**********************************************************************
 *
 * Copyright (c) 2019 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment;

import java.io.File;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.AbstractRDHSWFileBasedPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportDDV;
import org.kapott.hbci.passport.HBCIPassportPinTan;

import de.willuhn.io.FileFinder;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.PassportLoader;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.passports.ddv.DDVConfig;
import de.willuhn.jameica.hbci.passports.pintan.PtSecMech;
import de.willuhn.jameica.hbci.passports.pintan.rmi.PinTanConfig;
import de.willuhn.jameica.hbci.passports.pintan.server.PinTanConfigImpl;
import de.willuhn.jameica.hbci.passports.rdh.RDHKeyFactory;
import de.willuhn.jameica.security.Wallet;
import de.willuhn.jameica.security.crypto.AESEngine;
import de.willuhn.jameica.security.crypto.Engine;
import de.willuhn.jameica.security.crypto.RSAEngine;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Level;
import de.willuhn.logging.Logger;
import de.willuhn.util.Base64;

/**
 * Container fuer die Einstellungen.
 */
public class Settings
{
  private final static de.willuhn.jameica.system.Settings settings = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getSettings();
  private static Wallet wallet = null;
  
  static
  {
    initWallet();
  }
  
  /**
   * Initialisiert das Wallet.
   */
  private static void initWallet()
  {
    // Migration:
    // Wir versuchen erstmal, das Wallet mit AES zu laden.
    // Wenn das fehlschlaegt, laden wir es mit RSA, schalten dann
    // aber auf AES um.
    Engine engine = null;
    String engineClass = settings.getString("wallet.engine",AESEngine.class.getName());
    try
    {
      Class<Engine> ec = Application.getPluginLoader().getManifest(Plugin.class).getClassLoader().load(engineClass);
      BeanService service = Application.getBootLoader().getBootable(BeanService.class);
      engine = service.get(ec);
    }
    catch (Exception e)
    {
      Logger.error("unable to load " + engineClass + ", fallback to default (aes)");
      engine = new AESEngine();
    }
    try
    {
      wallet = new Wallet(Plugin.class,engine);
    }
    catch (Exception e)
    {
      try
      {
        Logger.warn("**** migrating wallet ****");
        Logger.write(Level.DEBUG,"exception for debugging purpose",e);
        wallet = new Wallet(Plugin.class); // Hier wird es gleich gelesen
        wallet.setEngine(engine); // Nach dem Lesen schalten wir auf AES um
        
        // Wir vermerken den Migrationszeitpunkt.
        // Das bewirkt auch gleich das Speichern im neuen Format.
        wallet.set("wallet.migration.aes",HBCI.LONGDATEFORMAT.format(new Date()));
        Logger.warn("**** successfully migrated ****");
      }
      catch (Exception e2)
      {
        Logger.error("unable to migrate wallet to AES, keeping RSA",e2);
        wallet.setEngine(new RSAEngine());
      }
    }
  }
  
  /**
   * Liefert true, wenn auf dem Webfrontend das Master-Passwort als
   * Login abgefragt werden soll.
   * @return true, wenn das Master-Passwort abgefragt werden soll.
   * Per Default: true.
   */
  public static boolean isLoginEnabled()
  {
    return settings.getBoolean("web.login.enabled",true);
  }
  
  /**
   * Liefert das Scheduler-Intervall in Minuten.
   * @return Scheduler-Intervall.
   */
  public static int getSchedulerInterval()
  {
    return settings.getInt("scheduler.interval.minutes",180);
  }
  
  /**
   * Liefert die Anzahl der Sekunden, die bei PushTAN Decoupled gewartet wird, bis das HKTAN gesendet wird.
   * @return die Anzahl der Sekunden.
   */
  public static int getPushTanDecoupledWait()
  {
    return settings.getInt("tan.decoupled.wait.seconds",120);
  }

  /**
   * Liefert true, wenn auch bei PushTAN Decoupled der TAN-Handler gefragt werden soll, statt zu warten.
   * @return true, wenn auch bei PushTAN Decoupled der TAN-Handler gefragt werden soll, statt zu warten.
   */
  public static boolean getPushTanDecoupledTanHandler()
  {
    return settings.getBoolean("tan.decoupled.tanhandler",false);
  }

  /**
   * Speichert das Scheduler-Intervall.
   * @param minutes
   */
  public static void setSchedulerInterval(int minutes)
  {
    settings.setAttribute("scheduler.interval.minutes",Math.abs(minutes));
  }
  
  private static boolean recursion = false;
  
  /**
   * Speichert das TAN-Verfahren in den Einstellungen des Passports.
   * @param passport der Passport.
   * @param value die Bezeichnung des TAN-Verfahrens.
   */
  public static void setPinTanSecMech(HBCIPassport passport, String value)
  {
    try
    {
      final PinTanConfig config = getPinTanConfig(passport);
      config.setStoredSecMech(PtSecMech.createFailsafe(value));
    }
    catch (Exception e)
    {
      Logger.error("unable to save secmech " + value + " in passport config",e);
    }
  }
  
  /**
   * Laedt die Passport-Konfiguration fuer den Passport.
   * @param passport der Passport.
   * @return die Konfiguration.
   * @throws RemoteException
   */
  private static PinTanConfig getPinTanConfig(HBCIPassport passport) throws RemoteException
  {
    final HBCIPassportPinTan ppt = (HBCIPassportPinTan) passport;
    final PassportLoader loader = new PassportLoader() {
      /**
       * @see de.willuhn.jameica.hbci.PassportLoader#load()
       */
      @Override
      public HBCIPassport load()
      {
        return ppt;
      }
      
      /**
       * @see de.willuhn.jameica.hbci.PassportLoader#reload()
       */
      @Override
      public void reload()
      {
      }
    };
    return new PinTanConfigImpl(loader,new File(ppt.getFileName()));
  }
  
  /**
   * Liefert das zu verwendende PIN/TAN-Sicherheitsverfahren.
   * @param passport der HBCI-Passport.
   * @param validMechs Liste der verfuegbaren Verfahren.
   * @return PIN/TAN-Sicherheitsverfahren oder NULL, wenn keines ermittelbar ist.
   */
  public static String getPinTanSecMech(HBCIPassport passport, String validMechs)
  {
    // 1) Wir versuchen erstmal, das Verfahren ueber den Passport zu ermitteln
    // Achtung: ppt.getCurrentTANMethod(false) kann eine Rekursion ausloesen,
    // wenn die Member-Variable "currentTANMethod" in AbstractPinTanPassport
    // noch NULL ist - daher der Check mit "recursion"
    if (!recursion && passport != null && (passport instanceof HBCIPassportPinTan))
    {
      recursion = true;
      try
      {
        // Checken, ob wir ein fest vorgegebenes TAN-Verfahren haben
        final PinTanConfig config = getPinTanConfig(passport);
        PtSecMech mech = config.getStoredSecMech();
        if (mech == null)
          mech = config.getCurrentSecMech();
        String secMech = mech != null ? mech.getId() : null;
        if (secMech != null && secMech.length() > 0)
        {
          Logger.info("using pintan secmech from passport config: " + mech);
          return secMech;
        }
        
        // Checken, ob wir im Passport eines haben
        final HBCIPassportPinTan ppt = (HBCIPassportPinTan) passport;
        secMech = ppt.getCurrentTANMethod(false);
        if (secMech != null && secMech.length() > 0)
        {
          Logger.info("using pintan secmech from passport: " + secMech);
          return secMech;
        }
      }
      catch (Exception e)
      {
        Logger.error("unable to determine pintan secmech for passport, fallback to default secmech",e);
      }
      finally
      {
        recursion = false;
      }
    }
    

    // 2) Mal schauen, ob wir eine Auswahl von TAN-Verfahren haben.
    // Wenn wir welche haben, nehmen wir das erste gefundene.
    if (validMechs != null && validMechs.length() > 0)
    {
      String[] s = validMechs.split("\\|");
      if (s != null && s.length > 0)
      {
        // Wir nehmen immer das erste, wenn es gueltig ist
        if (s[0] != null && s[0].length() > 0 && s[0].indexOf(":") != -1)
        {
          String secMech = s[0].substring(0,s[0].indexOf(":"));
          if (secMech.length() > 0)
          {
            Logger.info("using first pintan secmech from list: " + secMech);
            return secMech;
          }
        }
      }
    }

    // 3) Wir haben weder im Passport was, noch HBCI4Java konnte etwas
    // liefern. Dann halt der Default-Wert aus der Config.
    String defaultSecMech = settings.getString("pintan.secmech","900");
    if (defaultSecMech != null && defaultSecMech.length() > 0)
    {
      Logger.info("using default pintan secmech: " + defaultSecMech);
      return defaultSecMech;
    }
    
    return null;
  }

  /**
   * speichert die TAN-Medienbezeichnung.
   * @param passport der HBCI-Passport.
   * @param tanmedia die TAN-Medienbezeichnung.
   */
  public static void setPinTanMedia(HBCIPassport passport, String tanmedia)
  {
    try
    {
      final PinTanConfig config = getPinTanConfig(passport);
      config.setTanMedia(tanmedia);
    }
    catch (Exception e)
    {
      Logger.error("unable to save tan media name " + tanmedia + " in passport config ",e);
    }
  }

  /**
   * Liefert die TAN-Medienbezeichnung.
   * Es findet keine Benutzerinteraktion statt. Wenn sie nicht ermittelbar ist, wird NULL geliefert.
   * @param passport der HBCI-Passport.
   * @return PIN/TAN-Medienbezeichnung.
   */
  public static String getPinTanMedia(HBCIPassport passport)
  {
    try
    {
      final PinTanConfig config = getPinTanConfig(passport);
      return config.getTanMedia();
    }
    catch (Exception e)
    {
      Logger.error("unable to determin tan media name",e);
    }
    return null;
  }

  
  /**
   * Liefert die Beginn-Uhrzeit, ab der der Scheduler aussetzen soll.
   * Da Banken nachts ihre Buchungslaeufe durchfuehren, werden die HBCI-System
   * in dieser Zeit oft deaktiviert. Damit der Payment-Server hierdurch
   * nicht unnoetig Fehler produziert, kann er in dieser Zeit pausiert werden.
   * @return Beginn-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public static int getSchedulerExcludeFrom()
  {
    return settings.getInt("scheduler.exclude.from",23);
  }
  
  /**
   * Speichert die Beginn-Uhrzeit, ab der der Scheduler aussetzen soll.
   * @param hour Beginn-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   * @see Settings#getSchedulerExcludeFrom()
   */
  public static void setSchedulerExcludeFrom(int hour)
  {
    settings.setAttribute("scheduler.exclude.from",hour);
  }
  
  /**
   * Liefert die End-Uhrzeit, bis zu der der Scheduler aussetzen soll.
   * Da Banken nachts ihre Buchungslaeufe durchfuehren, werden die HBCI-System
   * in dieser Zeit oft deaktiviert. Damit der Payment-Server hierdurch
   * nicht unnoetig Fehler produziert, kann er in dieser Zeit pausiert werden.
   * @return End-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public static int getSchedulerExcludeTo()
  {
    return settings.getInt("scheduler.exclude.to",05);
  }
  
  /**
   * Speichert die End-Uhrzeit, bis zu der der Scheduler aussetzen soll.
   * @param hour End-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   * @see Settings#getSchedulerExcludeTo()
   */
  public static void setSchedulerExcludeTo(int hour)
  {
    settings.setAttribute("scheduler.exclude.to",hour);
  }
  
  /**
   * Liefert true, wenn der Scheduler am genannten Tag aussetzen soll.
   * @param day der Tag - gemass {@link java.util.Calendar#MONDAY},{@link java.util.Calendar#TUESDAY},...
   * @return true, wenn der Scheduler am genannten Tag aussetzen soll.
   */
  public static boolean getSchedulerExcludeDay(int day)
  {
    return settings.getBoolean("scheduler.exclude.day." + day,false);
  }

  /**
   * Legt fest, ob der Scheduler am genannten Tag aussetzen soll.
   * @param day der Tag - gemass {@link java.util.Calendar#MONDAY},{@link java.util.Calendar#TUESDAY},...
   * @param b true, wenn der Scheduler am genannten Tag aussetzen soll.
   */
  public static void setSchedulerExcludeDay(int day, boolean b)
  {
    settings.setAttribute("scheduler.exclude.day." + day,b);
  }

  /**
   * Legt fest, ob der Scheduler im Fehlerfall beendet werden soll.
   * @return true, wenn der Scheduler im Fehlerfall angehalten wird.
   */
  public static boolean getStopSchedulerOnError()
  {
    return settings.getBoolean("scheduler.stoponerror",true);
  }
  
  /**
   * Legt fest, ob der Scheduler im Fehlerfall beendet werden soll.
   * @param stop true, wenn der Scheduler im Fehlerfall angehalten wird.
   */
  public static void setStopSchedulerOnError(boolean stop)
  {
    settings.setAttribute("scheduler.stoponerror",stop);
  }
  
  /**
   * Legt fest, ob die Synchronisierung im Fehlerfall beendet werden soll.
   * @return true, wenn die Synchronisierung im Fehlerfall beendet wird.
   */
  public static boolean getStopSyncOnError()
  {
    return settings.getBoolean("sync.stoponerror",true);
  }
  
  /**
   * Legt fest, ob die Synchronisierung im Fehlerfall beendet werden soll.
   * @param stop true, wenn die Synchronisierung im Fehlerfall beendet wird.
   */
  public static void setStopSyncOnError(boolean stop)
  {
    settings.setAttribute("sync.stoponerror",stop);
  }

  /**
   * Liefert eine optionale Benachrichtigungs-URL, die aufgerufen wird, wenn
   * die Synchronisierung lief. Damit koennen Dritt-Systeme zeitnah reagieren,
   * wenn in Hibiscus neue Daten vorliegen.
   * @return die Benachrichtigungs-URL oder NULL.
   */
  public static String getNotifyUrl()
  {
    return settings.getString("notify.url",null);
  }
  
  /**
   * Speichert die optionale Benachrichtigungs-URL.
   * @param url die Benachrichtigungs-URL.
   */
  public static void setNotifyUrl(String url)
  {
    settings.setAttribute("notify.url",url);
  }

  /**
   * Liefert true, wenn PIN/TAN-Bankzugänge automatisch auf aktualisierte URLs migriert werden sollen, wenn diese bekannt werden.
   * @return true, wenn PIN/TAN-Bankzugänge automatisch migriert werden sollen.
   */
  public static boolean isAutoMigratePinTan()
  {
    return settings.getBoolean("automigrate.pintan",true);
  }
  
  /**
   * Liefert true, wenn VoP-Anfragen generell freigegeben werden sollen.
   * @return true, wenn VoP-Anfragen generell freigegeben werden sollen.
   */
  public static boolean isVoPApprove()
  {
    return settings.getBoolean("vop.approve",false);
  }
  
  /**
   * Liefert das Passwort fuer das Sicherheitsmedium oder null.
   * @param passport das Sicherheitsmedium.
   * @param reason zusaetzlicher Code fuer den Zweck.
   * @return das Ppasswort oder null.
   */
  public static String getHBCIPassword(HBCIPassport passport, int reason)
  {
    String key = getHBCIPasswordKey(passport,reason);
    return getHBCIPassword(key);
  }
  
  /**
   * Liefert das Passwort fuer den Key.
   * @param key der Key.
   * @return das Passwort.
   */
  public static String getHBCIPassword(String key)
  {
    if (key == null || key.length() == 0)
      return null;
    return (String) wallet.get(key);
  }
  
  /**
   * Speichert das HBCI-Passwort explizit fuer den Dateinamen einer Passport-Datei.
   * @param key der Key.
   * @param password
   */
  public static void setHBCIPassword(String key, String password)
  {
    if (key == null || key.length() == 0)
      return;

    try
    {
      if (password == null || password.length() == 0)
        wallet.delete(key);
      else
        wallet.set(key,password);
    }
    catch (Exception e)
    {
      Logger.error("unable to store password for key " + key,e);
    }
  }

  /**
   * Liefert einen Key, mit dem das Passwort im Wallet gespeichert werden kann.
   * @param passport
   * @param reason der konkrete Callback-Zweck (NEED_PASSPHRASE_LOAD, NEED_SOFTPIN usw)
   * @return der Key oder null.
   */
  private static String getHBCIPasswordKey(HBCIPassport passport, int reason)
  {
    if (passport == null)
    {
      Logger.error("no hbci passport given - unable to store password");
      return null;
    }
    
    if ((passport instanceof AbstractRDHSWFileBasedPassport))
    {
      String filename = ((AbstractRDHSWFileBasedPassport) passport).getFilename();
      if (filename != null)
      {
        File f = new File(filename);
        return f.getName();
      }
      return null;
    }
    else if (passport instanceof HBCIPassportPinTan)
    {
      String filename = ((HBCIPassportPinTan) passport).getFileName();
      if (filename != null)
      {
        File f = new File(filename);
        String name = f.getName();
        if (reason == HBCICallback.NEED_PT_PIN)
          name = name + "." + reason;
        return name;
      }
      return null;
    }
    
    else if (passport instanceof HBCIPassportDDV)
    {
      // Fuer die eigentliche Passport-Datei verwenden wir nicht die PIN sondern
      // ein zufaellig ausgewuerfeltes Passwort. Wuerden wir hier ebenfalls die
      // PIN verwenden, bekaemen wir ein Henne-Ei-Problem, falls der User eine
      // zweite DDV-Config anlegt fuer die gleiche Karte anlegt. Dann existiert
      // naemlich bereits eine Passport-Datei (mit den gecachten UPD/BPD). HBCI4Java
      // wird im AbstractHBCIPassport.getInstance("DDV") versuchen, diese zu laden.
      // Zu dem Zeitpunkt existiert jedoch das Passport-Objekt noch nicht (das soll ja gerade
      // da erst erstellt werden) und demzufolge existiert auch die Context-Config
      // in den Persistent-Data noch nicht. Effekt: Wir haben keinen Key und keine PIN.
      // Also nehmen wir die Context-Config nur fuer die eigentliche PIN. Fuer die
      // Passport-Datei nehmen wir ein ausgewuerfeltes Passwort, welches bei allen
      // DDV-Passport-Dateien gleich ist. Bei PIN/TAN wird das auch so aehnlich gemacht.

      // Fuer die bereits angelegten Passport-Dateien muessen wir jedoch eine
      // Migration vornehmen. Das kann allerdings nicht hier im Callback geschehen, sondern
      // muss stattfinden, BEVOR HBCI4Java einen DDV-Passport oeffnet. Die
      // Migration sieht einfach so aus, dass die bereits existierenden Passport-
      // Dateien einfach geloescht werden. HBCI4Java legt sie dann automatisch
      // neu an und verwendet dafuer dann die neuen zufaelligen Passworte von hier.
      // Die Migration findet direkt im init() des Plugins statt. Dort ist sichergestellt,
      // dass das vor dem Oeffnen eines Passports geschieht.
      if (reason == HBCICallback.NEED_PASSPHRASE_LOAD || reason == HBCICallback.NEED_PASSPHRASE_SAVE)
      {
        String passportKey = "ddv.passport";
        String pw = getHBCIPassword(passportKey);
        if (pw != null) // Wir haben schon ein Passwort, also koennen wir den Key verwenden 
          return passportKey;
        
        // Wir erstellen ein neus Passwort
        try
        {
          Logger.info("creating random passport key");
          byte[] pass = new byte[8];
          SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
          random.nextBytes(pass);
          pw = Base64.encode(pass);
        }
        catch (Exception e)
        {
          // Das ist nicht weiter wild. Mit dem Passwort werden nur die BPD/UPD verschluesselt
          // Mit den Daten kann man nicht wirklich viel anfangen.
          Logger.error("unable to create random password via SHA1PRNG, using fallback",e);
          String s = Long.toString(System.nanoTime()) + Double.toString(new Random().nextDouble());
          pw = Base64.encode(s.getBytes());
        }
        Settings.setHBCIPassword(passportKey,pw);
        return passportKey;
      }
      
      
      // Das hier ist jetzt fuer die eigentliche PIN
      
      DDVConfig config = (DDVConfig) ((AbstractHBCIPassport)passport).getPersistentData(PassportHandle.CONTEXT_CONFIG);
      if (config == null)
        return null;

      //////////////////////////////////////////////////////////////////////////
      // MIGRATION fuer den neuen "Multi-DDV-Support"
      // Falls dies eine existierende Server-Installation ist, muessen wir die aktuell
      // unter "__ddv" abgespeicherte PIN vorher noch in den neuen Context-Config-Parameter
      // verschieben
      String old = getHBCIPassword("__ddv");
      if (old != null)
      {
        Logger.info("migrating ddv pin key from __ddv to " + config.getId());
        // jepp, wir haben schon eine PIN, die migrieren wir jetzt auf den neuen Context-Parameter
        setHBCIPassword(config.getId(),old); // Speichern unter dem neuen Key
        setHBCIPassword("__ddv",null);       // Loeschen des alten Key.
      }
      //////////////////////////////////////////////////////////////////////////
      
      return config.getId();
    }
      
    Logger.error("unsupported passport");
    return null;
  }
  
  /**
   * Migriert die alten DDV-Passport-Dateien.
   * Siehe getHBCIPasswordKey - Absatz DDV.
   */
  static void migrateDDV()
  {
    // Nichts zu migrieren
    if (getHBCIPassword("__ddv") == null)
      return;
    
    // Jetzt loeschen wir einfach alle DDV-Passport-Dateien weg. Bei
    // der naechsten Verwendung werden sie von HBCI4Java automatisch
    // neu angelegt.
    File dir = new File(de.willuhn.jameica.hbci.Settings.getWorkPath() + File.separator + "passports");
    if (dir.exists())
    {
      // Die Passport-Dateien von DDV haben keine Dateiendung
      // Die Passport-Dateien von PIN/TAN haben immer die Endung ".pt"
      // Die Passport-Dateien von RDH haben den originalen Dateinamen, wie
      // sie vom User hochgeladen wurde. 
      de.willuhn.jameica.system.Settings rdh = new de.willuhn.jameica.system.Settings(RDHKeyFactory.class);
      String[] keys = rdh.getList("key",new String[0]);
      Map<String,File> lookup = new HashMap<String,File>();
      for (String s:keys)
      {
        File f = new File(s);
        lookup.put(f.getName(),f);
      }
      
      FileFinder finder = new FileFinder(dir);
      File[] files = finder.find();
      for (File f:files)
      {
        String name = f.getName();
        if (name.endsWith(".pt"))
          continue; // PIN/TAN-Passport
        if (lookup.containsKey(name))
          continue; // RDH-Passport

        // Jetzt koennen eigentlich nur noch DDV-Passports uebrig sein.
        // Wir testen einfach mal, ob wir die Datei mit dem Passwort
        // lesen koennen. Wenn ja, ist es die richtige
        if (!name.matches("^[0-9_]*$")) // Darf nur Zahlen und Unterstrich enthalten
          continue;
        Logger.info("migrating ddv passport " + f);
        f.delete();
      }
      
    }
  }
}
