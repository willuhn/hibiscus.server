/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/messaging/TrustMessageConsumer.java,v $
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

import de.willuhn.jameica.messaging.CheckTrustMessage;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.logging.Logger;

/**
 * Mit diesem Consumer koennen wir die Trust-Nachrichten fuer
 * neue HBCI-Verbindungen abfangen.
 */
public class TrustMessageConsumer implements MessageConsumer
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
    return new Class[]{CheckTrustMessage.class};
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    if (message == null || !(message instanceof CheckTrustMessage))
      return;
    
    CheckTrustMessage msg = (CheckTrustMessage) message;
    Logger.info("applying trust state to certificate: " + msg.getCertificate().toString());
    msg.setTrusted(true,"Hibiscus Payment-Server");
  }
}


/*********************************************************************
 * $Log: TrustMessageConsumer.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/