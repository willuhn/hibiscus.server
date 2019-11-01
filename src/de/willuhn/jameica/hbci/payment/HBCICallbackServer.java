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

import java.util.Date;

import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.jameica.hbci.AbstractHibiscusHBCICallback;
import de.willuhn.jameica.hbci.HBCICallbackSWT;
import de.willuhn.jameica.hbci.passport.PassportHandle;
import de.willuhn.jameica.hbci.payment.messaging.TANMessage;
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
public class HBCICallbackServer extends AbstractHibiscusHBCICallback
{
  private HBCICallback parent = null;

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
        break;

      case NEED_PT_TAN:
        Logger.info("sending TAN message");
        
        BeanService service = Application.getBootLoader().getBootable(BeanService.class);
        SynchronizeSession session = service.get(HBCISynchronizeBackend.class).getCurrentSession();
        Konto konto = session != null ? session.getKonto() : null;
        
        TANMessage tm = new TANMessage(msg, passport, konto);
        Application.getMessagingFactory().sendSyncMessage(tm);
        final String tan = tm.getTAN();
        if (tan != null && tan.length() > 0)
        {
          Logger.info("got TAN message response, using TAN");
          retData.replace(0,retData.length(),tan);
          return;
        }
        // Faellt durch bis zum Parent
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
        ((AbstractHBCIPassport)passport).setPersistentData(PassportHandle.CONTEXT_CONFIG,retData.toString());
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
   * @see de.willuhn.jameica.hbci.AbstractHibiscusHBCICallback#status(java.lang.String)
   */
  protected void status(String text)
  {
    Logger.info(text);
  }
}
