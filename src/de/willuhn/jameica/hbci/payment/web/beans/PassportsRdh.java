/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/PassportsRdh.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/03/28 22:28:09 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.datasource.GenericIterator;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.passport.Passport;
import de.willuhn.jameica.hbci.passports.rdh.RDHKeyFactory;
import de.willuhn.jameica.hbci.passports.rdh.keyformat.KeyFormat;
import de.willuhn.jameica.hbci.passports.rdh.rmi.RDHKey;
import de.willuhn.jameica.hbci.passports.rdh.server.PassportImpl;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.MultipleClassLoader;

/**
 * Controller fuer Schluesseldisketten.
 */
public class PassportsRdh extends AbstractPassports
{
  @Request
  private HttpServletRequest request = null;
  
  private String format    = null;
  private String filename  = null;
  private String password  = null;
  private String password2 = null;
  private String version   = null;
  

  /**
   * @see de.willuhn.jameica.hbci.payment.web.beans.AbstractPassports#getImplementationClass()
   */
  Class<? extends Passport> getImplementationClass()
  {
    return PassportImpl.class;
  }

  /**
   * Action-Methode zum Import eines neuen Schluessels.
   * @throws Exception
   */
  public void store() throws Exception
  {
    File f = null;
    RDHKey key = null;
    boolean copied = false;

    // Das ist etwas umstaendlich, weil:
    // Der RequestInputStream kann nur einmal verarbeitet werden. Wenn man
    // am Ende des Streams angekommen ist, ist er verbraucht. Wuerde nun
    // der FrontController selbst checken, ob es sich um ein Multipart/Form-Data-Formular
    // handelt und den Action-Parameter dort rausfischen, dann wuerde er den
    // Inputstream verbrauchen und wir wuerden dann hier alt aussehen. Daher wird
    // die "store"-Action dieser Bean generell ausgefuehrt (via "action"-Attribut in webtools.xml).
    // Die Entscheidung, ob nun tatsaechlich gespeichert wird oder nicht, treffen wir
    // dann hier anhand des Content-Types. Somit haben wir den InputStream
    // fuer uns allein.
    if (!ServletFileUpload.isMultipartContent(request))
      return;

    try
    {
      ////////////////////////////////////////////////////////////////////////////
      // Upload und Aufteilen in Formular-Felder und Dateien
      Map<String,String> params = new HashMap<String,String>(); // Regulaere Formular-Felder
      Map<String,byte[]> files  = new HashMap<String,byte[]>(); // hochgeladene Files
      ServletFileUpload upload = new ServletFileUpload();
      FileItemIterator iter = upload.getItemIterator(request);
      while (iter.hasNext())
      {
        FileItemStream item = iter.next();

        String name         = item.getFieldName();
        InputStream value   = item.openStream();
        if (item.isFormField())
        {
          params.put(name,Streams.asString(value));
        }
        else
        {
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          Streams.copy(value,bos,true);
          files.put(name,bos.toByteArray());
          params.put(name,item.getName()); // der zugehoerige Dateiname
        }
      }
      //
      ////////////////////////////////////////////////////////////////////////////

      this.format    = params.get("format");
      this.password  = params.get("password");
      this.password2 = params.get("password2");
      this.version   = params.get("version");
      this.filename  = params.get("filename");
      
      if (this.format != null)
        this.format = this.format.trim(); // sicher ist sicher bei Klassen-Namen

      byte[] data = files.get("filename");

      if (this.filename == null || data == null || data.length == 0)
        throw new ApplicationException(i18n.tr("Kein hochzuladender Schlüssel angegeben"));
      
      if (this.format == null || this.format.length() == 0)
        throw new ApplicationException(i18n.tr("Kein Schlüssel-Format angegeben"));

      if (this.password == null || this.password.length() == 0)
        throw new ApplicationException(i18n.tr("Kein Passwort angegeben"));

      if (this.password2 == null || this.password2.length() == 0)
        throw new ApplicationException(i18n.tr("Bitte gib das Passwort zur Kontrolle zweimal ein"));

      if (!this.password.equals(this.password2))
        throw new ApplicationException(i18n.tr("Passwörter stimmen nicht überein."));

      String path = de.willuhn.jameica.hbci.Settings.getWorkPath() + File.separator + "passports";
      f = new File(path, this.filename);
      if (f.exists())
        throw new ApplicationException(i18n.tr("Schlüsseldiskette {0} existiert bereits",f.getAbsolutePath()));

      
      
      // 1. Kopieren des Keys
      Logger.info("copying " + this.filename + " to " + f.getAbsolutePath());
      OutputStream os = new FileOutputStream(f);
      os.write(data);
      os.close(); // nicht im finally Block, weil die Datei gleich wieder gelesen wird
      copied = true;
      
      // 2. Importieren
      Logger.info("importing key");
      MultipleClassLoader loader = Application.getPluginLoader().getManifest(HBCI.class).getClassLoader();
      KeyFormat keyFormat = (KeyFormat) loader.load(this.format).newInstance();
      key = keyFormat.importKey(f);
      key.setHBCIVersion(this.version);
      RDHKeyFactory.addKey(key);
      
      // 3. Passwort speichern
      Logger.info("saving password in wallet");
      de.willuhn.jameica.hbci.payment.Settings.setHBCIPassword(filename,password);

      // 4. Konten abrufen
      Logger.info("fetch accounts");
      HBCIPassport passport = key.load();
      Logger.info("using HBCI version " + this.version);
      
      Konto[] konten = readKonten(new HBCIHandler(this.version,passport));
      if (konten != null)
        key.setKonten(konten); // Konten fest verknuepfen
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Schlüssel importiert und Konten angelegt."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      try
      {
        // Datei loeschen - aber nur, wenn die Datei existiert und wir sie selbst angelegt haben
        if (f != null && f.exists() && copied)
        f.delete();
        
        // Schluessel deregistrieren
        if (key != null)
          RDHKeyFactory.removeKey(key);
      }
      catch (Exception e2)
      {
        Logger.error("unable to remove key " + f,e2);
      }

      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while saving rdh config",e);
        msg = i18n.tr("Fehler beim Import der Schlüsseldiskette: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
  
  }
  
  /**
   * Action zum Loeschen einer Schluesseldatei.
   * @throws Exception
   */
  public void delete() throws Exception
  {
    try
    {
      String file = request.getParameter("key");
      if (file == null || file.length() == 0)
        throw new ApplicationException("Keine zu löschende Schlüsseldiskette ausgewählt");

      File f1 = new File(file);
      
      List<RDHKey> keys = this.getKeys();
      for (RDHKey k:keys)
      {
        String f = k.getFilename();
        if (f == null || f.length() == 0)
          continue;
        
        File f2 = new File(f);
        if (f1.equals(f2))
        {
          // Synchronisierung der Konten komplett deaktivieren - wuerde sonst nur Fehler liefern, weil
          // das Sicherheitsmedium nicht mehr da ist
          Konto[] konten = k.getKonten();
          for (Konto konto:konten)
          {
            Logger.info("disable all synchronize options for account [id: " + konto.getID() + "]");
            SynchronizeOptions options = new SynchronizeOptions(konto);
            options.setAll(false);
          }

          RDHKeyFactory.removeKey(k);
          
          // Wir loeschen jetzt noch die Schluesseldatei physisch
          Logger.info("deleting file " + f1);
          f1.delete();
          Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Schlüsseldiskette gelöscht"),StatusBarMessage.TYPE_SUCCESS));
          return;
        }
      }
    }
    catch (Exception e)
    {
      String msg = e.getMessage();
      if (!(e instanceof ApplicationException))
      {
        Logger.error("error while deleting pin/tan config",e);
        msg = i18n.tr("Fehler beim Löschen der Schlüsseldiskette: {0}",e.getMessage());
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(msg,StatusBarMessage.TYPE_ERROR));
    }
  }

  /**
   * Liefert eine Liste der vorhandenen Schluessel.
   * @return Liste der vorhandenen Schluessel.
   * @throws Exception
   */
  public List<RDHKey> getKeys() throws Exception
  {
    GenericIterator i = RDHKeyFactory.getKeys();
    List<RDHKey> keys = new ArrayList<RDHKey>();
    while (i.hasNext())
    {
      RDHKey k = (RDHKey) i.next();
      if (k.isEnabled())
        keys.add(k);
    }
    return keys;
  }
  
  /**
   * Liefert eine Liste von Schluesselformaten, die fuer den Import verwendet werden koennen.
   * @return Liste von Schluesselformaten.
   * @throws Exception
   */
  public List<KeyFormat> getFormats() throws Exception
  {
    return Arrays.asList(RDHKeyFactory.getKeyFormats(KeyFormat.FEATURE_IMPORT));
  }
  
  /**
   * Prueft, ob die zugehoerige Schluesseldatei existiert.
   * @param key zu testender Schluessel.
   * @return true, wenn die Datei existiert.
   * @throws Exception
   */
  public boolean exists(RDHKey key) throws Exception
  {
    if (key == null)
      return false;
    File f = new File(key.getFilename());
    return f.exists() && f.isFile() && f.canRead();
  }

  /**
   * Liefert den Namen der Klasse des ausgewaehlten Dateiformats.
   * @return format Name der Klasse des ausgewaehlten Dateiformats.
   */
  public String getFormat()
  {
    return format;
  }

  /**
   * Liefert den Dateinamen.
   * @return filename Dateiname.
   */
  public String getFilename()
  {
    return filename;
  }

  /**
   * Liefert das Passwort.
   * @return password das Passwort.
   */
  public String getPassword()
  {
    return password;
  }

  /**
   * Liefert das Kontroll-Passwort.
   * @return password2 das Kontroll-Passwort.
   */
  public String getPassword2()
  {
    return password2;
  }

  /**
   * Liefert die ausgewaehlte HBCI-Version.
   * @return version die HBCI-Version.
   */
  public String getVersion()
  {
    return version;
  }
  
  
}



/**********************************************************************
 * $Log: PassportsRdh.java,v $
 * Revision 1.2  2012/03/28 22:28:09  willuhn
 * @N Einfuehrung eines neuen Interfaces "Plugin", welches von "AbstractPlugin" implementiert wird. Es dient dazu, kuenftig auch Jameica-Plugins zu unterstuetzen, die selbst gar keinen eigenen Java-Code mitbringen sondern nur ein Manifest ("plugin.xml") und z.Bsp. Jars oder JS-Dateien. Plugin-Autoren muessen lediglich darauf achten, dass die Jameica-Funktionen, die bisher ein Object vom Typ "AbstractPlugin" zuruecklieferten, jetzt eines vom Typ "Plugin" liefern.
 * @C "getClassloader()" verschoben von "plugin.getRessources().getClassloader()" zu "manifest.getClassloader()" - der Zugriffsweg ist kuerzer. Die alte Variante existiert weiterhin, ist jedoch als deprecated markiert.
 *
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.7  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.6  2010/12/14 10:48:57  willuhn
 * *** empty log message ***
 *
 * Revision 1.5  2010/10/07 12:20:28  willuhn
 * @N Lizensierungsumfang (Anzahl der zulaessigen Konten) konfigurierbar
 *
 * Revision 1.4  2010/03/04 16:13:31  willuhn
 * @N Kartenleser-Konfiguration
 *
 * Revision 1.3  2010/02/26 16:19:43  willuhn
 * @N Konten loeschen
 *
 * Revision 1.2  2010/02/26 15:22:46  willuhn
 * @N Konten in Liste der Schluesseldisketten anzeigen
 * @N Schluesseldisketten loeschen
 * @B kleinere Bugfixes
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/