/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Format.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.util.Date;

import org.kapott.hbci.manager.HBCIUtils;

import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

/**
 * Bean zum Formatieren von Ausgaben.
 */
public class Format
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  /**
   * Liefert einen Saldo-Text fuer das angegebene Konto.
   * @param k das Konto.
   * @return der Saldo-Text.
   * @throws Exception
   */
  public String getSaldoText(Konto k) throws Exception
  {
    double saldo = k.getSaldo();
    Date date    = k.getSaldoDatum();
    if (!Double.isNaN(saldo) && date != null)
      return HBCI.DECIMALFORMAT.format(saldo) + " " + k.getWaehrung() + " [" + HBCI.LONGDATEFORMAT.format(date) + "]";
    return i18n.tr("Noch kein Saldo abgerufen");
  }
  
  /**
   * Zeigt die Bank zur angegebenen BLZ an.
   * @param blz die BLZ.
   * @return Bank
   * @throws Exception
   */
  public String getInstitut(String blz) throws Exception
  {
    String name = HBCIUtils.getNameForBLZ(blz);
    if (name != null && name.length() > 0)
      return i18n.tr("{0} (BLZ {1})", new String[]{name,blz});
    return blz;
  }
  
  /**
   * Formatiert ein Datum.
   * @param d das Datum.
   * @return das formatierte Datum.
   */
  public String getDatum(Date d)
  {
    return HBCI.DATEFORMAT.format(d);
  }
  
  /**
   * Formatiert ein Datum mit Uhrzeit.
   * @param d das Datum mit Uhrzeit.
   * @return das formatierte Datum.
   */
  public String getLongDatum(Date d)
  {
    return HBCI.LONGDATEFORMAT.format(d);
  }

  /**
   * Formatiert einen Betrag.
   * @param d der Betrag.
   * @return der formatierte Betrag.
   */
  public String getBetrag(double d)
  {
    return HBCI.DECIMALFORMAT.format(d) + " " + HBCIProperties.CURRENCY_DEFAULT_DE;
  }

  /**
   * Escaped eine Pfad-Angabe.
   * @param f der Pfad der Datei.
   * @return der escapte Pfad.
   * @throws Exception
   */
  public String escapePath(String f) throws Exception
  {
    if (f == null || f.length() == 0)
      return "";
    return f.replaceAll("\\\\","/");
  }
}



/**********************************************************************
 * $Log: Format.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.3  2010/02/26 15:38:16  willuhn
 * @B
 *
 * Revision 1.2  2010/02/26 15:22:46  willuhn
 * @N Konten in Liste der Schluesseldisketten anzeigen
 * @N Schluesseldisketten loeschen
 * @B kleinere Bugfixes
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/