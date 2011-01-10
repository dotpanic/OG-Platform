/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.math.minimization;

import org.junit.Test;

/**
 * 
 */
public class BrentMinimizer1DTest extends Minimizer1DTestCase {
  private static final ScalarMinimizer MINIMIZER = new BrentMinimizer1D();

  @Test
  public void test() {
    super.testInputs(MINIMIZER);
    super.test(MINIMIZER);
  }
}
