/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.messaging;

import de.willuhn.jameica.hbci.payment.Settings;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.messaging.QueryMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.logging.Logger;

/**
 * Wird benachrichtigt, wenn es bei der Synchronisierung zu einem Fehler kam.
 * Der User kann dann selbst entscheiden, ob er fortsetzt oder nicht.
 */
public class SynchronizeErrorMessageConsumer implements MessageConsumer
{
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
    QueryMessage msg = (QueryMessage) message;
    
    // wir kuemmern uns nur im Server-Mode um diese Nachricht
    if (!Application.inServerMode())
      return;
    
    boolean b = Settings.getStopSyncOnError();
    Logger.info("stop sync on error: " + b);
    msg.setData(!b); // hier wird gefragt, ob trotz Fehler fortgesetzt werden soll. Daher die Invertierung
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#autoRegister()
   */
  public boolean autoRegister()
  {
    return false; // passiert via plugin.xml
  }
}
