/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/HBCICallbackServer.java,v $
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

package de.willuhn.jameica.hbci.payment;

import java.util.Date;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.callback.HBCICallbackConsole;
import org.kapott.hbci.exceptions.HBCI_Exception;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIUtilsInternal;
import org.kapott.hbci.passport.HBCIPassport;

import de.willuhn.jameica.hbci.AbstractHibiscusHBCICallback;
import de.willuhn.jameica.hbci.HBCICallbackSWT;
import de.willuhn.jameica.hbci.payment.messaging.TANMessage;
import de.willuhn.jameica.hbci.payment.web.beans.PassportsPinTan;
import de.willuhn.jameica.hbci.rmi.Nachricht;
import de.willuhn.jameica.hbci.server.hbci.HBCIFactory;
import de.willuhn.jameica.messaging.QueryMessage;
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
    cacheData(passport);

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
        retData.replace(0,retData.length(),Settings.getPinTanSecMech(passport,retData.toString()));
        return;
        
      case NEED_PT_TAN:
        Logger.info("sending TAN message");
        TANMessage tm = new TANMessage(msg, passport, HBCIFactory.getInstance().getCurrentKonto());
        Application.getMessagingFactory().sendSyncMessage(tm);
        String tan = tm.getTAN();
        if (tan == null || tan.length() == 0)
          throw new HBCI_Exception("No TAN-handler specified or empty TAN returned");
          
        Logger.info("got TAN message response, sending to institute");
        retData.replace(0,retData.length(),tan);
        return;
      ///////////////////////////////////////////////////////////////
        
        
      case NEED_CONNECTION:
      case CLOSE_CONNECTION:
        // Ueberschrieben, weil wir im Server-Mode davon ausgehen,
        // dass eine Internetverbindung verfuegbar ist
        return;
        
      case NEED_PASSPHRASE_LOAD:
      case NEED_PASSPHRASE_SAVE:
      case NEED_SOFTPIN:
      case NEED_PT_PIN:
        String pw = Settings.getHBCIPassword(passport,reason);
        if (pw == null || pw.length() == 0)
          throw new RuntimeException("no password/pin definded for passport " + passport.getClass().getName());

        // Wir haben ein gespeichertes Passwort. Das nehmen wir.
        retData.replace(0,retData.length(),pw);
        return;

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

      case NEED_INFOPOINT_ACK:
        QueryMessage qm = new QueryMessage(msg,retData);
        Application.getMessagingFactory().getMessagingQueue("hibiscus.infopoint").sendSyncMessage(qm);
        retData.replace(0,retData.length(),qm.getData() == null ? "" : "false");
        return;

      case HAVE_IBAN_ERROR:
      case HAVE_CRC_ERROR:
        Logger.error("IBAN/CRC error: " + msg+ " ["+retData.toString()+"]: "); // Muesste ich mal noch behandeln
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

        // Implementiert, weil die Console-Impl Eingaben von STDIN erfordern
      case HAVE_ERROR:
        Logger.error("NOT IMPLEMENTED: " + msg+ " ["+retData.toString()+"]: ");
        throw new HBCI_Exception("reason not implemented");
    }
    parent.callback(passport, reason, msg, datatype, retData);
  }

  /**
   * @see org.kapott.hbci.callback.HBCICallback#status(org.kapott.hbci.passport.HBCIPassport, int, java.lang.Object[])
   */
  public void status(HBCIPassport passport, int statusTag, Object[] o) {
    switch (statusTag) {

      case STATUS_INST_BPD_INIT:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_INST_DATA"));
        break;

      case STATUS_INST_BPD_INIT_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_INST_DATA_DONE",passport.getBPDVersion()));
        break;

      case STATUS_INST_GET_KEYS:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_INST_KEYS"));
        break;

      case STATUS_INST_GET_KEYS_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_INST_KEYS_DONE"));
        break;

      case STATUS_SEND_KEYS:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_SEND_MY_KEYS"));
        break;

      case STATUS_SEND_KEYS_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_SEND_MY_KEYS_DONE"));
        break;

      case STATUS_INIT_SYSID:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_SYSID"));
        break;

      case STATUS_INIT_SYSID_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_SYSID_DONE",o[1].toString()));
        break;

      case STATUS_INIT_SIGID:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_SIGID"));
        break;

      case STATUS_INIT_SIGID_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_SIGID_DONE",o[1].toString()));
        break;

      case STATUS_INIT_UPD:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_USER_DATA"));
        break;

      case STATUS_INIT_UPD_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_REC_USER_DATA_DONE",passport.getUPDVersion()));
        break;

      case STATUS_LOCK_KEYS:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_USR_LOCK"));
        break;

      case STATUS_LOCK_KEYS_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_USR_LOCK_DONE"));
        break;

      case STATUS_DIALOG_INIT:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_INIT"));
        break;

      case STATUS_DIALOG_INIT_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_INIT_DONE",o[1]));
        break;

      case STATUS_SEND_TASK:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_NEW_JOB",((HBCIJob)o[0]).getName()));
        break;

      case STATUS_SEND_TASK_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_JOB_DONE",((HBCIJob)o[0]).getName()));
        break;

      case STATUS_DIALOG_END:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_END"));
        break;

      case STATUS_DIALOG_END_DONE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_DIALOG_END_DONE"));
        break;

      case STATUS_MSG_CREATE:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_CREATE",o[0].toString()));
        break;

      case STATUS_MSG_SIGN:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_SIGN"));
        break;

      case STATUS_MSG_CRYPT:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_CRYPT"));
        break;

      case STATUS_MSG_SEND:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_SEND"));
        break;

      case STATUS_MSG_RECV:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_RECV"));
        break;

      case STATUS_MSG_PARSE:
        Logger.debug(HBCIUtilsInternal.getLocMsg("STATUS_MSG_PARSE",o[0].toString()+")"));
        break;

      case STATUS_MSG_DECRYPT:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_DECRYPT"));
        break;

      case STATUS_MSG_VERIFY:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_MSG_VERIFY"));
        break;
      case STATUS_SEND_INFOPOINT_DATA:
        Logger.info(HBCIUtilsInternal.getLocMsg("STATUS_SEND_INFOPOINT_DATA"));
        break;

      default:
        this.parent.status(passport,statusTag,o);
    }
    
  }
}


/*********************************************************************
 * $Log: HBCICallbackServer.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.22  2010/11/08 12:06:47  willuhn
 * *** empty log message ***
 *
 * Revision 1.21  2010/11/08 11:38:48  willuhn
 * @B Beim Ermitteln der TAN-Verfahren kann initial eine Rekursion auftreten
 *
 * Revision 1.20  2010/09/22 15:05:25  willuhn
 * *** empty log message ***
 *
 * Revision 1.19  2010/09/10 15:52:29  willuhn
 * @N Umstellung auf Multi-DDV-Support - Migration der Passport-Dateien
 *
 * Revision 1.18  2010/03/04 16:51:13  willuhn
 * *** empty log message ***
 *
 * Revision 1.17  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 * Revision 1.16  2008/11/18 13:46:27  willuhn
 * @N Infopoint-Response
 *
 * Revision 1.15  2008/11/18 13:42:18  willuhn
 * @N Infopoint-Response
 *
 * Revision 1.14  2008/09/02 08:24:05  willuhn
 * @B NPE
 *
 * Revision 1.13  2008/05/30 12:31:12  willuhn
 * @N Aufruf der Update-Cache-Funktion
 *
 * Revision 1.12  2008/05/30 12:01:35  willuhn
 * @N Gemeinsame Basisimplementierung des HBCICallback in Hibiscus und Payment-Server
 *
 * Revision 1.11  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 * Revision 1.10  2007/09/01 21:47:25  willuhn
 * @N PIN/TAN Sicherheitsverfahren
 *
 * Revision 1.9  2007/08/31 00:09:14  willuhn
 * @N PIN/TAN-Support
 *
 * Revision 1.8  2007/08/30 21:47:33  willuhn
 * @N Callback-Abfragen fuer PIN/TAN
 *
 * Revision 1.7  2007/08/30 17:56:38  willuhn
 * *** empty log message ***
 *
 * Revision 1.6  2007/08/30 17:44:06  willuhn
 * @N PIN/TAN-Support
 *
 * Revision 1.5  2007/07/20 15:03:48  willuhn
 * @N Instituts-Meldungen direkt speichern. Consolen-Eingaben vermeiden
 *
 * Revision 1.4  2007/05/29 16:58:04  willuhn
 * @N Support fuer Chipkarte
 *
 * Revision 1.3  2007/05/21 00:19:06  willuhn
 * @N key upload
 *
 * Revision 1.2  2007/05/15 17:21:08  willuhn
 * @N Added Scheduler
 *
 * Revision 1.1  2007/05/15 16:40:04  willuhn
 * @N Initial checkin for "Hibiscus Payment-Server"
 *
 **********************************************************************/