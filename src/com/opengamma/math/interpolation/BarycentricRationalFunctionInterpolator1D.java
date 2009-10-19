/**
 * Copyright (C) 2009 - 2009 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.math.interpolation;

import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * @author emcleod
 */
public class BarycentricRationalFunctionInterpolator1D extends Interpolator1D {
  private final int _degree;

  public BarycentricRationalFunctionInterpolator1D(final int degree) {
    if (degree < 1)
      throw new IllegalArgumentException("Cannot perform interpolation with rational functions of degree < 1");
    _degree = degree;
  }

  @Override
  public InterpolationResult<Double> interpolate(final Map<Double, Double> data, final Double value) {
    final TreeMap<Double, Double> sorted = initData(data);
    if (data.size() < _degree)
      throw new InterpolationException("Cannot interpolate " + sorted.size() + " data points with rational functions of degree " + _degree);
    final Double[] x = sorted.keySet().toArray(new Double[0]);
    final Double[] y = sorted.values().toArray(new Double[0]);
    final double[] w = getWeights(x);
    final int n = x.length;
    double delta, temp, num = 0, den = 0;
    for (int i = 0; i < n; i++) {
      delta = value - x[i];
      if (Math.abs(delta) < EPS)
        return new InterpolationResult<Double>(y[i]);
      temp = w[i] / delta;
      num += temp * y[i];
      den += temp;
    }
    return new InterpolationResult<Double>(num / den);
  }

  private double[] getWeights(final Double[] x) {
    final int n = x.length;
    final double[] w = new double[n];
    int iMin, iMax, mult, jMax;
    double sum, term;
    for (int k = 0; k < n; k++) {
      iMin = Math.max(k - _degree, 0);
      iMax = k >= n - _degree ? n - _degree - 1 : k;
      mult = iMin % 2 == 0 ? 1 : -1;
      sum = 0;
      for (int i = iMin; i <= iMax; i++) {
        jMax = Math.min(i + _degree, n - 1);
        term = 1;
        for (int j = i; j <= jMax; j++) {
          if (j == k) {
            continue;
          }
          term *= x[k] - x[j];
        }
        term = mult / term;
        mult *= -1;
        sum += term;
      }
      w[k] = sum;
    }
    return w;
  }
}
