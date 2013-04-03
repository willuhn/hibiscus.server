/**********************************************************************
 * $Source: /cvsroot/hibiscus/hibiscus.server/src/de/willuhn/jameica/hbci/payment/util/JsonUtil.java,v $
 * $Revision: 1.1 $
 * $Date: 2011/11/12 15:09:59 $
 * $Author: willuhn $
 *
 * Copyright (c) by willuhn - software & services
 * All rights reserved
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.util;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.willuhn.datasource.BeanUtil;
import de.willuhn.datasource.GenericIterator;
import de.willuhn.datasource.GenericObject;

/**
 * JSON Utils.
 */
public class JsonUtil
{
  /**
   * Wandelt eine Liste von Fachobjekten in JSON um.
   * @param list Liste der Fach-Objekte.
   * @return JSON-Liste.
   * @throws Exception
   */
  public static JSONArray toJson(GenericIterator list) throws Exception
  {
    JSONArray result = new JSONArray();
    if (list != null)
    {
      while (list.hasNext())
      {
        result.put(toJson(list.next()));
      }
    }
    return result;
  }

  /**
   * Wandelt eine Liste von Beans in JSON um.
   * @param list Liste der Beans.
   * @return JSON-Liste.
   * @throws Exception
   */
  public static JSONArray toJson(List list) throws Exception
  {
    JSONArray result = new JSONArray();
    if (list != null)
    {
      for (Object o:list)
      {
        result.put(toJson(o));
      }
    }
    return result;
  }

  /**
   * Wandelt eine Bean in JSON um.
   * @param object die Bean.
   * @return JSON-Objekt.
   * @throws Exception
   */
  public static JSONObject toJson(Object object) throws Exception
  {
    JSONObject o = new JSONObject();
    List<String> names = BeanUtil.getProperties(object);
    for (String name:names)
    {
      Object value = BeanUtil.get(object,name);
      String s = null;
      if (value != null && (value instanceof GenericObject))
        s = ((GenericObject)value).getID();
      else
        s = BeanUtil.toString(value);
      o.put(name,s);
    }
    
    if (object instanceof GenericObject)
      o.put("id",((GenericObject)object).getID());
    return o;
  }

}



/**********************************************************************
 * $Log: JsonUtil.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.1  2010/06/14 11:22:34  willuhn
 * @N Benachrichtigungs-URL, mit der ein Fremd-System darueber informiert werden kann, wenn die Synchronisierung eines Kontos lief
 *
 **********************************************************************/