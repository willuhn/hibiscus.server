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

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.MatrixCode;
import org.kapott.hbci.manager.QRCode;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.annotation.Lifecycle;
import de.willuhn.annotation.Lifecycle.Type;
import de.willuhn.jameica.hbci.AbstractHibiscusHBCICallback;
import de.willuhn.jameica.hbci.HBCICallbackSWT;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.payment.messaging.TANMessage;
import de.willuhn.jameica.hbci.payment.messaging.TANMessage.TANType;
import de.willuhn.jameica.hbci.payment.web.beans.PassportsPinTan;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Nachricht;
import de.willuhn.jameica.hbci.synchronize.SynchronizeSession;
import de.willuhn.jameica.hbci.synchronize.hbci.HBCISynchronizeBackend;
import de.willuhn.jameica.services.BeanService;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Wir ueberschreiben den Hibiscus-Callback, um alle HBCI-Aufrufe ueber uns zu leiten.
 */
@Lifecycle(Type.CONTEXT)
public class HBCICallbackServer extends AbstractHibiscusHBCICallback
{
  private HBCICallback parent = null;

  @Resource private HBCISynchronizeBackend backend = null;

  /**
   * ct.
   */
  public HBCICallbackServer()
  {
    if (!Application.inServerMode())
      this.parent = new HBCICallbackSWT();
    else
      this.parent = new HBCICallbackConsole();
    Logger.info("parent hbci callback: " + this.parent.getClass().getName());
  }
  
  /**
   * @see org.kapott.hbci.callback.HBCICallback#log(java.lang.String, int, java.util.Date, java.lang.StackTraceElement)
   */
  public void log(String msg, int level, Date date, StackTraceElement trace)
  {
    SynchronizeSession session = this.backend.getCurrentSession();

    switch (level)
    {
      case HBCIUtils.LOG_DEBUG2:
      case HBCIUtils.LOG_DEBUG:
        Logger.debug(msg);
        break;

      case HBCIUtils.LOG_INFO:
        Logger.info(msg);
        break;

      case HBCIUtils.LOG_WARN:
        // Die logge ich mit DEBUG - die nerven sonst
        if (msg != null && msg.startsWith("konnte folgenden nutzerdefinierten Wert nicht in Nachricht einsetzen:"))
        {
          Logger.debug(msg);
          break;
        }
        if (msg != null && msg.matches("Algorithmus .* nicht implementiert"))
        {
          Logger.debug(msg);
          break;
        }
        Logger.warn(msg);
        break;

      case HBCIUtils.LOG_ERR:
        
        if (session != null && msg != null)
          session.getErrors().add(msg.replace("HBCI error code: ",""));
        
        Logger.error(msg + " " + trace.toString());
        break;

      default:
        Logger.warn("(unknown log level " + level + "):" + msg);
    }
  }


  /**
   * @see org.kapott.hbci.callback.HBCICallback#callback(org.kapott.hbci.passport.HBCIPassport, int, java.lang.String, int, java.lang.StringBuffer)
   */
  public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData)
  {
    // Ueberschrieben, um die Passwort-Abfragen abzufangen und die Werte
    // zu speichern.
    switch (reason) {
    
      ///////////////////////////////////////////////////////////////
      // PIN/TAN - reichen wir an die PinTanBean durch
      case NEED_COUNTRY:
      case NEED_BLZ:
      case NEED_HOST:
      case NEED_PORT:
      case NEED_FILTER:
      case NEED_USERID:
      case NEED_CUSTOMERID:
        
        String value = (String) PassportsPinTan.SESSION.get(new Integer(reason));
        if (value == null || value.length() == 0)
        {
          Logger.warn("PIN/TAN: have no valid value for callback reason: " + reason);
          retData.replace(0,retData.length(),"");
        }
        else
        {
          Logger.info("applying callback reason " + reason + " via pintan session");
          retData.replace(0,retData.length(),value);
        }
        return;
        
      case NEED_PT_SECMECH:
        Logger.info("GOT PIN/TAN secmech list: " + msg + " ["+retData.toString()+"]");
        ((AbstractHBCIPassport)passport).setPersistentData(PassportHandle.CONTEXT_SECMECHLIST,retData.toString());
        
        // Checken, ob wir den Wert in der Session haben
        String secmech = (String) PassportsPinTan.SESSION.get(new Integer(reason));
        if (secmech != null && secmech.length() > 0)
        {
          // wir haben den Wert in der Session. Da wir jetzt auch den Passport haben, koennen wir den Wert gleich abspeichern
          Settings.setPinTanSecMech(passport,secmech);
          // Aus der Session entfernen - wir haben es ja jetzt fest gespeichert
          PassportsPinTan.SESSION.remove(new Integer(reason));
        }
        else
        {
          // Checken, ob wir ihn schon in der Config haben
          secmech = Settings.getPinTanSecMech(passport,retData.toString());
        }
        
        
        if (secmech != null && secmech.length() > 0)
        {
          Logger.info("using secmech: " + secmech);
          retData.replace(0,retData.length(),secmech);
          return;
        }
        
        // Parent fragen
        parent.callback(passport, reason, msg, datatype, retData);
        if (retData != null && retData.length() > 0)
        {
          // Gleich abspeichern
          Settings.setPinTanSecMech(passport,retData.toString());
        }
        break;

      case NEED_PT_TANMEDIA:
        Logger.info("PIN/TAN media name requested: " + msg + " ["+retData.toString()+"]");
        ((AbstractHBCIPassport)passport).setPersistentData(PassportHandle.CONTEXT_TANMEDIALIST,retData.toString());
        
        // Checken, ob wir den Wert in der Session haben
        String tanmedia = (String) PassportsPinTan.SESSION.get(new Integer(reason));
        if (tanmedia != null && tanmedia.length() > 0)
        {
          // wir haben den Wert in der Session. Da wir jetzt auch den Passport haben, koennen wir den Wert gleich abspeichern
          Settings.setPinTanMedia(passport,tanmedia);
          // Aus der Session entfernen - wir haben es ja jetzt fest gespeichert
          PassportsPinTan.SESSION.remove(new Integer(reason));
        }
        else
        {
          // Checken, ob wir ihn schon in der Config haben
          tanmedia = Settings.getPinTanMedia(passport);
        }

        if (tanmedia != null && tanmedia.length() > 0)
        {
          Logger.info("using tan media name: " + tanmedia);
          retData.replace(0,retData.length(),tanmedia);
          return;
        }
        
        // Parent fragen
        parent.callback(passport, reason, msg, datatype, retData);
        if (retData != null && retData.length() > 0)
        {
          // Gleich abspeichern
          Settings.setPinTanMedia(passport,retData.toString());
        }
        return;

      case NEED_PT_TAN:
      case NEED_PT_PHOTOTAN:
      case NEED_PT_QRTAN:
        
        Logger.info("sending TAN message");
        final String tan = this.getTAN(passport, reason, msg, retData);
        if (tan != null && tan.length() > 0)
        {
          Logger.info("got TAN message response, using TAN");
          retData.replace(0,retData.length(),tan);
          return;
        }
        
        // Wenn wir keine TAN haben, soll es das Parent beantworten
        break;
        
        
      case NEED_CONNECTION:
      case CLOSE_CONNECTION:
        // Ueberschrieben, weil wir im Server-Mode davon ausgehen,
        // dass eine Internetverbindung verfuegbar ist
        return;
        
      case NEED_PASSPHRASE_LOAD:
      case NEED_PASSPHRASE_SAVE:
      case NEED_SOFTPIN:
      case NEED_PT_PIN:
        final String pw = Settings.getHBCIPassword(passport,reason);
        if (pw != null && pw.length() > 0)
        {
          Logger.debug("using stored pin");
          retData.replace(0,retData.length(),pw);
          return;
        }
        // Faellt durch bis zum Parent
        break;

      // Implementiert, weil die Console-Impl Eingaben von STDIN erfordern
      case HAVE_INST_MSG:
        try
        {
          Nachricht n = (Nachricht) de.willuhn.jameica.hbci.Settings.getDBService().createObject(Nachricht.class,null);
          n.setBLZ(passport.getBLZ());
          n.setNachricht(msg);
          n.setDatum(new Date());
          n.store();
        }
        catch (Exception e)
        {
          Logger.error("unable to store system message",e);
        }
        return;

      case HAVE_IBAN_ERROR:
      case HAVE_CRC_ERROR:
        Logger.error("IBAN/CRC error: " + msg+ " ["+retData.toString()+"]: "); // Muesste ich mal noch behandeln
        return;

      case WRONG_PIN:
        Logger.error("detected wrong PIN: " + msg+ " ["+retData.toString()+"]: ");
        return;

      case USERID_CHANGED:
        Logger.info("got changed user/account data (code 3072) - saving in persistent data for later handling");
        ((AbstractHBCIPassport)passport).setPersistentData(PassportHandle.CONTEXT_USERID_CHANGED,retData.toString());
        return;
        
      case HBCICallback.NEED_CHIPCARD:
        Logger.debug("callback: need chipcard");
        return;
      case HBCICallback.HAVE_CHIPCARD:
        Logger.debug("callback: have chipcard");
        return;
        
      case HBCICallback.NEED_HARDPIN:
      case HBCICallback.HAVE_HARDPIN:
        throw new HBCI_Exception("hard pin not allowed in payment server");

      case HBCICallback.NEED_REMOVE_CHIPCARD:
        return;

        // Implementiert, weil die Console-Impl Eingaben von STDIN erfordern
      case HAVE_ERROR:
        Logger.error("NOT IMPLEMENTED: " + msg+ " ["+retData.toString()+"]: ");
        throw new HBCI_Exception("reason not implemented");
    }
    parent.callback(passport, reason, msg, datatype, retData);
  }
  
  /**
   * Fuehrt die TAN-Abfrage durch.
   * @param passport der Passport.
   * @param reason der Callback-Typ.
   * @param msg die Message von HBCI4Java.
   * @param retData Stringbuffer fuer die Austausch-Daten mit HBCI4Java.
   * @return die TAN oder NULL, wenn keine ermittelt werden konnte.
   */
  private String getTAN(HBCIPassport passport, int reason, String msg, StringBuffer retData)
  {
    //////////////////////////////////////////////////////
    // TAN-Typ ermitteln und Payload aufbereiten
    TANType type = TANType.NORMAL;
    String payload = retData.toString();

    // Bei den ersten beiden TAN-Varianten ist es eindeutig anhand des Callbacks erkennbar
    // Bei ChipTAN ist es daran erkennbar, wenn in retData etwas steht - das ist dann der Flickercode.
    if (reason == HBCICallback.NEED_PT_PHOTOTAN || reason == HBCICallback.NEED_PT_QRTAN)
    {
      byte[] data = null;
      String mime = null;
      if (reason == HBCICallback.NEED_PT_PHOTOTAN)
      {
        type = TANType.PHOTOTAN;
        MatrixCode code = MatrixCode.tryParse(payload);
        if (code != null)
        {
          data = code.getImage();
          mime = code.getMimetype();
        }
      }
      else
      {
        type = TANType.QRTAN;
        QRCode code = QRCode.tryParse(payload,msg);
        if (code != null)
        {
          data = code.getImage();
          mime = code.getMimetype();
        }
      }
      
      // Wenn das Parsen eines der beiden Codes geklappt hat, uebernehmen wir ihn Base64-codiert in den Payload
      // Achtung: Wir duerfen hier kein MIME-codiertes Base64 verwenden, weil dort Zeilenumbrueche enthalten sind.
      // Nur "rohes" Base64 gemaess RFC 4648 erlaubt.
      // Ausserdem Mimetype und Encoding vorn dran schreiben
      if (data != null)
      {
        mime = StringUtils.trimToNull(mime);
        if (mime == null)
        {
          Logger.warn("got no mime type from server, using image/png as default");
          mime = "image/png";
        }
        
        Logger.info("image data for TAN payload: " + mime + ", " + data.length + " bytes");
        Encoder enc = Base64.getEncoder();
        payload = "data:" + mime + ";base64," + enc.encodeToString(data);
      }
    }
    else if (retData != null && retData.length() > 0)
    {
      type = TANType.CHIPTAN;
    }
    //
    //////////////////////////////////////////////////////
      
    Logger.info("sending TAN message, type " + type);
    
    
    final BeanService service = Application.getBootLoader().getBootable(BeanService.class);
    final SynchronizeSession session = service.get(HBCISynchronizeBackend.class).getCurrentSession();
    final Konto konto = session != null ? session.getKonto() : null;
    
    TANMessage tm = new TANMessage(msg, passport, konto,type,payload);
    Application.getMessagingFactory().sendSyncMessage(tm);
    return tm.getTAN();
  }
  
  /**
   * @see de.willuhn.jameica.hbci.AbstractHibiscusHBCICallback#status(java.lang.String)
   */
  protected void status(String text)
  {
    Logger.info(text);
  }
}
