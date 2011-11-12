/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/messaging/NotifyMessageConsumer.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.messaging;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import de.willuhn.jameica.hbci.messaging.SaldoMessage;
import de.willuhn.jameica.hbci.payment.Settings;
import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Level;
import de.willuhn.logging.Logger;

/**
 * Benachrichtigt ein Fremdsystem, wenn in den Einstellungen eine
 * entsprechende URL angegeben ist.
 */
public class NotifyMessageConsumer implements MessageConsumer
{

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
   */
  public boolean autoRegister()
  {
    return true;
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
   */
  public Class[] getExpectedMessageTypes()
  {
    return new Class[]{SaldoMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    // Checken, ob wir ein Konto haben
    SaldoMessage msg = (SaldoMessage) message;
    Object o = msg.getObject();
    if (o == null || !(o instanceof Konto))
      return;
    
    // Checken, ob wir ueberhaupt eine URL haben.
    String url = Settings.getNotifyUrl();
    if (url == null || url.length() == 0)
      return;

    Konto k = (Konto) o;
    String data = "context=" + JsonUtil.toJson(k).toString();


    TrustMessageConsumer tmc = null;
    HttpURLConnection conn = null;
    try
    {
      if (url.toLowerCase().startsWith("https"))
      {
        Logger.info("using SSL");
        tmc = new TrustMessageConsumer();
        Application.getMessagingFactory().registerMessageConsumer(tmc);
      }

      URL u = new URL(url);
      Logger.info("notifying " + u + " for \"" + k.getBezeichnung() + "\"");
      conn = (HttpURLConnection) u.openConnection();
      conn.setAllowUserInteraction(false);
      conn.setInstanceFollowRedirects(true);
      conn.setUseCaches(false);
      conn.setDoInput(true);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      conn.setRequestProperty("Content-length",""+data.length()); 
      conn.setRequestMethod("POST");
      conn.connect();

      DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
      writer.writeBytes(data);
      
      int rc = conn.getResponseCode();
      if (rc != HttpURLConnection.HTTP_OK)
        throw new IOException("received HTTP response code " + rc + " [Message: " + conn.getResponseMessage() + "]");
    }
    catch (Exception e)
    {
      Logger.write(Level.INFO,"unable to notify url " + url,e);
    }
    finally
    {
      try
      {
        if (conn != null)
          conn.disconnect();
      }
      catch (Exception e)
      {
        // ignore
      }

      finally
      {
        if (tmc != null)
          Application.getMessagingFactory().unRegisterMessageConsumer(tmc);
      }
    }
  }
}



/**********************************************************************
 * $Log: NotifyMessageConsumer.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.2  2011/01/27 13:52:45  willuhn
 * @B TrustMessageConsumer fehlte - fuer den Fall, dass die Notify-URL HTTPS verwendet
 *
 * Revision 1.1  2010/06/14 11:22:33  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 **********************************************************************/