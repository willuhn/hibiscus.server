/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/web/beans/Account.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.web.beans;

import java.rmi.RemoteException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.willuhn.datasource.pseudo.PseudoIterator;
import de.willuhn.datasource.rmi.DBIterator;
import de.willuhn.jameica.hbci.HBCIProperties;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.SynchronizeOptions;
import de.willuhn.jameica.hbci.payment.Plugin;
import de.willuhn.jameica.hbci.rmi.Konto;
import de.willuhn.jameica.hbci.rmi.Umsatz;
import de.willuhn.jameica.messaging.StatusBarMessage;
import de.willuhn.jameica.system.Application;
import de.willuhn.jameica.webadmin.annotation.Request;
import de.willuhn.jameica.webadmin.annotation.Response;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

/**
 * Controller fuer ein einzelnes Konto.
 */
public class Account
{
  private final static I18N i18n = Application.getPluginLoader().getPlugin(Plugin.class).getResources().getI18N();

  @Request
  private HttpServletRequest request = null;
  
  @Response
  private HttpServletResponse response = null;
  
  private Konto konto = null;
  
  /**
   * Liefert das aktuell geladene Konto.
   * @return das aktuell geladene Konto.
   */
  public Konto getAccount()
  {
    return this.konto;
  }
  
  /**
   * Liefert eine Liste der letzten Umsaetze.
   * @return Liste der letzten Umsaetze.
   * @throws RemoteException
   */
  public List<Umsatz> getUmsaetze() throws RemoteException
  {
    DBIterator it = this.konto.getUmsaetze(HBCIProperties.UMSATZ_DEFAULT_DAYS);
    it.setLimit(500);
    return PseudoIterator.asList(it);
  }
  
  /**
   * Liefert eine Liste der letzten Protokoll-Eintraege.
   * @return Liste der letzten Protokoll-Eintraege.
   * @throws RemoteException
   */
  public List<Protokoll> getProtokoll() throws RemoteException
  {
    DBIterator it = this.konto.getProtokolle();
    it.setLimit(200);
    return PseudoIterator.asList(it);
  }

  /**
   * Action zum Laden des Kontos.
   * @throws Exception
   */
  public void load() throws Exception
  {
    String id = this.request.getParameter("id");
    if (id == null || id.length() == 0)
      throw new ApplicationException(i18n.tr("Kein Konto angegeben"));

    this.konto = (Konto) Settings.getDBService().createObject(Konto.class,id);
  }
  
  /**
   * Action zum Loeschen eines Kontos und aller zugeordneten Auftraege.
   * @throws Exception
   */
  public void delete() throws Exception
  {
    try
    {
      this.konto.delete();
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Konto und alle zugeordneten Umsätze/Aufträge gelöscht"),StatusBarMessage.TYPE_SUCCESS));
      response.sendRedirect("accounts.html");
    }
    catch (ApplicationException ae)
    {
      Application.getMessagingFactory().sendMessage(new StatusBarMessage(ae.getMessage(),StatusBarMessage.TYPE_ERROR));
    }
  }
  
  /**
   * Aktion zum Speichern der Einstellungen.
   * @throws RemoteException
   */
  public void store() throws RemoteException
  {
    SynchronizeOptions options = this.getOptions();
    options.setSyncSaldo(request.getParameter("saldo") != null);
    options.setSyncKontoauszuege(request.getParameter("umsatz") != null);
    options.setSyncUeberweisungen(request.getParameter("ueb") != null);
    options.setSyncLastschriften(request.getParameter("last") != null);
    options.setSyncDauerauftraege(request.getParameter("dauer") != null);
    options.setSyncAuslandsUeberweisungen(request.getParameter("foreign") != null);
    Application.getMessagingFactory().sendMessage(new StatusBarMessage(i18n.tr("Synchronisierungsoptionen gespeichert"),StatusBarMessage.TYPE_SUCCESS));
  }
  
  /**
   * Liefert die Synchronisationseinstellungen fuer das Konto.
   * @return Synchronisationseinstellungen fuer das Konto.
   * @throws RemoteException
   */
  public SynchronizeOptions getOptions() throws RemoteException
  {
    return new SynchronizeOptions(this.konto);
  }

}



/**********************************************************************
 * $Log: Account.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.3  2011/01/18 12:20:07  willuhn
 * @B Limit-Angaben in Umsatz- und Protokoll-Liste falsch
 *
 * Revision 1.2  2010/02/26 16:19:43  willuhn
 * @N Konten loeschen
 *
 * Revision 1.1  2010/02/18 17:13:09  willuhn
 * @N Komplettes Rewrite des Webfrontends auf jameica.webtools-Plattform - endlich keine haesslichen JSPs mehr
 *
 **********************************************************************/