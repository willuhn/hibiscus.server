/**********************************************************************
 *
 * Copyright (c) 2019 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.handler;

import java.util.Vector;

import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.passport.HBCIPassportPinTan;

import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.messaging.TANMessage;
import de.willuhn.jameica.messaging.Message;
import de.willuhn.jameica.messaging.MessageConsumer;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.system.Settings;
import de.willuhn.logging.Logger;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.ClassFinder;
import de.willuhn.util.I18N;

/**
 * Eine Registry zum Ermitteln der installierten TAN-Handler.
 */
public class TANHandlerRegistry implements MessageConsumer
{
  private static Settings settings = new Settings(TANHandlerRegistry.class);
  private static TANHandler[] list = null;

  /**
   * Liefert den TAN-Handler zu einer Config.
   * @param config Name der Config.
   * @return der TAN-Haendler.
   */
  public static TANHandler getTANHandler(String config)
  {
    String clazz = settings.getString(config,null);
    if (clazz == null)
      return null;
    
    try
    {
      TANHandler handler = (TANHandler) Application.getClassLoader().load(clazz).newInstance();
      handler.setConfig(config);
      return handler;
    }
    catch (Exception e)
    {
      Logger.error("unable to init TAN handler " + clazz,e);
    }
    return null;
  }
  
  /**
   * Erzeugt eine neuen TAN-Handler.
   * @param clazz Zu verwendende Implementierung.
   * @param config die Config.
   * @return der Handler.
   * @throws ApplicationException
   */
  public static TANHandler createTANHandler(String clazz, String config) throws ApplicationException
  {
    I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

    if (clazz == null)
      throw new ApplicationException(i18n.tr("Keine Handler-Klasse angegeben"));

    try
    {
      TANHandler handler = (TANHandler) Application.getClassLoader().load(clazz).newInstance();
      handler.setConfig(config);
      settings.setAttribute(config,clazz);
      return handler;
    }
    catch (Exception e)
    {
      Logger.error("unable to init TAN handler " + clazz,e);
      throw new ApplicationException(i18n.tr("Handler kann nicht erzeugt werden: {0}",e.getMessage()));
    }
    
  }
  
  /**
   * Liefert eine Liste der verfuegbaren TAN-Handler.
   * @return Liste der verfuegbaren Handler.
   */
  public static synchronized TANHandler[] getTANHandler()
  {
    if (list != null)
      return list;
    
    Logger.info("init TAN-Handlers");

    ClassFinder finder = Application.getClassLoader().getClassFinder();

    Vector v = new Vector();
    Class[] handlers = new Class[0];

    try
    {
      handlers = finder.findImplementors(TANHandler.class);
    }
    catch (ClassNotFoundException ce)
    {
      Logger.error("no tan handlers found",ce);
    }

    TANHandler current = null;
    for (int i=0;i<handlers.length;++i)
    {
      try
      {
        current = (TANHandler) handlers[i].newInstance();
        Logger.info("  found " + current.getName());
        v.add(current);
      }
      catch (Exception e)
      {
        Logger.error("unable to load TAN handler " + handlers.getClass().getName() + ", skipping");
      }
    }
    list = (TANHandler[]) v.toArray(new TANHandler[v.size()]);
    return list;
  }

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
    return new Class[]{TANMessage.class}; 
  }

  /**
   * @see de.willuhn.jameica.messaging.MessageConsumer#handleMessage(de.willuhn.jameica.messaging.Message)
   */
  public void handleMessage(Message message) throws Exception
  {
    if (message == null || !(message instanceof TANMessage))
    {
      Logger.warn("got <null> or invalid TAN message");
      return;
    }
    
    Logger.info("searching for according pin/tan config");
    TANMessage tm = (TANMessage) message;
    HBCIPassport passport = tm.getPassport();
    
    if (passport == null || !(passport instanceof HBCIPassportPinTan))
      throw new Exception("no valid PIN/TAN passport");
    
    HBCIPassportPinTan ppt = (HBCIPassportPinTan) passport;
    
    Logger.info("loading TAN handler for " + ppt.getFileName());
    TANHandler handler = getTANHandler(ppt.getFileName());
    
    if (handler == null)
      throw new Exception("no TAN handler found for config " + ppt.getFileName());

    Logger.info("retrieving TAN from handler");
    handler.getTAN(tm);
  }
}
