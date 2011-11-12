/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/messaging/StatusBarMessageConsumer.java,v $
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

import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;

/**
 * Speichert die letzte empfangene Statusbar-Message.
 */
public class StatusBarMessageConsumer implements MessageConsumer
{
  private static StatusBarMessage last = null;

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
    return new Class[]{StatusBarMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    last = (StatusBarMessage) message;
  }

  /**
   * Liefert die letzte Nachricht.
   * @return die letzte Nachricht oder null.
   */
  public static StatusBarMessage getLastMessage()
  {
    // Wir warten ggf. noch einen kurzen Moment, bis alle Nachrichten zugestellt sind.
    int maxRetries = 0;
    while (Application.getMessagingFactory().getQueueSize() > 0 && maxRetries++ < 4)
    {
      try
      {
        Thread.sleep(100l);
      }
      catch (InterruptedException e)
      {
        break;
      }
    }
    return last;
  }
}


/*********************************************************************
 * $Log: StatusBarMessageConsumer.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.4  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 * Revision 1.3  2007/06/04 15:12:07  willuhn
 * *** empty log message ***
 *
 * Revision 1.2  2007/05/29 13:19:41  willuhn
 * @N Schluessel-Upload
 *
 * Revision 1.1  2007/05/22 15:51:47  willuhn
 * @N Key-Upload
 *
 **********************************************************************/