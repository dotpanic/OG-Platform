/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.analytics.blotter;

import java.util.Collections;
import java.util.Map;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.PropertyReadWrite;
import org.joda.convert.StringConvert;

import com.google.common.collect.Maps;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
/* package */ class BeanBuildingVisitor<T extends Bean> implements BeanVisitor<BeanBuilder<T>> {

  private final BeanDataSource _data;
  private final MetaBeanFactory _metaBeanFactory;
  private final Map<MetaProperty<?>, StringConvert> _converters;

  private BeanBuilder<T> _builder;

  @SuppressWarnings("unchecked")
  /* package */ BeanBuildingVisitor(BeanDataSource data, MetaBeanFactory metaBeanFactory) {
    this(data, metaBeanFactory, Collections.<MetaProperty<?>, StringConvert>emptyMap());
  }

  @SuppressWarnings("unchecked")
  /* package */ BeanBuildingVisitor(BeanDataSource data,
                                    MetaBeanFactory metaBeanFactory,
                                    Map<MetaProperty<?>, StringConvert> converters) {
    ArgumentChecker.notNull(data, "data");
    ArgumentChecker.notNull(metaBeanFactory, "metaBeanFactory");
    ArgumentChecker.notNull(converters, "converters");
    _converters = converters;
    _metaBeanFactory = metaBeanFactory;
    _data = data;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void visitBean(MetaBean metaBean) {
    _builder = (BeanBuilder<T>) metaBean.builder();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void visitBeanProperty(MetaProperty<?> property, BeanTraverser traverser) {
    if (isWriteable(property)) {
      String propertyName = property.name();
      BeanDataSource beanData = _data.getBeanData(propertyName);
      Bean result;
      if (beanData != null) {
        BeanBuildingVisitor<?> visitor = new BeanBuildingVisitor<>(beanData, _metaBeanFactory, _converters);
        MetaBean metaBean = _metaBeanFactory.beanFor(beanData);
        result = ((BeanBuilder<?>) traverser.traverse(metaBean, visitor)).build();
      } else {
        result = null;
      }
      _builder.set(property, result);
    }
  }

  @Override
  public void visitSetProperty(MetaProperty<?> property) {
    // TODO implement visitSetProperty()
    throw new UnsupportedOperationException("visitSetProperty not implemented");
  }

  @Override
  public void visitListProperty(MetaProperty<?> property) {
    // TODO implement visitListProperty()
    throw new UnsupportedOperationException("visitListProperty not implemented");
  }

  @Override
  public void visitCollectionProperty(MetaProperty<?> property) {
    // TODO implement visitCollectionProperty()
    throw new UnsupportedOperationException("visitCollectionProperty not implemented");
  }

  @Override
  public void visitMapProperty(MetaProperty<?> property) {
    if (isWriteable(property)) {
      _builder.set(property, buildMap(property, _data.getMapValues(property.name())));
    }
  }

  @Override
  public void visitProperty(MetaProperty<?> property) {
    if (isWriteable(property)) {
      StringConvert converter = _converters.get(property);
      String value = _data.getValue(property.name());
      if (converter != null) {
        _builder.set(property, converter.convertFromString(property.propertyType(), value));
      } else {
        _builder.setString(property, value);
      }
    }
  }

  @Override
  public BeanBuilder<T> finish() {
    return _builder;
  }

  private static Map<?, ?> buildMap(MetaProperty<?> property, Map<String, String> values) {
    if (values == null) {
      return null;
    }
    Class<? extends Bean> beanType = property.metaBean().beanType();
    Class<?> keyType = JodaBeanUtils.mapKeyType(property, beanType);
    Class<?> valueType = JodaBeanUtils.mapValueType(property, beanType);
    Map<Object, Object> map = Maps.newHashMapWithExpectedSize(values.size());
    StringConvert converter = JodaBeanUtils.stringConverter();
    for (Map.Entry<String, String> entry : values.entrySet()) {
      Object key = converter.convertFromString(keyType, entry.getKey());
      Object value = converter.convertFromString(valueType, entry.getValue());
      map.put(key, value);
    }
    return map;
  }

  private static boolean isWriteable(MetaProperty<?> property) {
    return property.readWrite() != PropertyReadWrite.READ_ONLY;
  }
}
