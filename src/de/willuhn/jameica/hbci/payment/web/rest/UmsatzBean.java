/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/rest/UmsatzBean.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.rest;

import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.server.UmsatzUtil;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;

/**
 * REST-Bean zum Zugriff auf die Umsaetze.
 */
@Doc("Hibiscus: Liefert Informationen über die Umsätze")
public class UmsatzBean implements AutoRestBean
{
  /**
   * Liefert die Umsaetze der letzten 30 Tage fuer das Konto.
   * @param kontoId ID des Kontos.
   * @return Liste der Umsaetze im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der Umätze der letzten 30 Tage für das mit der ID angegebene Konto im JSON-Format",
      example="hibiscus/konto/2/umsaetze")
  @Path("/hibiscus/konto/([0-9]{1,4})/umsaetze$")
  public Object getUmsaetzeByKonto(String kontoId) throws Exception
  {
    return this.getUmsaetzeByKonto(kontoId,Integer.toString(HBCIProperties.UMSATZ_DEFAULT_DAYS));
  }
  
  /**
   * Liefert die Umsaetze der letzten X Tage fuer das Konto.
   * @param kontoId ID des Kontos.
   * @param days Anzahl der Tage.
   * @return Liste der Umsaetze im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der Umätze der letzten X Tage für das mit der ID angegebene Konto im JSON-Format",
      example="hibiscus/konto/2/umsaetze/days/30")
  @Path("/hibiscus/konto/([0-9]{1,4})/umsaetze/days/([0-9]{1,4})$")
  public Object getUmsaetzeByKonto(String kontoId, String days) throws Exception
  {
    Konto k = (Konto) Settings.getDBService().createObject(Konto.class,kontoId);
    return JsonUtil.toJson(k.getUmsaetze(Integer.parseInt(days)));
  }
  
  /**
   * Liefert die Umsaetze aller Konten, in denen der genannte Suchbegriff auftaucht.
   * @param query Suchbegriff.
   * @return Liste der Umsaetze im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der Umätze aller Konto mit dem genannten Suchbegriff im JSON-Format",
      example="hibiscus/umsaetze/query/Telekom")
  @Path("/hibiscus/umsaetze/query/(.*)$")
  public Object getUmsaetze(String query) throws Exception
  {
    return JsonUtil.toJson(UmsatzUtil.find(query));
  }

}



/**********************************************************************
 * $Log: UmsatzBean.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/09/10 11:57:42  willuhn
 * @N REST-Aufrufe fuer Umsaetze - auch anhand von Suchbegriffen
 *
 * Revision 1.5  2010/06/14 11:22:33  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 * Revision 1.4  2010/05/17 15:45:26  willuhn
 * @N Neue REST-Beans
 *
 * Revision 1.3  2010/05/12 10:59:12  willuhn
 * @N Automatische Dokumentations-Seite fuer die REST-Beans basierend auf der Annotation "Doc"
 *
 * Revision 1.2  2010/05/11 23:21:44  willuhn
 * @N Automatische Dokumentations-Seite fuer die REST-Beans basierend auf der Annotation "Doc"
 *
 * Revision 1.1  2010/05/11 16:41:40  willuhn
 * @N REST-API
 *
 **********************************************************************/