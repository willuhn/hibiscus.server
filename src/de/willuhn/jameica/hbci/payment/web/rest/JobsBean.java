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
import de.willuhn.jameica.hbci.payment.web.beans.Jobs;
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
  public Object list() throws Exception
  {
    return JsonUtil.toJson(new Jobs().getJobs());
  }
}
