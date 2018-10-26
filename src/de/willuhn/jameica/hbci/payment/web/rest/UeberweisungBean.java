/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/rest/UeberweisungBean.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.rest;

import javax.servlet.http.HttpServletRequest;

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.datasource.rmi.ObjectNotFoundException;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung;
import de.willuhn.jameica.hbci.rmi.HBCIDBService;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Doc;
import de.willuhn.jameica.webadmin.annotation.Path;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.jameica.webadmin.rest.AutoRestBean;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * REST-Bean zum Zugriff auf die Ueberweisungen.
 */
@Doc("Hibiscus: Bietet Zugriff auf Überweisungen")
public class UeberweisungBean implements AutoRestBean
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();
  
  @Request
  private HttpServletRequest request = null;

  /**
   * Liefert eine Liste der offenen Ueberweisungen.
   * @return Liste der offenen Ueberweisungen im JSON-Format, sortiert nach Termin, neueste zuerst.
   * @throws Exception
   */
  @Doc(value="Liefert eine Liste der offnen Überweisungen im JSON-Format",
       example="hibiscus/ueberweisung/list/open")
  @Path("/hibiscus/ueberweisung/list/open$")
  public Object open() throws Exception
  {
    HBCIDBService service = Settings.getDBService();
    DBIterator i = service.createList(AuslandsUeberweisung.class);
    i.addFilter("(ausgefuehrt is null or ausgefuehrt = 0)");
    i.setOrder("ORDER BY " + service.getSQLTimestamp("termin") + " DESC, id DESC");
    return JsonUtil.toJson(i);
  }
  
  /**
   * Loescht die Ueberweisung mit der genannten ID.
   * @param id die ID der Ueberweisung.
   * @return Die Eigenschaften der geloeschten Ueberweisung.
   * @throws Exception
   */
  @Doc(value="Loescht die Ueberweisung mit der angegebenen ID",
       example="hibiscus/ueberweisung/delete/1234")
  @Path("/hibiscus/ueberweisung/delete/([0-9]{1,8})$")
  public Object delete(String id) throws Exception
  {
    AuslandsUeberweisung u = (AuslandsUeberweisung) Settings.getDBService().createObject(AuslandsUeberweisung.class,id);
    u.delete();
    return JsonUtil.toJson(u);
  }

  /**
   * Erstellt eine neue Ueberweisung.
   * @return Die Eigenschaften der erstellten Ueberweisung.
   * @throws Exception
   */
  @Doc(value="Erstellt eine neue Ueberweisung. " +
             "Die Funktion erwartet folgende 4 Parameter via GET oder POST.<br/>" +
             "<ul>" +
             "  <li><b>betrag</b>: Betrag im Format 000,00 (Komma als Dezimaltrennzeichen)</li>" +
             "  <li><b>bic</b>: BIC des Gegenkontos</li>" +
             "  <li><b>name</b>: Inhaber-Name des Gegenkontos</li>" +
             "  <li><b>iban</b>: IBAN des Gegenkontos</li>" +
             "  <li><b>konto_id</b>: ID des eigenen Kontos</li>" +
             "  <li><b>zweck</b>: Verwendungszweck</li>" +
             "  <li><b>termin</b>: optional: &quot;true&quot; wenn die Überweisung als Termin-Überweisung ausgeführt werden soll</li>" +
             "  <li><b>umbuchung</b>: optional: &quot;true&quot; wenn der Auftrag als Bank-interne Umbuchung ausgeführt werden soll</li>" +
             "  <li><b>datum</b>: optional: Ausführungstermin im Format TT.MM.JJJJ</li>" +
             "</ul>",
       example="hibiscus/ueberweisung/create")
  @Path("/hibiscus/ueberweisung/create$")
  public Object create() throws Exception
  {
    AuslandsUeberweisung u = (AuslandsUeberweisung) Settings.getDBService().createObject(AuslandsUeberweisung.class,null);
    
    String betrag = request.getParameter("betrag");
    String konto  = request.getParameter("konto_id");
    if (betrag == null || betrag.length() == 0)
      throw new ApplicationException(i18n.tr("Kein Betrag angegeben"));
    if (konto == null || konto.length() == 0)
      throw new ApplicationException(i18n.tr("Kein Konto angegeben"));
    
    // Pflichtparameter
    try
    {
      u.setBetrag(HBCI.DECIMALFORMAT.parse(betrag).doubleValue());
    }
    catch (Exception e)
    {
      throw new ApplicationException(i18n.tr("Betrag ungültig: {0}",betrag));
    }
    
    u.setGegenkontoBLZ(request.getParameter("bic"));
    u.setGegenkontoName(request.getParameter("name"));
    u.setGegenkontoNummer(request.getParameter("iban"));
    
    try
    {
      u.setKonto((Konto) Settings.getDBService().createObject(Konto.class,konto));
    }
    catch (ObjectNotFoundException e)
    {
      throw new ApplicationException(i18n.tr("Konto [ID: {0}] nicht gefunden",konto));
    }

    // Verwendungszweck
    u.setZweck(request.getParameter("zweck"));
    
    // Optionale Parameter
    u.setTerminUeberweisung("true".equalsIgnoreCase(request.getParameter("termin")));
    u.setUmbuchung("true".equalsIgnoreCase(request.getParameter("umbuchung")));
    
    String datum = request.getParameter("datum");
    if (datum != null && datum.length() > 0)
    {
      try
      {
        u.setTermin(HBCI.DATEFORMAT.parse(datum));
      }
      catch (Exception e)
      {
        throw new ApplicationException(i18n.tr("Datum ungültig: {0}",datum));
      }
    }
    
    u.store();
    return JsonUtil.toJson(u);
  }

}
