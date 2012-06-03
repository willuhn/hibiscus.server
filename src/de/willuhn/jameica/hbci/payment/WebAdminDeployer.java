/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/WebAdminDeployer.java,v $
 * $Revision: 1.2 $
 * $Date: 2012/06/03 13:47:45 $
 * $Author: willuhn $
 * $Locker:  $
 * $State: Exp $
 *
 * Copyright (c) by willuhn software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment;

import java.io.File;

import org.mortbay.jetty.security.UserRealm;

import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer;
import de.willuhn.jameica.webadmin.server.JameicaUserRealm;

/**
 * Deployer fuer das Hibiscus-Webfrontend.
 */
public class WebAdminDeployer extends AbstractWebAppDeployer
{
  /**
   * @see de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer#getContext()
   */
  protected String getContext()
  {
    return "/hibiscus";
  }

  /**
   * @see de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer#getPath()
   */
  protected String getPath()
  {
    Manifest mf = Application.getPluginLoader().getManifest(Plugin.class);
    return mf.getPluginDir() + File.separator + "webapps" + File.separator + "hibiscus";
  }

  /**
   * @see de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer#getSecurityRoles()
   */
  protected String[] getSecurityRoles()
  {
    if (!Settings.isLoginEnabled())
      return null;
    return new String[]{"admin"};
  }

  /**
   * @see de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer#getUserRealm()
   */
  protected UserRealm getUserRealm()
  {
    if (!Settings.isLoginEnabled())
      return null;
    return new JameicaUserRealm();
  }

}


/*********************************************************************
 * $Log: WebAdminDeployer.java,v $
 * Revision 1.2  2012/06/03 13:47:45  willuhn
 * @N Login via Config abschaltbar - siehe http://www.onlinebanking-forum.de/phpBB2/viewtopic.php?t=14386
 *
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.3  2009/03/30 13:09:02  willuhn
 * *** empty log message ***
 *
 * Revision 1.2  2007/12/04 12:07:17  willuhn
 * *** empty log message ***
 *
 * Revision 1.1  2007/05/16 16:26:04  willuhn
 * @N Webfrontend
 *
 **********************************************************************/