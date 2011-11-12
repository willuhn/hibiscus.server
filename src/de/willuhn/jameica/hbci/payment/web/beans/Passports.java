/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Passports.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import de.willuhn.jameica.hbci.PassportRegistry;
import de.willuhn.jameica.hbci.passport.Passport;

/**
 * Controller fuer die Liste der Passports.
 */
public class Passports
{
  /**
   * Liefert eine Liste der installierten Passports.
   * @return Liste der installierten Passports.
   * @throws Exception
   */
  public Passport[] getPassports() throws Exception
  {
    return PassportRegistry.getPassports();
  }
  
  /**
   * Liefert einen passenden Link fuer den Passport-Typ.
   * @param p Passport.
   * @return passender Link.
   */
  public String getLink(Passport p)
  {
    if (p == null)
      return "";
    String id = p.getClass().getName();
    id = id.replaceFirst("de\\.willuhn\\.jameica\\.hbci\\.passports\\.","").replaceFirst("\\..*$","");
    return "passports." + id + ".html";
  }
}



/**********************************************************************
 * $Log: Passports.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/