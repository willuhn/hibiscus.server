/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/messaging/QueryHBCIVersionMessageConsumer.java,v $
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

package de.willuhn.jameica.hbci.payment.messaging;

import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.QueryMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.Settings;

/**
 * Beantwortet Abfragen zur HBCI-Version.
 */
public class QueryHBCIVersionMessageConsumer implements MessageConsumer
{
  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
   */
  public boolean autoRegister()
  {
    return false;
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#getExpectedMessageTypes()
   */
  public Class[] getExpectedMessageTypes()
  {
    return new Class[]{QueryMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    if (!Application.inServerMode())
      return; // Wenn wir mit GUI laufen, ignorieren wir die Nachricht
    
    QueryMessage msg = (QueryMessage) message;
    Settings settings = Application.getPluginLoader().getPlugin(HBCI.class).getResources().getSettings();
    msg.setData(settings.getString("hbci.version.default","210"));
  }
}


/*********************************************************************
 * $Log: QueryHBCIVersionMessageConsumer.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2008/07/28 09:31:11  willuhn
 * @N Abfrage der HBCI-Version via Messaging
 *
 **********************************************************************/