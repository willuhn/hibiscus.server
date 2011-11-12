/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/rest/JobsBean.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.rest;

import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.server.hbci.synchronize.SynchronizeEngine;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;

/**
 * REST-Bean zum Zugriff auf die aktuellen Synchronisations-Jobs.
 */
@Doc("Hibiscus: Liefert Informationen über die aktuellen Synchronisierungsaufgaben")
public class JobsBean implements AutoRestBean
{
  /**
   * Liefert eine Liste der anstehenden Synchronisierungsaufgaben.
   * @return Liste der anstehenden Synchronisierungsaufgaben im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der anstehenden Synchronisierungsaufgaben im JSON-Format",
       example="hibiscus/jobs/list")
  @Path("/hibiscus/jobs/list$")
  public Object open() throws Exception
  {
    return JsonUtil.toJson(SynchronizeEngine.getInstance().getSynchronizeJobs());
  }
}



/**********************************************************************
 * $Log: JobsBean.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.2  2010/06/14 11:22:33  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 * Revision 1.1  2010/05/17 15:45:26  willuhn
 * @N Neue REST-Beans
 *
 **********************************************************************/