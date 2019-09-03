/**********************************************************************
 *
 * Copyright (c) 2019 Olaf Willuhn
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.payment.handler;

import java.io.Serializable;

/**
 * Ein Konfigurationsparameter fuer einen Handler.
 */
public class Parameter implements Serializable, Cloneable
{
  private String id           = null;
  private String name         = null;
  private String description  = null;
  private String defaultValue = null;
  private String value        = null;

  /**
   * ct.
   * @param id Identifier des Parameters.
   * @param name Sprechender Name.
   * @param desc Zusaetzliche Beschreibung.
   * @param defaultValue Default-Wert.
   */
  public Parameter(String id, String name, String desc, String defaultValue)
  {
    this.id           = id;
    this.name         = name;
    this.description  = desc;
    this.defaultValue = defaultValue;
  }

  /**
   * @return the value
   */
  public String getValue()
  {
    return this.value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(String value)
  {
    this.value = value;
  }

  /**
   * @return the defaultValue
   */
  public String getDefaultValue()
  {
    return this.defaultValue;
  }

  /**
   * @param defaultValue the defaultValue to set
   */
  public void setDefaultValue(String defaultValue)
  {
    this.defaultValue = defaultValue;
  }

  /**
   * @return the description
   */
  public String getDescription()
  {
    return this.description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description)
  {
    this.description = description;
  }

  /**
   * @return the id
   */
  public String getId()
  {
    return this.id;
  }

  /**
   * @param id the id to set
   */
  public void setId(String id)
  {
    this.id = id;
  }

  /**
   * @return the name
   */
  public String getName()
  {
    return this.name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   * @see java.lang.Object#clone()
   */
  @SuppressWarnings("javadoc")
  public Object clone()
  {
    try
    {
      Parameter clone    = (Parameter) super.clone();
      clone.defaultValue = this.defaultValue;
      clone.description  = this.description;
      clone.id           = this.id;
      clone.name         = this.name;
      clone.value        = this.value;
      return clone;
    }
    catch (CloneNotSupportedException e)
    {
      throw new RuntimeException(e);
    }
  }

}


/*********************************************************************
 * $Log: Parameter.java,v $
 * Revision 1.1  2011/11/12 15:09:59  willuhn
 * @N initial import
 *
 * Revision 1.2  2011/10/25 13:57:16  willuhn
 * @R Saemtliche Lizenz-Checks entfernt - ist jetzt Opensource
 *
 * Revision 1.1  2007/09/05 16:14:23  willuhn
 * @N TAN-Support via XML-RPC Callback Handler
 *
 **********************************************************************/