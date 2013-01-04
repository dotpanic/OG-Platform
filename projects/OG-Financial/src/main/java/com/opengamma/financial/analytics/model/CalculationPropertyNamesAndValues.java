/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model;

/**
 * Contains property names and values that are used to label calculations.
 */
public final class CalculationPropertyNamesAndValues {

  /** Property name for the model type */
  public static final String PROPERTY_MODEL_TYPE = "ModelType";

  // Values for ValuePropertyNames.CALCULATION_METHOD
  /** Black calculations */
  public static final String BLACK_METHOD = "BlackMethod";
  /** The Barone-Adesi Whaley approximation for American options */
  public static final String BAW_METHOD = "BaroneAdesiWhaleyMethod";
  /** The Bjerksund-Stensland approximation for American options */
  public static final String BJERKSUND_STENSLAND_METHOD = "BjerksundStenslandMethod";

  //Values for PROPERTY_MODEL_TYPE
  /** Analytic */
  public static final String ANALYTIC = "Analytic";
  /** PDE */
  public static final String PDE = "PDE";

  private CalculationPropertyNamesAndValues() {
  }

}
