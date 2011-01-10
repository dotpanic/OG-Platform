/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.master.exchange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.time.calendar.TimeZone;

import org.joda.beans.BeanDefinition;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.direct.DirectBean;
import org.joda.beans.impl.direct.DirectMetaProperty;

import com.opengamma.core.exchange.Exchange;
import com.opengamma.core.exchange.ExchangeUtils;
import com.opengamma.id.Identifier;
import com.opengamma.id.IdentifierBundle;
import com.opengamma.id.UniqueIdentifier;
import com.opengamma.util.ArgumentChecker;

/**
 * An exchange on which financial products can be traded or settled.
 * <p>
 * Financial products are often traded at a specific location known as an exchange.
 * This class represents details of the exchange, including region and opening hours.
 */
@BeanDefinition
public class ManageableExchange extends DirectBean implements Exchange {
  // TODO: regionId should be regionKey, but it is stored in Fudge in DB

  /**
   * The unique identifier of the exchange.
   * This must be null when adding to a master and not null when retrieved from a master.
   */
  @PropertyDefinition
  private UniqueIdentifier _uniqueId;
  /**
   * The bundle of identifiers that define the exchange.
   * This field must not be null for the object to be valid.
   */
  @PropertyDefinition
  private IdentifierBundle _identifiers = IdentifierBundle.EMPTY;
  /**
   * The name of the exchange intended for display purposes.
   * This field must not be null for the object to be valid.
   */
  @PropertyDefinition
  private String _name;
  /**
   * The region key identifier bundle that defines where the exchange is located.
   */
  @PropertyDefinition(get = "manual", set = "manual")
  private IdentifierBundle _regionId;
  /**
   * The time-zone of the exchange.
   */
  @PropertyDefinition
  private TimeZone _timeZone;
  /**
   * The detailed information about when an exchange is open or closed, not null.
   */
  @PropertyDefinition
  private final List<ManageableExchangeDetail> _detail = new ArrayList<ManageableExchangeDetail>();

  /**
   * Creates an exchange.
   */
  public ManageableExchange() {
  }

  /**
   * Creates an exchange specifying the values of the main fields.
   * 
   * @param identifiers  the bundle of identifiers that define the exchange, not null
   * @param name  the name of the exchange, for display purposes, not null
   * @param regionKey  the region key identifier bundle where the exchange is located, null if not applicable (dark pool, electronic, ...)
   * @param timeZone  the time-zone, may be null
   */
  public ManageableExchange(IdentifierBundle identifiers, String name, IdentifierBundle regionKey, TimeZone timeZone) {
    ArgumentChecker.notNull(identifiers, "identifiers");
    ArgumentChecker.notNull(name, "name");
    setIdentifiers(identifiers);
    setName(name);
    setRegionKey(regionKey);
    setTimeZone(timeZone);
  }

  /**
   * Returns an independent clone of this exchange.
   * @return the clone, not null
   */
  public ManageableExchange clone() {
    ManageableExchange cloned = new ManageableExchange();
    cloned._uniqueId = _uniqueId;
    cloned._name = _name;
    cloned._identifiers = _identifiers;
    cloned._regionId = _regionId;
    cloned._detail.addAll(_detail);
    return cloned;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the region key identifier bundle that defines where the exchange is located.
   * @return the value of the property
   */
  public IdentifierBundle getRegionKey() {
    return getRegionId();
  }

  /**
   * Sets the region key identifier bundle that defines where the exchange is located.
   * @param regionKey  the new value of the property
   */
  public void setRegionKey(IdentifierBundle regionKey) {
    setRegionId(regionKey);
  }

  /**
   * Gets the region key identifier bundle that defines where the exchange is located.
   * @return the value of the property
   */
  private IdentifierBundle getRegionId() {
    return _regionId;
  }

  /**
   * Sets the region key identifier bundle that defines where the exchange is located.
   * @param regionKey  the new value of the property
   */
  private void setRegionId(IdentifierBundle regionKey) {
    this._regionId = regionKey;
  }

  //-------------------------------------------------------------------------
  /**
   * Adds an identifier to the bundle representing this exchange.
   * 
   * @param identifier  the identifier to add, not null
   */
  public void addIdentifier(Identifier identifier) {
    setIdentifiers(getIdentifiers().withIdentifier(identifier));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the ISO MIC code.
   * 
   * @return the value of the property
   */
  public String getISOMic() {
    return _identifiers.getIdentifier(ExchangeUtils.ISO_MIC);
  }

  /**
   * Sets the ISO MIC code, stored in the identifier set.
   * 
   * @param isoMicCode  the exchange MIC to set, null to remove any defined ISO MIC
   */
  public void setISOMic(String isoMicCode) {
    setIdentifiers(getIdentifiers().withoutScheme(ExchangeUtils.ISO_MIC));
    if (isoMicCode != null) {
      addIdentifier(ExchangeUtils.isoMicExchangeId(isoMicCode));
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ManageableExchange}.
   * @return the meta-bean, not null
   */
  public static ManageableExchange.Meta meta() {
    return ManageableExchange.Meta.INSTANCE;
  }

  @Override
  public ManageableExchange.Meta metaBean() {
    return ManageableExchange.Meta.INSTANCE;
  }

  @Override
  protected Object propertyGet(String propertyName) {
    switch (propertyName.hashCode()) {
      case -294460212:  // uniqueId
        return getUniqueId();
      case 1368189162:  // identifiers
        return getIdentifiers();
      case 3373707:  // name
        return getName();
      case -690339025:  // regionId
        return getRegionId();
      case -2077180903:  // timeZone
        return getTimeZone();
      case -1335224239:  // detail
        return getDetail();
    }
    return super.propertyGet(propertyName);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void propertySet(String propertyName, Object newValue) {
    switch (propertyName.hashCode()) {
      case -294460212:  // uniqueId
        setUniqueId((UniqueIdentifier) newValue);
        return;
      case 1368189162:  // identifiers
        setIdentifiers((IdentifierBundle) newValue);
        return;
      case 3373707:  // name
        setName((String) newValue);
        return;
      case -690339025:  // regionId
        setRegionId((IdentifierBundle) newValue);
        return;
      case -2077180903:  // timeZone
        setTimeZone((TimeZone) newValue);
        return;
      case -1335224239:  // detail
        setDetail((List<ManageableExchangeDetail>) newValue);
        return;
    }
    super.propertySet(propertyName, newValue);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the unique identifier of the exchange.
   * This must be null when adding to a master and not null when retrieved from a master.
   * @return the value of the property
   */
  public UniqueIdentifier getUniqueId() {
    return _uniqueId;
  }

  /**
   * Sets the unique identifier of the exchange.
   * This must be null when adding to a master and not null when retrieved from a master.
   * @param uniqueId  the new value of the property
   */
  public void setUniqueId(UniqueIdentifier uniqueId) {
    this._uniqueId = uniqueId;
  }

  /**
   * Gets the the {@code uniqueId} property.
   * This must be null when adding to a master and not null when retrieved from a master.
   * @return the property, not null
   */
  public final Property<UniqueIdentifier> uniqueId() {
    return metaBean().uniqueId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the bundle of identifiers that define the exchange.
   * This field must not be null for the object to be valid.
   * @return the value of the property
   */
  public IdentifierBundle getIdentifiers() {
    return _identifiers;
  }

  /**
   * Sets the bundle of identifiers that define the exchange.
   * This field must not be null for the object to be valid.
   * @param identifiers  the new value of the property
   */
  public void setIdentifiers(IdentifierBundle identifiers) {
    this._identifiers = identifiers;
  }

  /**
   * Gets the the {@code identifiers} property.
   * This field must not be null for the object to be valid.
   * @return the property, not null
   */
  public final Property<IdentifierBundle> identifiers() {
    return metaBean().identifiers().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the name of the exchange intended for display purposes.
   * This field must not be null for the object to be valid.
   * @return the value of the property
   */
  public String getName() {
    return _name;
  }

  /**
   * Sets the name of the exchange intended for display purposes.
   * This field must not be null for the object to be valid.
   * @param name  the new value of the property
   */
  public void setName(String name) {
    this._name = name;
  }

  /**
   * Gets the the {@code name} property.
   * This field must not be null for the object to be valid.
   * @return the property, not null
   */
  public final Property<String> name() {
    return metaBean().name().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the the {@code regionId} property.
   * @return the property, not null
   */
  public final Property<IdentifierBundle> regionId() {
    return metaBean().regionId().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-zone of the exchange.
   * @return the value of the property
   */
  public TimeZone getTimeZone() {
    return _timeZone;
  }

  /**
   * Sets the time-zone of the exchange.
   * @param timeZone  the new value of the property
   */
  public void setTimeZone(TimeZone timeZone) {
    this._timeZone = timeZone;
  }

  /**
   * Gets the the {@code timeZone} property.
   * @return the property, not null
   */
  public final Property<TimeZone> timeZone() {
    return metaBean().timeZone().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the detailed information about when an exchange is open or closed, not null.
   * @return the value of the property
   */
  public List<ManageableExchangeDetail> getDetail() {
    return _detail;
  }

  /**
   * Sets the detailed information about when an exchange is open or closed, not null.
   * @param detail  the new value of the property
   */
  public void setDetail(List<ManageableExchangeDetail> detail) {
    this._detail.clear();
    this._detail.addAll(detail);
  }

  /**
   * Gets the the {@code detail} property.
   * @return the property, not null
   */
  public final Property<List<ManageableExchangeDetail>> detail() {
    return metaBean().detail().createProperty(this);
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ManageableExchange}.
   */
  public static class Meta extends BasicMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code uniqueId} property.
     */
    private final MetaProperty<UniqueIdentifier> _uniqueId = DirectMetaProperty.ofReadWrite(this, "uniqueId", UniqueIdentifier.class);
    /**
     * The meta-property for the {@code identifiers} property.
     */
    private final MetaProperty<IdentifierBundle> _identifiers = DirectMetaProperty.ofReadWrite(this, "identifiers", IdentifierBundle.class);
    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> _name = DirectMetaProperty.ofReadWrite(this, "name", String.class);
    /**
     * The meta-property for the {@code regionId} property.
     */
    private final MetaProperty<IdentifierBundle> _regionId = DirectMetaProperty.ofReadWrite(this, "regionId", IdentifierBundle.class);
    /**
     * The meta-property for the {@code timeZone} property.
     */
    private final MetaProperty<TimeZone> _timeZone = DirectMetaProperty.ofReadWrite(this, "timeZone", TimeZone.class);
    /**
     * The meta-property for the {@code detail} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<ManageableExchangeDetail>> _detail = DirectMetaProperty.ofReadWrite(this, "detail", (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<Object>> _map;

    @SuppressWarnings({"unchecked", "rawtypes" })
    protected Meta() {
      LinkedHashMap temp = new LinkedHashMap();
      temp.put("uniqueId", _uniqueId);
      temp.put("identifiers", _identifiers);
      temp.put("name", _name);
      temp.put("regionId", _regionId);
      temp.put("timeZone", _timeZone);
      temp.put("detail", _detail);
      _map = Collections.unmodifiableMap(temp);
    }

    @Override
    public ManageableExchange createBean() {
      return new ManageableExchange();
    }

    @Override
    public Class<? extends ManageableExchange> beanType() {
      return ManageableExchange.class;
    }

    @Override
    public Map<String, MetaProperty<Object>> metaPropertyMap() {
      return _map;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code uniqueId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<UniqueIdentifier> uniqueId() {
      return _uniqueId;
    }

    /**
     * The meta-property for the {@code identifiers} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<IdentifierBundle> identifiers() {
      return _identifiers;
    }

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<String> name() {
      return _name;
    }

    /**
     * The meta-property for the {@code regionId} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<IdentifierBundle> regionId() {
      return _regionId;
    }

    /**
     * The meta-property for the {@code timeZone} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<TimeZone> timeZone() {
      return _timeZone;
    }

    /**
     * The meta-property for the {@code detail} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<List<ManageableExchangeDetail>> detail() {
      return _detail;
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
