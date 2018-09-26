/**********************************************************************
 *
 * Copyright (c) 2004 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.rest;

import javax.servlet.http.HttpServletRequest;

import org.kapott.hbci.manager.HBCIUtils;

import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;

/**
 * REST-Bean zum Zugriff auf Banken und Zugaenge.
 */
@Doc("Hibiscus: Liefert Informationen über Banken")
public class BankBean implements AutoRestBean
{
  @Request private HttpServletRequest request = null;

  /**
   * Sucht nach einer Bank.
   * @return Liste der Banken im JSON-Format.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der Banken im JSON-Format",
       example="hibiscus/bank/search?q=Sparkasse")
  @Path("/hibiscus/bank/search")
  public Object search() throws Exception
  {
    return JsonUtil.toJson(HBCIUtils.searchBankInfo(request.getParameter("q")));
  }
}

