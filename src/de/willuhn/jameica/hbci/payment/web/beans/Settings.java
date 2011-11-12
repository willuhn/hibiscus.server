/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Settings.java,v $
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

package de.willuhn.jameica.hbci.payment.web.beans;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLElement;
import net.n3.nanoxml.XMLParserFactory;
import net.n3.nanoxml.XMLWriter;
import de.willuhn.datasource.Service;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.sensors.notify.Rule;
import de.willuhn.jameica.sensors.notify.notifier.Mail;
import de.willuhn.jameica.sensors.notify.operator.SmallerThan;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.jameica.xmlrpc.rmi.XmlRpcServiceDescriptor;
import de.willuhn.logging.Logger;
import de.willuhn.util.I18N;


/**
 * Eine Bean zum Laden und Speichern der Einstellungen sowie zum Hochladen von Keys.
 */
public class Settings
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  private Rule rule = null;
  
  @Request
  private HttpServletRequest request = null;
  
  /**
   * Liefert das Scheduler-Intervall in Minuten.
   * @return Scheduler-Intervall.
   */
  public int getSchedulerInterval()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerInterval();
  }
  
  /**
   * Liefert die Beginn-Uhrzeit (in Stunden), ab der der Scheduler aussetzen soll.
   * @return Beginn-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public int getSchedulerExcludeFrom()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeFrom();
  }

  /**
   * Speichert die Beginn-Uhrzeit, ab der der Scheduler aussetzen soll.
   * @param hour Beginn-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public void setSchedulerExcludeFrom(String hour)
  {
    try
    {
      int newValue = Integer.parseInt(hour);
      int oldValue = getSchedulerExcludeFrom();
      if (oldValue == newValue)
        return;
      
      de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeFrom(newValue);
    }
    catch (Exception e)
    {
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Ungültige Uhrzeit: {0}.",hour), StatusBarMessage.TYPE_ERROR));
    }
  }

  /**
   * Speichert die End-Uhrzeit, bis zu der der Scheduler aussetzen soll.
   * @param hour End-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public void setSchedulerExcludeTo(String hour)
  {
    try
    {
      int newValue = Integer.parseInt(hour);
      int oldValue = getSchedulerExcludeTo();
      if (oldValue == newValue)
        return;
      
      de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeTo(newValue);
    }
    catch (Exception e)
    {
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Ungültige Uhrzeit: {0}.",hour), StatusBarMessage.TYPE_ERROR));
    }
  }

  /**
   * Liefert die End-Uhrzeit, bis zu der der Scheduler aussetzen soll.
   * @return End-Uhrzeit fuer das Aussetzen des Schedulers (Angabe in Stunden).
   */
  public int getSchedulerExcludeTo()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeTo();
  }
  
  /**
   * Liefert true, wenn der Scheduler am genannten Tag aussetzen soll.
   * @param day der Tag - gemass {@link java.util.Calendar#MONDAY},{@link java.util.Calendar#TUESDAY},...
   * @return true, wenn der Scheduler am genannten Tag aussetzen soll.
   */
  public boolean getSchedulerExcludeDay(int day)
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(day);
  }

  /**
   * Uebernimmt das Scheduler-Intervall und startet bei Bedarf den Service neu. 
   * @param interval
   */
  public void setSchedulerInterval(String interval)
  {
    if (interval == null || interval.length() == 0)
      return;
    
    try
    {
      int newInterval = Integer.parseInt(interval);
      int oldInterval = getSchedulerInterval();
      if (oldInterval == newInterval)
        return;
      
      de.willuhn.jameica.hbci.payment.Settings.setSchedulerInterval(newInterval);
      
      // Scheduler neu starten
      Service scheduler = Application.getServiceFactory().lookup(Plugin.class,"scheduler");
      if (scheduler.isStarted())
      {
        scheduler.stop(true);
        scheduler.start();
        Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Intervall geändert und Scheduler neu gestartet."), StatusBarMessage.TYPE_SUCCESS));
      }
      else
      {
        Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Intervall geändert."), StatusBarMessage.TYPE_SUCCESS));
      }
      
    }
    catch (NumberFormatException nfe)
    {
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Ungültiges Intervall: {0}.", interval), StatusBarMessage.TYPE_ERROR));
    }
    catch (Exception e)
    {
      Logger.error("error while applying new scheduler interval: " + interval,e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Ändern des Intervalls."), StatusBarMessage.TYPE_ERROR));
    }
  }

  /**
   * Liefert das Auftragslimit.
   * @return das Auftragslimit.
   */
  public String getHbciJobLimit()
  {
    return HBCI.DECIMALFORMAT.format(de.willuhn.jameica.hbci.Settings.getUeberweisungLimit());
  }
  
  /**
   * Speichert das Limit fuer HBCI-Auftraege.
   * @param limit
   */
  public void setHbciJobLimit(String limit)
  {
    try
    {
      Number n = HBCI.DECIMALFORMAT.parse(limit);
      double oldLimit = de.willuhn.jameica.hbci.Settings.getUeberweisungLimit();
      double newLimit = n.doubleValue();
      if (oldLimit == newLimit)
        return;
      
      de.willuhn.jameica.hbci.Settings.setUeberweisungLimit(newLimit);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Auftragslimit geändert."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Ungültiges Auftragslimit: {0}.",limit), StatusBarMessage.TYPE_ERROR));
    }
  }
  
  /**
   * Prueft, ob der Scheduler im Fehlerfall gestoppt werden soll.
   * @return true, wenn der Scheduler im Fehlerfall gestoppt werden soll.
   */
  public boolean getStopSchedulerOnError()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getStopSchedulerOnError();
  }
  
  /**
   * Prueft, ob der Scheduler Montags aussetzen soll.
   * @return true, wenn der Scheduler Montags aussetzen soll.
   */
  public boolean getSchedulerExcludeMon()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.MONDAY);
  }

  /**
   * Prueft, ob der Scheduler Dienstags aussetzen soll.
   * @return true, wenn der Scheduler Dienstags aussetzen soll.
   */
  public boolean getSchedulerExcludeTue()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.TUESDAY);
  }

  /**
   * Prueft, ob der Scheduler Mittwochs aussetzen soll.
   * @return true, wenn der Scheduler Mittwochs aussetzen soll.
   */
  public boolean getSchedulerExcludeWed()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.WEDNESDAY);
  }

  /**
   * Prueft, ob der Scheduler Donnerstags aussetzen soll.
   * @return true, wenn der Scheduler Donnerstags aussetzen soll.
   */
  public boolean getSchedulerExcludeThu()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.THURSDAY);
  }

  /**
   * Prueft, ob der Scheduler Freitags aussetzen soll.
   * @return true, wenn der Scheduler Freitags aussetzen soll.
   */
  public boolean getSchedulerExcludeFri()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.FRIDAY);
  }

  /**
   * Prueft, ob der Scheduler Samstags aussetzen soll.
   * @return true, wenn der Scheduler Samstags aussetzen soll.
   */
  public boolean getSchedulerExcludeSat()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.SATURDAY);
  }

  /**
   * Prueft, ob der Scheduler Sonntags aussetzen soll.
   * @return true, wenn der Scheduler Sonntags aussetzen soll.
   */
  public boolean getSchedulerExcludeSun()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getSchedulerExcludeDay(Calendar.SUNDAY);
  }
  
  /**
   * Liefert eine optionale Benachrichtigungs-URL, die aufgerufen wird, wenn
   * die Synchronisierung lief. Damit koennen Dritt-Systeme zeitnah reagieren,
   * wenn in Hibiscus neue Daten vorliegen.
   * @return die Benachrichtigungs-URL oder NULL.
   */
  public String getNotifyUrl()
  {
    return de.willuhn.jameica.hbci.payment.Settings.getNotifyUrl();
  }
  
  /**
   * Speichert die optionale Benachrichtigungs-URL.
   * @param url die Benachrichtigungs-URL.
   */
  public void setNotifyUrl(String url)
  {
    de.willuhn.jameica.hbci.payment.Settings.setNotifyUrl(url);
  }

  /**
   * Liefert den SMTP-Hostnamen.
   * @return der SMTP-Hostname.
   */
  public String getSmtpHost()
  {
    return getRule().getParams().get("smtp.host");
  }

  /**
   * Speichert den SMTP-Hostnamen.
   * @param smtpHost smtpHost der SMTP-Hostname.
   */
  public void setSmtpHost(String smtpHost)
  {
    getRule().getParams().put("smtp.host",smtpHost);
  }

  /**
   * Liefert den SMTP-Usernamen.
   * @return der SMTP-Username.
   */
  public String getSmtpUsername()
  {
    return getRule().getParams().get("smtp.username");
  }

  /**
   * Speichert den SMTP-Usernamen.
   * @param smtpUsername der SMTP-Username.
   */
  public void setSmtpUsername(String smtpUsername)
  {
    getRule().getParams().put("smtp.username",smtpUsername);
  }

  /**
   * Liefert das SMTP-Passwort.
   * @return das SMTP-Passwort.
   */
  public String getSmtpPassword()
  {
    return getRule().getParams().get("smtp.password");
  }

  /**
   * Speichert das SMTP-Passwort.
   * @param smtpPassword das SMTP-Passwort.
   */
  public void setSmtpPassword(String smtpPassword)
  {
    getRule().getParams().put("smtp.password",smtpPassword);
  }

  /**
   * Liefert den Absender der Mail.
   * @return der Absender der Mail.
   */
  public String getMailSender()
  {
    return getRule().getParams().get("mail.sender");
  }

  /**
   * Speichert den Absender der Mail.
   * @param mailSender der Absender der Mail.
   */
  public void setMailSender(String mailSender)
  {
    getRule().getParams().put("mail.sender",mailSender);
  }

  /**
   * Liefert die Mail-Empfaenger.
   * @return die Mail-Empfaenger.
   */
  public String getMailRecipients()
  {
    return getRule().getParams().get("mail.recipients");
  }

  /**
   * Speichert die Mail-Empfaenger.
   * @param mailRecipients die Mail-Empfaenger.
   */
  public void setMailRecipients(String mailRecipients)
  {
    getRule().getParams().put("mail.recipients",mailRecipients);
  }

  /**
   * Legt fest, ob die Benachrichtigung aktiviert sein soll.
   * @return true, wenn die Benachrichtigung aktiviert sein soll.
   */
  public boolean getNotifyEnabled()
  {
    return getRule().isEnabled();
  }

  /**
   * Legt fest, ob die Benachrichtigung aktiviert sein soll.
   * @param notifyEnabled true, wenn die Benachrichtigung aktiviert sein soll.
   */
  public void setNotifyEnabled(String notifyEnabled)
  {
    getRule().setEnabled("true".equalsIgnoreCase(notifyEnabled));
  }

  /**
   * Laedt die Benachrichtigungseinstellungen.
   */
  private synchronized Rule getRule()
  {
    if (this.rule != null)
      return this.rule;
    
    File dir = new File(Application.getPluginLoader().getPlugin(de.willuhn.jameica.sensors.Plugin.class).getResources().getWorkPath(),"rules");
    File f = new File(dir,"hibiscus.server.xml");
    if (f.exists())
    {
      InputStream is = null;
      try
      {
        is = new BufferedInputStream(new FileInputStream(f));
        IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
        parser.setReader(new StdXMLReader(is));
        IXMLElement x = (IXMLElement) parser.parse();
        this.rule = new Rule(x.getFirstChildNamed("rule"));
        return this.rule;
      }
      catch (Exception e)
      {
        Logger.error("unable to read notify settings",e);
        Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Lesen der Benachrichtigungseinstellungen: {0}.",e.getMessage()), StatusBarMessage.TYPE_ERROR));
      }
      finally
      {
        if (is != null)
        {
          try
          {
            is.close();
          }
          catch (Exception e)
          {
            Logger.error("unable to close inputstream",e);
          }
        }
      }
    }
    this.rule = new Rule();
    this.rule.setEnabled(false);
    return this.rule;
  }
  
  /**
   * Liefert true, wenn XML-RPC NULL unterstuetzen soll.
   * @return true, wenn XML-RPC NULL unterstuetzen soll.
   */
  public boolean getXmlRpcNullSupported()
  {
    return de.willuhn.jameica.hbci.xmlrpc.Settings.isNullSupported();
  }
  
  /**
   * Legt fest, ob XML-RPC NULL unterstuetzen soll.
   * @param b true, wenn XML-RPC NULL unterstuetzen soll.
   */
  public void setXmlRpcNullSupported(String b)
  {
    de.willuhn.jameica.hbci.xmlrpc.Settings.setNullSupported("true".equalsIgnoreCase(b));
  }
  
  /**
   * Liefert die Liste der XML-RPC-Services.
   * @return Liste der XML-RPC-Services.
   */
  public List<XmlRpcServiceDescriptor> getXmlRpcServices()
  {
    // Wir fischen uns nur die aus hibiscus.xmlrpc raus
    XmlRpcServiceDescriptor[] list = de.willuhn.jameica.xmlrpc.Settings.getServices();
    List<XmlRpcServiceDescriptor> result = new ArrayList<XmlRpcServiceDescriptor>();
    for (XmlRpcServiceDescriptor s:list)
    {
      try
      {
        String plugin = s.getPluginName();
        if (plugin == null || !plugin.equals("hibiscus.xmlrpc"))
          continue;
        result.add(s);
      }
      catch (Exception e)
      {
        Logger.error("unable to load xml-rpc service, skipping",e);
      }
    }
    return result;
  }
  
  /**
   * Action zum Speichern der Systemeinstellungen.
   */
  public void storeSettings()
  {
    // Im folgenden nur die Checkboxen. Alle anderen Optionen werden direkt
    // via Setter gespeichert.
    de.willuhn.jameica.hbci.payment.Settings.setStopSchedulerOnError(request.getParameter("stopSchedulerOnError") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.MONDAY,   request.getParameter("schedulerExcludeMon") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.TUESDAY,  request.getParameter("schedulerExcludeTue") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.WEDNESDAY,request.getParameter("schedulerExcludeWed") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.THURSDAY, request.getParameter("schedulerExcludeThu") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.FRIDAY,   request.getParameter("schedulerExcludeFri") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.SATURDAY, request.getParameter("schedulerExcludeSat") != null);
    de.willuhn.jameica.hbci.payment.Settings.setSchedulerExcludeDay(Calendar.SUNDAY,   request.getParameter("schedulerExcludeSun") != null);
  }
  
  /**
   * Action zum Speichern der Benachrichtigungseinstellungen.
   */
  public void storeNotify()
  {
    OutputStream os = null;

    try
    {
      Rule r = this.getRule();
      r.setEnabled(this.rule.isEnabled() && this.getStopSchedulerOnError());
      r.setLimit("1");
      r.setOperator(new SmallerThan());
      r.setNotifier(new Mail());
      r.setSensor("hibiscus.server.device.scheduler.status");
      r.getParams().put("mail.subject.inside", i18n.tr("[Hibiscus Payment-Server] Scheduler-Status: OK"));
      r.getParams().put("mail.subject.outside",i18n.tr("[Hibiscus Payment-Server] Scheduler-Status: GESTOPPT"));
      
      IXMLElement rules = new XMLElement("rules");
      rules.addChild(r.toXml());

      File dir = new File(Application.getPluginLoader().getPlugin(de.willuhn.jameica.sensors.Plugin.class).getResources().getWorkPath(),"rules");
      if (!dir.exists())
        dir.mkdirs();
      
      File f = new File(dir,"hibiscus.server.xml");
      os = new BufferedOutputStream(new FileOutputStream(f));
      XMLWriter writer = new XMLWriter(os);
      writer.write(rules,true);
      os.flush();

      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Benachrichtigungseinstellungen gespeichert."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      Logger.error("unable to store notify settings",e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Speichern der Benachrichtigungseinstellungen: {0}.",e.getMessage()), StatusBarMessage.TYPE_ERROR));
    }
    finally
    {
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (Exception e)
        {
          Logger.error("unable to close outputstream",e);
        }
      }
    }
  }
  
  /**
   * Action-Methode zum Speichern der XML-RPC-Einstellungen
   */
  public void storeXmlrpc()
  {
    try
    {
      List<XmlRpcServiceDescriptor> list = getXmlRpcServices();
      for (XmlRpcServiceDescriptor d:list)
      {
        String value = request.getParameter("xmlrpc_" + d.getServiceName());
        boolean start = "true".equalsIgnoreCase(value);
        d.setShared(start);
      }
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("XML-RPC Einstellungen gespeichert."), StatusBarMessage.TYPE_SUCCESS));
    }
    catch (Exception e)
    {
      Logger.error("unable to store xml-rpc settings",e);
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Fehler beim Speichern der XML-RPC Einstellungen: {0}.",e.getMessage()), StatusBarMessage.TYPE_ERROR));
    }
  }

}


/**********************************************************************
 * $Log: Settings.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.7  2011/01/26 18:37:10  willuhn
 * @N XML-RPC-Einstellungen
 *
 * Revision 1.6  2010/06/14 11:22:33  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 * Revision 1.5  2010/05/17 12:44:30  willuhn
 * @N Einzelne Wochentage koennen nun von der Synchronisierung ausgeschlossen werden. Ist z.Bsp. sinnvoll, wenn die Bank am Wochenende eher schlecht/gar nicht erreichbar ist
 *
 * Revision 1.4  2010/04/08 12:40:02  willuhn
 * *** empty log message ***
 *
 * Revision 1.3  2010/03/24 12:09:47  willuhn
 * @N GUI-Polishing
 *
 * Revision 1.2  2010/03/23 18:35:32  willuhn
 * @N Mail-Benachrichtigung (jameica.sensors) via Webfrontend konfigurierbar
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/
