/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.ircurve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.time.calendar.Clock;
import javax.time.calendar.LocalDate;
import javax.time.calendar.ZonedDateTime;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

import com.google.common.collect.Sets;
import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetSpecification;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.config.ConfigSource;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionExecutionContext;
import com.opengamma.engine.function.FunctionInputs;
import com.opengamma.engine.function.FunctionInvoker;
import com.opengamma.engine.value.ComputedValue;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.financial.Currency;
import com.opengamma.financial.OpenGammaCompilationContext;
import com.opengamma.financial.OpenGammaExecutionContext;
import com.opengamma.financial.analytics.cash.CashSecurityToCashConverter;
import com.opengamma.financial.analytics.fra.FRASecurityToForwardRateAgreementConverter;
import com.opengamma.financial.analytics.interestratefuture.InterestRateFutureSecurityToInterestRateFutureConverter;
import com.opengamma.financial.analytics.swap.FixedFloatSwapSecurityToSwapConverter;
import com.opengamma.financial.analytics.swap.TenorSwapSecurityToTenorSwapConverter;
import com.opengamma.financial.convention.ConventionBundle;
import com.opengamma.financial.convention.ConventionBundleSource;
import com.opengamma.financial.convention.InMemoryConventionBundleMaster;
import com.opengamma.financial.interestrate.InterestRateDerivative;
import com.opengamma.financial.interestrate.LastDateCalculator;
import com.opengamma.financial.interestrate.MultipleYieldCurveFinderDataBundle;
import com.opengamma.financial.interestrate.MultipleYieldCurveFinderFunction;
import com.opengamma.financial.interestrate.MultipleYieldCurveFinderJacobian;
import com.opengamma.financial.interestrate.PresentValueCalculator;
import com.opengamma.financial.interestrate.PresentValueSensitivityCalculator;
import com.opengamma.financial.model.interestrate.curve.InterpolatedYieldCurve;
import com.opengamma.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.financial.security.cash.CashSecurity;
import com.opengamma.financial.security.fra.FRASecurity;
import com.opengamma.financial.security.future.InterestRateFutureSecurity;
import com.opengamma.financial.security.swap.SwapSecurity;
import com.opengamma.financial.world.holiday.HolidaySource;
import com.opengamma.financial.world.region.RegionSource;
import com.opengamma.id.IdentificationScheme;
import com.opengamma.id.Identifier;
import com.opengamma.id.IdentifierBundle;
import com.opengamma.livedata.normalization.MarketDataRequirementNames;
import com.opengamma.math.ParallelArrayBinarySort;
import com.opengamma.math.function.Function1D;
import com.opengamma.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.math.interpolation.Interpolator1D;
import com.opengamma.math.interpolation.Interpolator1DFactory;
import com.opengamma.math.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.math.interpolation.sensitivity.CombinedInterpolatorExtrapolatorNodeSensitivityCalculatorFactory;
import com.opengamma.math.interpolation.sensitivity.Interpolator1DNodeSensitivityCalculator;
import com.opengamma.math.linearalgebra.DecompositionFactory;
import com.opengamma.math.matrix.DoubleMatrix1D;
import com.opengamma.math.matrix.DoubleMatrix2D;
import com.opengamma.math.rootfinding.RootNotFoundException;
import com.opengamma.math.rootfinding.newton.BroydenVectorRootFinder;
import com.opengamma.math.rootfinding.newton.NewtonVectorRootFinder;
import com.sun.org.apache.xalan.internal.xsltc.runtime.BasisLibrary;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * 
 */
public class MarketInstrumentImpliedYieldCurveFunction extends AbstractFunction implements FunctionInvoker {
  private final LocalDate _curveDate;
  private final Currency _currency;
  private final String _fundingCurveDefinitionName;
  private final String _fundingCurveValueRequirementName;
  private final String _forwardCurveDefinitionName;
  private final String _forwardCurveValueRequirementName;
  private YieldCurveDefinition _fundingCurveDefinition;
  private Set<ValueRequirement> _fundingCurveRequirements;
  private YieldCurveDefinition _forwardCurveDefinition;
  private Set<ValueRequirement> _forwardCurveRequirements;
  private InterpolatedYieldCurveSpecification _fundingCurveSpecification;
  private InterpolatedYieldCurveSpecification _forwardCurveSpecification;
  private ValueSpecification _fundingCurveResult;
  private ValueSpecification _forwardCurveResult;
  private ValueSpecification _jacobianResult;
  private Set<ValueSpecification> _results;
  private final Map<Identifier, Double> _identifierToFundingNodeTimes = new HashMap<Identifier, Double>();
  private final Map<Identifier, Double> _identifierToForwardNodeTimes = new HashMap<Identifier, Double>();
  private static final LastDateCalculator LAST_DATE_CALCULATOR = new LastDateCalculator();

  public MarketInstrumentImpliedYieldCurveFunction(final String curveDate, final String currency, final String curveDefinitionName, 
      final String curveValueRequirementName) {
    this(LocalDate.parse(curveDate), Currency.getInstance(currency), curveDefinitionName, curveValueRequirementName, curveDefinitionName, curveValueRequirementName);
  }

  public MarketInstrumentImpliedYieldCurveFunction(final String curveDate, final String currency, final String fundingCurveDefinitionName, 
      final String fundingCurveValueRequirementName, String forwardCurveDefinitionName, String forwardCurveValueRequirementName) {
    this(LocalDate.parse(curveDate), Currency.getInstance(currency), fundingCurveDefinitionName, fundingCurveValueRequirementName, forwardCurveDefinitionName, forwardCurveValueRequirementName);
  }
  
  public MarketInstrumentImpliedYieldCurveFunction(final LocalDate curveDate, final Currency currency, final String curveDefinitionName, final String curveValueRequirementName) {
    this(curveDate, currency, curveDefinitionName, curveValueRequirementName, curveDefinitionName, curveValueRequirementName);
  }
  
  public MarketInstrumentImpliedYieldCurveFunction(final LocalDate curveDate, final Currency currency, final String fundingCurveDefinitionName, final String fundingValueRequirementName, 
      String forwardCurveDefinitionName, String forwardValueRequirementName) {
    Validate.notNull(curveDate, "curve date");
    Validate.notNull(currency, "curve currency");
    Validate.notNull(fundingCurveDefinitionName, "funding curve name");
    Validate.notNull(fundingValueRequirementName, "funding value requirement name");
    Validate.notNull(forwardCurveDefinitionName, "forward curve name");
    Validate.notNull(forwardValueRequirementName, "forward value requirement name");
    _curveDate = curveDate;
    _currency = currency;
    _fundingCurveDefinitionName = fundingCurveDefinitionName;
    _fundingCurveValueRequirementName = fundingValueRequirementName;
    _forwardCurveDefinitionName = forwardCurveDefinitionName;
    _forwardCurveValueRequirementName = forwardValueRequirementName;
  }

  @Override
  public void init(final FunctionCompilationContext context) {
    final ConfigSource configSource = OpenGammaCompilationContext.getConfigSource(context);
    final ConfigDBInterpolatedYieldCurveDefinitionSource curveDefinitionSource = new ConfigDBInterpolatedYieldCurveDefinitionSource(configSource);
    _fundingCurveDefinition = curveDefinitionSource.getDefinition(_currency, _fundingCurveDefinitionName);
    _forwardCurveDefinition = curveDefinitionSource.getDefinition(_currency, _forwardCurveDefinitionName);
    final ConfigDBInterpolatedYieldCurveSpecificationBuilder curveSpecificationBuilder = new ConfigDBInterpolatedYieldCurveSpecificationBuilder(configSource);
    _fundingCurveSpecification = curveSpecificationBuilder.buildCurve(_curveDate, _fundingCurveDefinition);
    _forwardCurveSpecification = curveSpecificationBuilder.buildCurve(_curveDate, _forwardCurveDefinition);
    _fundingCurveRequirements = buildRequirements(_fundingCurveSpecification, context);
    _forwardCurveRequirements = buildRequirements(_forwardCurveSpecification, context);
    _fundingCurveResult = new ValueSpecification(new ValueRequirement(_fundingCurveValueRequirementName, _currency), getUniqueIdentifier());
    _forwardCurveResult = new ValueSpecification(new ValueRequirement(_forwardCurveValueRequirementName, _currency), getUniqueIdentifier());
    _jacobianResult = new ValueSpecification(new ValueRequirement(ValueRequirementNames.YIELD_CURVE_JACOBIAN, _currency), getUniqueIdentifier());
    _results = Sets.newHashSet(_fundingCurveResult, _forwardCurveResult, _jacobianResult);
  }

  @SuppressWarnings("unchecked")
  public static Set<ValueRequirement> buildRequirements(final InterpolatedYieldCurveSpecification specification, final FunctionCompilationContext context) {
    final Set<ValueRequirement> result = new HashSet<ValueRequirement>();
    for (final FixedIncomeStripWithIdentifier strip : specification.getStrips()) {
      result.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, strip.getSecurity()));
    }
    final ConventionBundleSource conventionBundleSource = OpenGammaCompilationContext.getConventionBundleSource(context);
    final ConventionBundle conventionBundle = conventionBundleSource.getConventionBundle(Identifier.of(InMemoryConventionBundleMaster.SIMPLE_NAME_SCHEME, specification.getCurrency()
        .getISOCode() + "_SWAP"));
    final ConventionBundle referenceRateConvention = conventionBundleSource.getConventionBundle(IdentifierBundle.of(conventionBundle.getSwapFloatingLegInitialRate()));
    final Identifier initialRefRateId = Identifier.of(IdentificationScheme.BLOOMBERG_TICKER, referenceRateConvention.getIdentifiers().getIdentifier(IdentificationScheme.BLOOMBERG_TICKER));
    result.add(new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, initialRefRateId));
    return Collections.unmodifiableSet(result);
  }

  public InterpolatedYieldCurveSpecification getFundingCurveSpecification() {
    return _fundingCurveSpecification;
  }

  // just for debugging.
  public Map<Identifier, Double> getIdentifierToFundingNodeTimesMap() {
    return _identifierToFundingNodeTimes;
  }

  public Map<Identifier, Double> getIdentifierToForwardNodeTimesMap() {
    return _identifierToForwardNodeTimes;
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    if (target.getType() != ComputationTargetType.PRIMITIVE) {
      return false;
    }
    return ObjectUtils.equals(target.getUniqueIdentifier(), getFundingCurveSpecification().getCurrency().getUniqueIdentifier());
  }

  @Override
  public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target) {
    if (canApplyTo(context, target)) {
      Set<ValueRequirement> result = new HashSet<ValueRequirement>();
      result.addAll(_fundingCurveRequirements);
      result.addAll(_forwardCurveRequirements);
      return result;
    }
    return null;
  }

  @Override
  public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
    if (canApplyTo(context, target)) {
      return _results;
    }
    return null;
  }

  @Override
  public String getShortName() {
    return "[" + _fundingCurveDefinitionName + ", " + _forwardCurveDefinitionName + "]" + "_MarketInstrumentImpliedYieldCurveFunction";
  }

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.PRIMITIVE;
  }

  private Map<Identifier, Double> buildMarketDataMap(final FunctionInputs inputs) {
    final Map<Identifier, Double> marketDataMap = new HashMap<Identifier, Double>();
    for (final ComputedValue value : inputs.getAllValues()) {
      final ComputationTargetSpecification targetSpecification = value.getSpecification().getRequirementSpecification().getTargetSpecification();
      if (value.getValue() instanceof Double) {
        marketDataMap.put(targetSpecification.getIdentifier(), (Double) value.getValue());
      }
    }
    return marketDataMap;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<ComputedValue> execute(final FunctionExecutionContext executionContext, final FunctionInputs inputs, final ComputationTarget target, final Set<ValueRequirement> desiredValues) {
    final FixedIncomeStripIdentifierAndMaturityBuilder builder = new FixedIncomeStripIdentifierAndMaturityBuilder(OpenGammaExecutionContext.getRegionSource(executionContext),
        OpenGammaExecutionContext.getConventionBundleSource(executionContext), executionContext.getSecuritySource());
    final Clock snapshotClock = executionContext.getSnapshotClock();
    final ZonedDateTime now = snapshotClock.zonedDateTime();
    final HolidaySource holidaySource = OpenGammaExecutionContext.getHolidaySource(executionContext);
    final RegionSource regionSource = OpenGammaExecutionContext.getRegionSource(executionContext);
    final ConventionBundleSource conventionSource = OpenGammaExecutionContext.getConventionBundleSource(executionContext);
    final FixedFloatSwapSecurityToSwapConverter swapConverter = new FixedFloatSwapSecurityToSwapConverter(holidaySource, regionSource, conventionSource);
    final CashSecurityToCashConverter cashConverter = new CashSecurityToCashConverter(holidaySource, conventionSource);
    final FRASecurityToForwardRateAgreementConverter fraConverter = new FRASecurityToForwardRateAgreementConverter(holidaySource, conventionSource);
    final InterestRateFutureSecurityToInterestRateFutureConverter futureConverter = new InterestRateFutureSecurityToInterestRateFutureConverter(holidaySource, conventionSource);
    final TenorSwapSecurityToTenorSwapConverter tenorSwapConverter = new TenorSwapSecurityToTenorSwapConverter(holidaySource, regionSource, conventionSource);

    if (_fundingCurveDefinitionName.equals(_forwardCurveDefinitionName)) {
      //TODO going to arbitrarily use funding curve - will give the same result as forward curve
      InterpolatedYieldCurveSpecificationWithSecurities specificationWithSecurities = builder.resolveToSecurity(_fundingCurveSpecification, buildMarketDataMap(inputs));
      List<InterestRateDerivative> derivatives = new ArrayList<InterestRateDerivative>();
      Set<FixedIncomeStrip> strips = _fundingCurveDefinition.getStrips();
      int n = strips.size();
      double[] initialRatesGuess = new double[n];
      double[] nodeTimes = new double[n];
      _identifierToFundingNodeTimes.clear();
      _identifierToForwardNodeTimes.clear();
      int i = 0;
      for (FixedIncomeStripWithSecurity strip : specificationWithSecurities.getStrips()) {
        ValueRequirement stripRequirement = new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, strip.getSecurityIdentifier());
        final Double marketValue = (Double) inputs.getValue(stripRequirement);
        if (marketValue == null) {
          throw new NullPointerException("Could not get market data for " + strip);
        }
        InterestRateDerivative derivative;
        if (strip.getInstrumentType() == StripInstrumentType.SWAP) {
          derivative = swapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, marketValue / 100., 0.0, now);
        } else if (strip.getInstrumentType() == StripInstrumentType.CASH) {
          derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _fundingCurveDefinitionName, marketValue / 100., now);
        } else if (strip.getInstrumentType() == StripInstrumentType.FRA) {
          derivative = fraConverter.getFRA((FRASecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, marketValue / 100., now);
        } else if (strip.getInstrumentType() == StripInstrumentType.FUTURE) {
          derivative = futureConverter.getInterestRateFuture((InterestRateFutureSecurity) strip.getSecurity(), _fundingCurveDefinitionName, marketValue, now);
        } else if (strip.getInstrumentType() == StripInstrumentType.LIBOR) {
          derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _fundingCurveDefinitionName, marketValue / 100., now);
        //} else if (strip.getInstrumentType() == StripInstrumentType.BASIS_SWAP) {
          //derivative = basisSwapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, _fundingCurveDefinitionName, marketValue / 100., now);
        } else if (strip.getInstrumentType() == StripInstrumentType.TENOR_SWAP) {
          derivative = tenorSwapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, _fundingCurveDefinitionName, marketValue / 10000., now);
        } else {
          throw new OpenGammaRuntimeException("Can only handle swap, cash, LIBOR, FRA, IR futures and tenor swaps at the moment");
        }
        if (derivative == null) {
          throw new NullPointerException("Had a null InterestRateDefinition for " + strip);
        }
        derivatives.add(derivative);
        initialRatesGuess[i] = 0.01;
        nodeTimes[i] = LAST_DATE_CALCULATOR.getValue(derivative);
        _identifierToFundingNodeTimes.put(strip.getSecurityIdentifier(), nodeTimes[i]); // just for debugging.
        _identifierToForwardNodeTimes.put(strip.getSecurityIdentifier(), nodeTimes[i]); // just for debugging.
        i++;
      }
      ParallelArrayBinarySort.parallelBinarySort(nodeTimes, initialRatesGuess);
      LinkedHashMap<String, double[]> curveKnots = new LinkedHashMap<String, double[]>();
      curveKnots.put(_fundingCurveDefinitionName, nodeTimes);
      final LinkedHashMap<String, double[]> curveNodes = new LinkedHashMap<String, double[]>();
      final LinkedHashMap<String, Interpolator1D<? extends Interpolator1DDataBundle>> interpolators = new LinkedHashMap<String, Interpolator1D<? extends Interpolator1DDataBundle>>();
      final LinkedHashMap<String, Interpolator1DNodeSensitivityCalculator<? extends Interpolator1DDataBundle>> sensitivityCalculators = 
        new LinkedHashMap<String, Interpolator1DNodeSensitivityCalculator<? extends Interpolator1DDataBundle>>();
      final Interpolator1D interpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(_fundingCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR);
      curveNodes.put(_fundingCurveDefinitionName, nodeTimes);
      interpolators.put(_fundingCurveDefinitionName, interpolator);
      //TODO have use finite difference or not as an input [FIN-147]
      final Interpolator1DNodeSensitivityCalculator sensitivityCalculator = CombinedInterpolatorExtrapolatorNodeSensitivityCalculatorFactory.getSensitivityCalculator(
          _fundingCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, false);
      sensitivityCalculators.put(_fundingCurveDefinitionName, sensitivityCalculator);
      final MultipleYieldCurveFinderDataBundle data = new MultipleYieldCurveFinderDataBundle(derivatives, null, curveNodes, interpolators, sensitivityCalculators);
      //TODO have the calculator and sensitivity calculators as an input [FIN-144], [FIN-145]
      final Function1D<DoubleMatrix1D, DoubleMatrix1D> curveCalculator = new MultipleYieldCurveFinderFunction(data, PresentValueCalculator.getInstance());
      final Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianCalculator = new MultipleYieldCurveFinderJacobian(data, PresentValueSensitivityCalculator.getInstance());
      NewtonVectorRootFinder rootFinder;
      double[] yields = null;
      try {
        //TODO have the decomposition as an optional input [FIN-146]
        rootFinder = new BroydenVectorRootFinder(1e-7, 1e-7, 100, DecompositionFactory.getDecomposition(DecompositionFactory.LU_COMMONS_NAME));
        yields = rootFinder.getRoot(curveCalculator, jacobianCalculator, new DoubleMatrix1D(initialRatesGuess)).getData();
      } catch (final RootNotFoundException e) {
        rootFinder = new BroydenVectorRootFinder(1e-7, 1e-7, 100, DecompositionFactory.getDecomposition(DecompositionFactory.SV_COMMONS_NAME));
        yields = rootFinder.getRoot(curveCalculator, jacobianCalculator, new DoubleMatrix1D(initialRatesGuess)).getData();

      }

      final YieldAndDiscountCurve fundingCurve = new InterpolatedYieldCurve(nodeTimes, yields, interpolator);
      final YieldAndDiscountCurve forwardCurve = new InterpolatedYieldCurve(nodeTimes, yields, interpolator);
      final DoubleMatrix2D jacobianMatrix = jacobianCalculator.evaluate(new DoubleMatrix1D(yields));
      return Sets.newHashSet(new ComputedValue(_fundingCurveResult, fundingCurve), new ComputedValue(_forwardCurveResult, forwardCurve), new ComputedValue(_jacobianResult, jacobianMatrix.getData()));
    }
    
    
    
    final InterpolatedYieldCurveSpecificationWithSecurities fundingCurveSpecificationWithSecurities = builder.resolveToSecurity(_fundingCurveSpecification, buildMarketDataMap(inputs));
    final InterpolatedYieldCurveSpecificationWithSecurities forwardCurveSpecificationWithSecurities = builder.resolveToSecurity(_forwardCurveSpecification, buildMarketDataMap(inputs));
    final List<InterestRateDerivative> derivatives = new ArrayList<InterestRateDerivative>();
    final Set<FixedIncomeStrip> fundingStrips = _fundingCurveDefinition.getStrips();
    final Set<FixedIncomeStrip> forwardStrips = _forwardCurveDefinition.getStrips();
    int nFunding = fundingStrips.size();
    int nForward = forwardStrips.size();
    final double[] initialRatesGuess = new double[nFunding + nForward];   
    double[] fundingNodeTimes = new double[nFunding];
    double[] forwardNodeTimes = new double[nForward];
    _identifierToFundingNodeTimes.clear();
    _identifierToForwardNodeTimes.clear();
    int i = 0, fundingIndex = 0, forwardIndex = 0;
    for (final FixedIncomeStripWithSecurity strip : fundingCurveSpecificationWithSecurities.getStrips()) {
      ValueRequirement stripRequirement = new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, strip.getSecurityIdentifier());
      final Double marketValue = (Double) inputs.getValue(stripRequirement);
      if (marketValue == null) {
        throw new NullPointerException("Could not get market data for " + strip);
      }
      InterestRateDerivative derivative;
      if (strip.getInstrumentType() == StripInstrumentType.SWAP) {
        derivative = swapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 100., 0.0, now);
      } else if (strip.getInstrumentType() == StripInstrumentType.CASH) {
        derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _fundingCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.FRA) {
        derivative = fraConverter.getFRA((FRASecurity) strip.getSecurity(), _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.FUTURE) {
        derivative = futureConverter.getInterestRateFuture((InterestRateFutureSecurity) strip.getSecurity(), _forwardCurveDefinitionName, marketValue, now);
      } else if (strip.getInstrumentType() == StripInstrumentType.LIBOR) {
        derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _forwardCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.TENOR_SWAP) {
        derivative = tenorSwapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 10000., now);
      } else {
        throw new OpenGammaRuntimeException("Can only handle swap, cash, LIBOR, FRA, IR futures and tenor swaps at the moment");
      }
      if (derivative == null) {
        throw new NullPointerException("Had a null InterestRateDefinition for " + strip);
      }
      derivatives.add(derivative);
      initialRatesGuess[i++] = 0.01;
      fundingNodeTimes[fundingIndex] = LAST_DATE_CALCULATOR.getValue(derivative);
      _identifierToFundingNodeTimes.put(strip.getSecurityIdentifier(), fundingNodeTimes[fundingIndex]); // just for debugging.
      fundingIndex++;
    }
    for (final FixedIncomeStripWithSecurity strip : forwardCurveSpecificationWithSecurities.getStrips()) {
      ValueRequirement stripRequirement = new ValueRequirement(MarketDataRequirementNames.MARKET_VALUE, strip.getSecurityIdentifier());
      final Double marketValue = (Double) inputs.getValue(stripRequirement);
      if (marketValue == null) {
        throw new NullPointerException("Could not get market data for " + strip);
      }
      InterestRateDerivative derivative;
      if (strip.getInstrumentType() == StripInstrumentType.SWAP) {
        derivative = swapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 100., 0.0, now);
      } else if (strip.getInstrumentType() == StripInstrumentType.CASH) {
        derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _fundingCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.FRA) {
        derivative = fraConverter.getFRA((FRASecurity) strip.getSecurity(), _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.FUTURE) {
        derivative = futureConverter.getInterestRateFuture((InterestRateFutureSecurity) strip.getSecurity(), _forwardCurveDefinitionName, marketValue, now);
      } else if (strip.getInstrumentType() == StripInstrumentType.LIBOR) {
        derivative = cashConverter.getCash((CashSecurity) strip.getSecurity(), _forwardCurveDefinitionName, marketValue / 100., now);
      } else if (strip.getInstrumentType() == StripInstrumentType.TENOR_SWAP) {
        derivative = tenorSwapConverter.getSwap((SwapSecurity) strip.getSecurity(), _fundingCurveDefinitionName, _fundingCurveDefinitionName, _forwardCurveDefinitionName, marketValue / 10000., now);
      } else {
        throw new OpenGammaRuntimeException("Can only handle swap, cash, LIBOR, FRA, IR futures and tenor swaps at the moment");
      }
      if (derivative == null) {
        throw new NullPointerException("Had a null InterestRateDefinition for " + strip);
      }
      derivatives.add(derivative);
      initialRatesGuess[i++] = 0.01;
      forwardNodeTimes[forwardIndex] = LAST_DATE_CALCULATOR.getValue(derivative);
      _identifierToForwardNodeTimes.put(strip.getSecurityIdentifier(), forwardNodeTimes[forwardIndex]); // just for debugging.
      forwardIndex++;
    }
    Arrays.sort(fundingNodeTimes);
    Arrays.sort(forwardNodeTimes);
    //ParallelArrayBinarySort.parallelBinarySort(fundingNodeTimes, initialRatesGuess); //TODO will eventually need two sets of rates guesses
    //ParallelArrayBinarySort.parallelBinarySort(fundingNodeTimes, initialRatesGuess); //TODO will eventually need two sets of rates guesses
    LinkedHashMap<String, double[]> curveKnots = new LinkedHashMap<String, double[]>();
    curveKnots.put(_fundingCurveDefinitionName, fundingNodeTimes);
    curveKnots.put(_forwardCurveDefinitionName, forwardNodeTimes);
    final LinkedHashMap<String, double[]> curveNodes = new LinkedHashMap<String, double[]>();
    final LinkedHashMap<String, Interpolator1D<? extends Interpolator1DDataBundle>> interpolators = new LinkedHashMap<String, Interpolator1D<? extends Interpolator1DDataBundle>>();
    final LinkedHashMap<String, Interpolator1DNodeSensitivityCalculator<? extends Interpolator1DDataBundle>> sensitivityCalculators = 
      new LinkedHashMap<String, Interpolator1DNodeSensitivityCalculator<? extends Interpolator1DDataBundle>>();
    final Interpolator1D fundingInterpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(_fundingCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    final Interpolator1D forwardInterpolator = CombinedInterpolatorExtrapolatorFactory.getInterpolator(_forwardCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR,
        Interpolator1DFactory.FLAT_EXTRAPOLATOR);
    curveNodes.put(_fundingCurveDefinitionName, fundingNodeTimes);
    interpolators.put(_fundingCurveDefinitionName, fundingInterpolator);
    curveNodes.put(_forwardCurveDefinitionName, forwardNodeTimes);
    interpolators.put(_forwardCurveDefinitionName, forwardInterpolator);
    //TODO have use finite difference or not as an input [FIN-147]
    final Interpolator1DNodeSensitivityCalculator fundingSensitivityCalculator = CombinedInterpolatorExtrapolatorNodeSensitivityCalculatorFactory.getSensitivityCalculator(
        _fundingCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, false);
    final Interpolator1DNodeSensitivityCalculator forwardSensitivityCalculator = CombinedInterpolatorExtrapolatorNodeSensitivityCalculatorFactory.getSensitivityCalculator(
        _forwardCurveDefinition.getInterpolatorName(), Interpolator1DFactory.LINEAR_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR, false);
    sensitivityCalculators.put(_fundingCurveDefinitionName, fundingSensitivityCalculator);
    sensitivityCalculators.put(_forwardCurveDefinitionName, forwardSensitivityCalculator);
    final MultipleYieldCurveFinderDataBundle data = new MultipleYieldCurveFinderDataBundle(derivatives, null, curveNodes, interpolators, sensitivityCalculators);
    //TODO have the calculator and sensitivity calculators as an input [FIN-144], [FIN-145]
    final Function1D<DoubleMatrix1D, DoubleMatrix1D> curveCalculator = new MultipleYieldCurveFinderFunction(data, PresentValueCalculator.getInstance());
    final Function1D<DoubleMatrix1D, DoubleMatrix2D> jacobianCalculator = new MultipleYieldCurveFinderJacobian(data, PresentValueSensitivityCalculator.getInstance());
    NewtonVectorRootFinder rootFinder;
    double[] yields = null;
    try {
      //TODO have the decomposition as an optional input [FIN-146]
      rootFinder = new BroydenVectorRootFinder(1e-7, 1e-7, 100, DecompositionFactory.getDecomposition(DecompositionFactory.LU_COMMONS_NAME));
      yields = rootFinder.getRoot(curveCalculator, jacobianCalculator, new DoubleMatrix1D(initialRatesGuess)).getData();
    } catch (final RootNotFoundException e) {
      rootFinder = new BroydenVectorRootFinder(1e-7, 1e-7, 100, DecompositionFactory.getDecomposition(DecompositionFactory.SV_COMMONS_NAME));
      yields = rootFinder.getRoot(curveCalculator, jacobianCalculator, new DoubleMatrix1D(initialRatesGuess)).getData();

    }
    //TODO duh the yields have to be split up
    double[] fundingYields = Arrays.copyOfRange(yields, 0,fundingNodeTimes.length);
    double[] forwardYields = Arrays.copyOfRange(yields, fundingNodeTimes.length, yields.length);
    final YieldAndDiscountCurve fundingCurve = new InterpolatedYieldCurve(fundingNodeTimes, fundingYields, fundingInterpolator);
    final YieldAndDiscountCurve forwardCurve = new InterpolatedYieldCurve(forwardNodeTimes, forwardYields, forwardInterpolator);
    final DoubleMatrix2D jacobianMatrix = jacobianCalculator.evaluate(new DoubleMatrix1D(yields));
    return Sets.newHashSet(new ComputedValue(_fundingCurveResult, fundingCurve), new ComputedValue(_forwardCurveResult, forwardCurve), new ComputedValue(_jacobianResult, jacobianMatrix.getData()));
  }
}
