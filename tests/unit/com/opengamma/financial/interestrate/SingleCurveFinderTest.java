/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.interestrate;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.opengamma.financial.interestrate.cash.definition.Cash;
import com.opengamma.math.interpolation.InterpolationResult;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.interpolation.Interpolator1DDataBundle;
import com.opengamma.math.interpolation.LinearInterpolator1D;
import com.opengamma.math.matrix.DoubleMatrix1D;

/**
 * 
 */
public class SingleCurveFinderTest {
  private static final List<InterestRateDerivative> DERIVATIVES;
  private static final double[] MARKET_RATES;
  private static final double SPOT_RATE = 0.05;
  private static final double[] NODES;
  private static final Interpolator1D<Interpolator1DDataBundle, InterpolationResult> INTERPOLATOR = new LinearInterpolator1D();
  private static final SingleCurveFinder FINDER;
  static {
    final int n = 10;
    DERIVATIVES = new ArrayList<InterestRateDerivative>();
    MARKET_RATES = new double[n];
    NODES = new double[n];
    for (int i = 0; i < n; i++) {
      DERIVATIVES.add(new Cash(i));
      MARKET_RATES[i] = Math.random() / .9 * SPOT_RATE;
      NODES[i] = i;
    }
    FINDER = new SingleCurveFinder(DERIVATIVES, MARKET_RATES, SPOT_RATE, NODES, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullDerivatives() {
    new SingleCurveFinder(null, MARKET_RATES, SPOT_RATE, NODES, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMarketRates() {
    new SingleCurveFinder(DERIVATIVES, null, SPOT_RATE, NODES, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullTimes() {
    new SingleCurveFinder(DERIVATIVES, MARKET_RATES, SPOT_RATE, null, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullInterpolator() {
    new SingleCurveFinder(DERIVATIVES, MARKET_RATES, SPOT_RATE, NODES, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyDerivatives() {
    new SingleCurveFinder(new ArrayList<InterestRateDerivative>(), MARKET_RATES, SPOT_RATE, NODES, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyRates() {
    new SingleCurveFinder(DERIVATIVES, new double[0], SPOT_RATE, NODES, INTERPOLATOR);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyTimes() {
    new SingleCurveFinder(DERIVATIVES, MARKET_RATES, SPOT_RATE, new double[0], INTERPOLATOR);
  }

  @Test
  public void test() {
    final DoubleMatrix1D results = FINDER.evaluate(new DoubleMatrix1D(MARKET_RATES));
    for (final double r : results.getData()) {
      assertEquals(r, 0, 0);
    }
  }
}
