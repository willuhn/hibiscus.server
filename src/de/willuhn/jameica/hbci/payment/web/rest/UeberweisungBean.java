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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.datasource.rmi.ObjectNotFoundException;
import de.willuhn.jameica.hbci.HBCI;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.payment.util.JsonUtil;
import de.willuhn.jameica.hbci.rmi.HBCIDBService;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Ueberweisung;
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
    HBCIDBService service = (HBCIDBService) Settings.getDBService();
    DBIterator i = service.createList(Ueberweisung.class);
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
    Ueberweisung u = (Ueberweisung) Settings.getDBService().createObject(Ueberweisung.class,id);
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
             "  <li><b>blz</b>: Bankleitzahl des Gegenkontos</li>" +
             "  <li><b>name</b>: Inhaber-Name des Gegenkontos</li>" +
             "  <li><b>konto</b>: Kontonummer des Gegenkontos</li>" +
             "  <li><b>konto_id</b>: ID des eigenen Kontos</li>" +
             "  <li><b>zweck</b>: Verwendungszweck Zeile 1</li>" +
             "  <li><b>zweck2</b>: optional: Verwendungszweck Zeile 2</li>" +
             "  <li><b>zweck3</b>: optional: Verwendungszweck Zeile 3</li>" +
             "  <li><b>zweck{nr}</b>: optional: Weitere Verwendungszweck-Zeilen</li>" +
             "  <li><b>termin</b>: optional: &quot;true&quot; wenn die Überweisung als Termin-Überweisung ausgeführt werden soll</li>" +
             "  <li><b>textschluessel</b>: optional: Textschlüssel (Nummer)</li>" +
             "  <li><b>umbuchung</b>: optional: &quot;true&quot; wenn der Auftrag als Bank-interne Umbuchung ausgeführt werden soll</li>" +
             "  <li><b>datum</b>: optional: Ausführungstermin im Format TT.MM.JJJJ</li>" +
             "</ul>",
       example="hibiscus/ueberweisung/create")
  @Path("/hibiscus/ueberweisung/create$")
  public Object create() throws Exception
  {
    Ueberweisung u = (Ueberweisung) Settings.getDBService().createObject(Ueberweisung.class,null);
    
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
    
    u.setGegenkontoBLZ(request.getParameter("blz"));
    u.setGegenkontoName(request.getParameter("name"));
    u.setGegenkontoNummer(request.getParameter("konto"));
    
    try
    {
      u.setKonto((Konto) Settings.getDBService().createObject(Konto.class,konto));
    }
    catch (ObjectNotFoundException e)
    {
      throw new ApplicationException(i18n.tr("Konto [ID: {0}] nicht gefunden",konto));
    }

    // Verwendungszwecke
    List<String> list = new ArrayList<String>();
    for (int i=-1;i<16;++i)
    {
      String name = "zweck" + (i < 0 ? "" : Integer.toString(i));
      String value = request.getParameter(name);
      if (value != null && value.length() > 0)
        list.add(value);
    }
    if (list.size() > 0) u.setZweck(list.remove(0));
    if (list.size() > 0) u.setZweck2(list.remove(0));
    if (list.size() > 0) u.setWeitereVerwendungszwecke(list.toArray(new String[list.size()]));
    
    // Optionale Parameter
    u.setTerminUeberweisung("true".equalsIgnoreCase(request.getParameter("termin")));
    u.setTextSchluessel(request.getParameter("textschluessel"));
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



/**********************************************************************
 * $Log: UeberweisungBean.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.4  2010/06/14 11:22:33  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 * Revision 1.3  2010/05/18 10:43:30  willuhn
 * @N Debugging
 *
 * Revision 1.2  2010/05/17 16:31:45  willuhn
 * @N Neue REST-Beans
 *
 * Revision 1.1  2010/05/17 15:45:26  willuhn
 * @N Neue REST-Beans
 *
 **********************************************************************/