/**********************************************************************
 *
 * Copyright (c) by Olaf Willuhn
 * All rights reserved
 * GPLv2
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment;

import java.io.File;

import org.eclipse.jetty.security.LoginService;

import de.willuhn.jameica.plugin.Manifest;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer;
import de.willuhn.jameica.webadmin.server.JameicaLoginService;

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
   * @see de.willuhn.jameica.webadmin.deploy.AbstractWebAppDeployer#getLoginService()
   */
  @Override
  protected LoginService getLoginService()
  {
    if (!Settings.isLoginEnabled())
      return null;
    return new JameicaLoginService();
  }

}
