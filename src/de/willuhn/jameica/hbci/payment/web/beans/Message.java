/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Message.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;

import de.willuhn.jameica.hbci.payment.messaging.StatusBarMessageConsumer;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.logging.Logger;

/**
 * Bean fuer Statusbar- und Log-Infos.
 */
public class Message
{
  private DateFormat DF = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

  /**
   * Liefert die letzte Statusbar-Message.
   * @return die letzte Statusbar-Message.
   */
  public StatusBarMessage getLast()
  {
    return StatusBarMessageConsumer.getLastMessage();
  }
  
  /**
   * Liefert den Message-Typ "ERROR".
   * @return Message-Typ "ERROR".
   */
  public int getError()
  {
    return StatusBarMessage.TYPE_ERROR;
  }

  /**
   * Liefert den Message-Typ "SUCCESS".
   * @return Message-Typ "SUCCESS".
   */
  public int getSuccess()
  {
    return StatusBarMessage.TYPE_SUCCESS;
  }
  
  /**
   * Liefert die letzten Log-Zeilen.
   * @return die letzten Log-Zeilen.
   */
  public List<Map> getLog()
  {
    List<Map> lines = new ArrayList<Map>();
    
    de.willuhn.logging.Message[] last = Logger.getLastLines();
    for (int i=last.length-1;i>=0;--i) // Wir iterieren rueckwaerts
    {
      String loggingClass = last[i].getLoggingClass();
      loggingClass = loggingClass.substring(loggingClass.lastIndexOf('.')+1);
      Map<String,String> entry = new HashMap<String,String>();
      entry.put("class",loggingClass);
      entry.put("date",DF.format(last[i].getDate()));
      entry.put("text",StringEscapeUtils.escapeXml(last[i].getText()));
      entry.put("level",last[i].getLevel().getName());
      lines.add(entry);
    }
    return lines;
  }

}



/**********************************************************************
 * $Log: Message.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/