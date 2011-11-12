/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/rest/KontoBean.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.rest;

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;

/**
 * REST-Bean zum Zugriff auf die Konten.
 */
@Doc("Hibiscus: Liefert Informationen über die Konten")
public class KontoBean implements AutoRestBean
{
  /**
   * Liefert eine Liste der Konten.
   * @return Liste der Konten im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der Konten im JSON-Format",
       example="hibiscus/konto/list")
  @Path("/hibiscus/konto/list$")
  public Object list() throws Exception
  {
    DBIterator i = Settings.getDBService().createList(Konto.class);
    i.setOrder("ORDER BY blz, bezeichnung");
    return JsonUtil.toJson(i);
  }
}



/**********************************************************************
 * $Log: KontoBean.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.6  2010/09/10 11:57:42  willuhn
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