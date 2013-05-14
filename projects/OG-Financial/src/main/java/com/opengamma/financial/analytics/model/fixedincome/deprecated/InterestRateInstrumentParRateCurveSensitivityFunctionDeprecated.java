/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.fixedincome.deprecated;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.interestrate.InstrumentDerivative;
import com.opengamma.analytics.financial.interestrate.ParRateCurveSensitivityCalculator;
import com.opengamma.analytics.financial.interestrate.YieldCurveBundle;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.analytics.ircurve.InterpolatedYieldCurveSpecificationWithSecurities;
import com.opengamma.financial.analytics.model.YieldCurveNodeSensitivitiesHelper;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentParRateCurveSensitivityFunction;
import com.opengamma.lambdava.tuple.DoublesPair;

/**
 * @deprecated Use the version that does not refer to funding or forward curves
 * @see InterestRateInstrumentParRateCurveSensitivityFunction
 */
@Deprecated
public class InterestRateInstrumentParRateCurveSensitivityFunctionDeprecated extends InterestRateInstrumentCurveSpecificFunctionDeprecated {
  private static final ParRateCurveSensitivityCalculator CALCULATOR = ParRateCurveSensitivityCalculator.getInstance();

  public InterestRateInstrumentParRateCurveSensitivityFunctionDeprecated() {
    super(ValueRequirementNames.PAR_RATE_CURVE_SENSITIVITY);
  }

  @Override
  public Set<ComputedValue> getResults(final InstrumentDerivative derivative, final String curveName, final InterpolatedYieldCurveSpecificationWithSecurities curveSpec,
      final YieldCurveBundle curves, final ValueSpecification resultSpec) {
    final Map<String, List<DoublesPair>> sensitivities = derivative.accept(CALCULATOR, curves);
    if (!sensitivities.containsKey(curveName)) {
      throw new OpenGammaRuntimeException("Could not get par rate curve sensitivities for curve named " + curveName + "; should never happen");
    }
    final List<DoublesPair> resultList = sensitivities.get(curveName);
    return YieldCurveNodeSensitivitiesHelper.getTimeLabelledSensitivitiesForCurve(resultList, resultSpec);
  }

}
