/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.examples.function;

import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.LINEAR;
import static com.opengamma.financial.analytics.model.curve.interestrate.MarketInstrumentImpliedYieldCurveFunction.PAR_RATE_STRING;
import static com.opengamma.financial.analytics.model.curve.interestrate.MarketInstrumentImpliedYieldCurveFunction.PRESENT_VALUE_STRING;
import static com.opengamma.master.historicaltimeseries.impl.HistoricalTimeSeriesRatingFieldNames.DEFAULT_CONFIG_NAME;

import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fudgemsg.FudgeContext;
import org.fudgemsg.FudgeMsg;
import org.fudgemsg.FudgeMsgFormatter;
import org.fudgemsg.wire.FudgeMsgWriter;
import org.fudgemsg.wire.xml.FudgeXMLSettings;
import org.fudgemsg.wire.xml.FudgeXMLStreamWriter;

import com.opengamma.analytics.financial.schedule.ScheduleCalculatorFactory;
import com.opengamma.analytics.financial.schedule.TimeSeriesSamplingFunctionFactory;
import com.opengamma.analytics.financial.timeseries.returns.TimeSeriesReturnCalculatorFactory;
import com.opengamma.analytics.math.statistics.descriptive.StatisticsCalculatorFactory;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.function.FunctionDefinition;
import com.opengamma.engine.function.config.FunctionConfiguration;
import com.opengamma.engine.function.config.ParameterizedFunctionConfiguration;
import com.opengamma.engine.function.config.RepositoryConfiguration;
import com.opengamma.engine.function.config.RepositoryConfigurationSource;
import com.opengamma.engine.function.config.SimpleRepositoryConfigurationSource;
import com.opengamma.engine.function.config.StaticFunctionConfiguration;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.financial.FinancialFunctions;
import com.opengamma.financial.aggregation.BottomPositionValues;
import com.opengamma.financial.aggregation.SortedPositionValues;
import com.opengamma.financial.aggregation.TopPositionValues;
import com.opengamma.financial.analytics.ircurve.DefaultYieldCurveMarketDataShiftFunction;
import com.opengamma.financial.analytics.ircurve.DefaultYieldCurveShiftFunction;
import com.opengamma.financial.analytics.ircurve.YieldCurveMarketDataShiftFunction;
import com.opengamma.financial.analytics.ircurve.YieldCurveShiftFunction;
import com.opengamma.financial.analytics.model.bond.BondCleanPriceFromCurvesFunction;
import com.opengamma.financial.analytics.model.bond.BondCleanPriceFromYieldFunction;
import com.opengamma.financial.analytics.model.bond.BondCouponPaymentDiaryFunction;
import com.opengamma.financial.analytics.model.bond.BondDefaultCurveNamesFunction;
import com.opengamma.financial.analytics.model.bond.BondDirtyPriceFromCurvesFunction;
import com.opengamma.financial.analytics.model.bond.BondDirtyPriceFromYieldFunction;
import com.opengamma.financial.analytics.model.bond.BondMacaulayDurationFromCurvesFunction;
import com.opengamma.financial.analytics.model.bond.BondMacaulayDurationFromYieldFunction;
import com.opengamma.financial.analytics.model.bond.BondMarketCleanPriceFunction;
import com.opengamma.financial.analytics.model.bond.BondMarketDirtyPriceFunction;
import com.opengamma.financial.analytics.model.bond.BondMarketYieldFunction;
import com.opengamma.financial.analytics.model.bond.BondModifiedDurationFromCurvesFunction;
import com.opengamma.financial.analytics.model.bond.BondModifiedDurationFromYieldFunction;
import com.opengamma.financial.analytics.model.bond.BondTenorFunction;
import com.opengamma.financial.analytics.model.bond.BondYieldFromCurvesFunction;
import com.opengamma.financial.analytics.model.bond.BondZSpreadFromCurveCleanPriceFunction;
import com.opengamma.financial.analytics.model.bond.BondZSpreadFromMarketCleanPriceFunction;
import com.opengamma.financial.analytics.model.bond.BondZSpreadPresentValueSensitivityFromCurveCleanPriceFunction;
import com.opengamma.financial.analytics.model.bond.BondZSpreadPresentValueSensitivityFromMarketCleanPriceFunction;
import com.opengamma.financial.analytics.model.bond.NelsonSiegelSvenssonBondCurveFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxCDSPriceFlatSpreadFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxCDSPriceHazardCurveFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxDiscountCurveFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxFlatSpreadFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldForwardFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldFuturesYCNSFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldPV01FuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldPresentValueFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldSpotFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldValueDeltaFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldValueRhoFuturesFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMBetaDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMBetaDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMBetaModelPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMBetaModelPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMFromRegressionDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMFromRegressionDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.CAPMFromRegressionModelFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.JensenAlphaDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.JensenAlphaDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.JensenAlphaFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.SharpeRatioDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.SharpeRatioDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.SharpeRatioPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.SharpeRatioPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.StandardEquityModelFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TotalRiskAlphaDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TotalRiskAlphaDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TotalRiskAlphaPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TotalRiskAlphaPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TreynorRatioDefaultPropertiesPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TreynorRatioDefaultPropertiesPositionFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TreynorRatioPortfolioNodeFunction;
import com.opengamma.financial.analytics.model.equity.portfoliotheory.TreynorRatioPositionFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityForwardFromSpotAndYieldCurveFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapStaticReplicationPresentValueFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapStaticReplicationVegaFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapStaticReplicationYCNSFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentPV01Function;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentParRateCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentParRateFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentParRateParallelCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentPresentValueFunction;
import com.opengamma.financial.analytics.model.fixedincome.InterestRateInstrumentYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentDefaultCurveNameFunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentPV01FunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentParRateCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentParRateFunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentParRateParallelCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.fixedincome.deprecated.InterestRateInstrumentYieldCurveNodeSensitivitiesFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.defaultproperties.FXForwardDefaultsDeprecated;
import com.opengamma.financial.analytics.model.forex.defaultproperties.FXOptionBlackDefaultsDeprecated;
import com.opengamma.financial.analytics.model.forex.forward.deprecated.FXForwardCurrencyExposureFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.forward.deprecated.FXForwardPresentValueCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.forward.deprecated.FXForwardPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.forward.deprecated.FXForwardYCNSFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackCurrencyExposureFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackPresentValueCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackVegaFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackVegaMatrixFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackVegaQuoteMatrixFunctionDeprecated;
import com.opengamma.financial.analytics.model.forex.option.black.deprecated.FXOptionBlackYCNSFunctionDeprecated;
import com.opengamma.financial.analytics.model.future.BondFutureGrossBasisFromCurvesFunction;
import com.opengamma.financial.analytics.model.future.BondFutureNetBasisFromCurvesFunction;
import com.opengamma.financial.analytics.model.future.InterestRateFutureDefaultValuesFunctionDeprecated;
import com.opengamma.financial.analytics.model.future.InterestRateFuturePV01FunctionDeprecated;
import com.opengamma.financial.analytics.model.future.InterestRateFuturePresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.future.InterestRateFutureYieldCurveNodeSensitivitiesFunctionDeprecated;
import com.opengamma.financial.analytics.model.option.AnalyticOptionDefaultCurveFunction;
import com.opengamma.financial.analytics.model.option.BlackScholesMertonModelFunction;
import com.opengamma.financial.analytics.model.option.BlackScholesModelCostOfCarryFunction;
import com.opengamma.financial.analytics.model.pnl.EquityPnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.EquityPnLFunction;
import com.opengamma.financial.analytics.model.pnl.ExternallyProvidedSensitivityPnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.ExternallyProvidedSensitivityPnLFunction;
import com.opengamma.financial.analytics.model.pnl.PortfolioExchangeTradedDailyPnLFunction;
import com.opengamma.financial.analytics.model.pnl.PortfolioExchangeTradedPnLFunction;
import com.opengamma.financial.analytics.model.pnl.PositionExchangeTradedDailyPnLFunction;
import com.opengamma.financial.analytics.model.pnl.PositionExchangeTradedPnLFunction;
import com.opengamma.financial.analytics.model.pnl.SecurityPriceSeriesDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.SecurityPriceSeriesFunction;
import com.opengamma.financial.analytics.model.pnl.SimpleFXFuturePnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.SimpleFXFuturePnLFunction;
import com.opengamma.financial.analytics.model.pnl.SimpleFuturePnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.SimpleFuturePnLFunction;
import com.opengamma.financial.analytics.model.pnl.TradeExchangeTradedDailyPnLFunction;
import com.opengamma.financial.analytics.model.pnl.TradeExchangeTradedPnLFunction;
import com.opengamma.financial.analytics.model.pnl.ValueGreekSensitivityPnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.ValueGreekSensitivityPnLFunction;
import com.opengamma.financial.analytics.model.pnl.YieldCurveNodePnLFunctionDeprecated;
import com.opengamma.financial.analytics.model.pnl.YieldCurveNodeSensitivityPnLDefaultsDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationVegaFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationYCNSFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVSABRSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationVegaFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationYCNSFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVCurveSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVSABRNodeSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVSABRSensitivityFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationVegaFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationYCNSFunctionDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationDefaultsDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationVegaDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationVegaDefaultsDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationDefaultsDeprecated;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationVegaDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationVegaDefaultsDeprecated;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSecurityMarkFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesCreditFactorsFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesNonYieldCurveFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurveCS01Function;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurvePV01Function;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFXFuturePresentValueFunction;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFuturePresentValueFunctionDeprecated;
import com.opengamma.financial.analytics.model.var.NormalHistoricalVaRDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.var.NormalHistoricalVaRFunction;
import com.opengamma.financial.analytics.model.volatility.SmileFittingProperties;
import com.opengamma.financial.analytics.model.volatility.cube.SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults;
import com.opengamma.financial.analytics.model.volatility.cube.SABRNonLinearLeastSquaresSwaptionCubeFittingFunction;
import com.opengamma.financial.analytics.model.volatility.surface.BlackScholesMertonImpliedVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.volatility.surface.DefaultVolatilitySurfaceShiftFunction;
import com.opengamma.financial.analytics.volatility.surface.VolatilitySurfaceShiftFunction;
import com.opengamma.financial.currency.CurrencyConversionFunction;
import com.opengamma.financial.currency.CurrencyMatrixConfigPopulator;
import com.opengamma.financial.currency.CurrencyMatrixSourcingFunction;
import com.opengamma.financial.currency.DefaultCurrencyFunction;
import com.opengamma.financial.currency.FixedIncomeInstrumentPnLSeriesCurrencyConversionFunction;
import com.opengamma.financial.property.AggregationDefaultPropertyFunction;
import com.opengamma.financial.property.AttributableDefaultPropertyFunction;
import com.opengamma.financial.property.CalcConfigDefaultPropertyFunction;
import com.opengamma.financial.property.DefaultPropertyFunction.PriorityClass;
import com.opengamma.financial.property.PositionDefaultPropertyFunction;
import com.opengamma.financial.value.ValueFunction;
import com.opengamma.util.SingletonFactoryBean;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.util.money.Currency;

/**
 * Constructs a standard function repository.
 * <p>
 * This should be replaced by something that loads the functions from the configuration database
 */
public class ExampleStandardFunctionConfiguration extends SingletonFactoryBean<RepositoryConfigurationSource> {

  // TODO: Change to inherit from AbstractRepositoryConfigurationBean

  private static final String USD = Currency.USD.getCode();
  private static final String SECONDARY = "SECONDARY";
  private static final String COST_OF_CARRY_FIELD = "COST_OF_CARRY";
  private static final String HTS_PRICE_FIELD = "CLOSE";
  private static final boolean OUTPUT_REPO_CONFIGURATION = false;

  public static <F extends FunctionDefinition> FunctionConfiguration functionConfiguration(final Class<F> clazz, final String... args) {
    if (Modifier.isAbstract(clazz.getModifiers())) {
      throw new IllegalStateException("Attempting to register an abstract class - " + clazz);
    }
    if (args.length == 0) {
      return new StaticFunctionConfiguration(clazz.getName());
    } else {
      return new ParameterizedFunctionConfiguration(clazz.getName(), Arrays.asList(args));
    }
  }

  protected static void addValueFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(ValueFunction.class));
  }

  protected static void addCurrencyConversionFunctions(final List<FunctionConfiguration> functionConfigs, final String requirementName) {
    functionConfigs.add(functionConfiguration(CurrencyConversionFunction.class, requirementName));
    functionConfigs.add(functionConfiguration(DefaultCurrencyFunction.Permissive.class, requirementName));
  }

  protected static void addCurrencyConversionFunctions(final List<FunctionConfiguration> functionConfigs) {
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.FAIR_VALUE);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.PV01);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.PRESENT_VALUE);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.DAILY_PNL);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_DELTA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_GAMMA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_SPEED);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.YIELD_CURVE_NODE_SENSITIVITIES);
    functionConfigs.add(functionConfiguration(CurrencyMatrixSourcingFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FixedIncomeInstrumentPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
  }

  protected static void addLateAggregationFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(BottomPositionValues.class));
    functionConfigs.add(functionConfiguration(SortedPositionValues.class));
    functionConfigs.add(functionConfiguration(TopPositionValues.class));
  }

  protected static void addDataShiftingFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(VolatilitySurfaceShiftFunction.class));
    functionConfigs.add(functionConfiguration(DefaultVolatilitySurfaceShiftFunction.class));
    functionConfigs.add(functionConfiguration(YieldCurveShiftFunction.class));
    functionConfigs.add(functionConfiguration(DefaultYieldCurveShiftFunction.class));
    functionConfigs.add(functionConfiguration(YieldCurveMarketDataShiftFunction.class));
    functionConfigs.add(functionConfiguration(DefaultYieldCurveMarketDataShiftFunction.class));
  }

  protected static void addDefaultPropertyFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(CalcConfigDefaultPropertyFunction.Generic.class));
    functionConfigs.add(functionConfiguration(CalcConfigDefaultPropertyFunction.Specific.class));
    functionConfigs.add(functionConfiguration(PositionDefaultPropertyFunction.class));
    functionConfigs.add(functionConfiguration(AttributableDefaultPropertyFunction.class));
  }

  public static RepositoryConfiguration constructRepositoryConfiguration() {
    final List<FunctionConfiguration> functionConfigs = new ArrayList<FunctionConfiguration>();

    addValueFunctions(functionConfigs);

    // options
    functionConfigs.add(functionConfiguration(BlackScholesMertonModelFunction.class));
    functionConfigs.add(functionConfiguration(BlackScholesMertonImpliedVolatilitySurfaceFunction.class));
    functionConfigs.add(functionConfiguration(BlackScholesModelCostOfCarryFunction.class));

    // equity and portfolio
    functionConfigs.add(functionConfiguration(PositionExchangeTradedPnLFunction.class));
    functionConfigs.add(functionConfiguration(PortfolioExchangeTradedPnLFunction.class));
    functionConfigs.add(functionConfiguration(PortfolioExchangeTradedDailyPnLFunction.Impl.class));
    functionConfigs.add(functionConfiguration(AggregationDefaultPropertyFunction.class, ValueRequirementNames.DAILY_PNL, PortfolioExchangeTradedDailyPnLFunction.Impl.AGGREGATION_STYLE_FULL));

    addPnLCalculators(functionConfigs);
    addVaRCalculators(functionConfigs);
    addPortfolioAnalysisCalculators(functionConfigs);
    addFixedIncomeInstrumentCalculators(functionConfigs);
    addDeprecatedFixedIncomeInstrumentCalculators(functionConfigs);

    functionConfigs.add(functionConfiguration(StandardEquityModelFunction.class));
    functionConfigs.add(functionConfiguration(SimpleFuturePresentValueFunctionDeprecated.class, SECONDARY));
    functionConfigs.add(functionConfiguration(SimpleFXFuturePresentValueFunction.class, SECONDARY, SECONDARY));
    addBondCalculators(functionConfigs);
    addBondFutureCalculators(functionConfigs);
    addSABRCalculators(functionConfigs);
    addDeprecatedSABRCalculators(functionConfigs);
    addForexOptionCalculators(functionConfigs);
    addForexForwardCalculators(functionConfigs);
    addInterestRateFutureCalculators(functionConfigs);
    addInterestRateFutureOptionCalculators(functionConfigs);
    addEquityDerivativesCalculators(functionConfigs);
    addExternallyProvidedSensitivitiesFunctions(functionConfigs);
    addCurrencyConversionFunctions(functionConfigs);
    addLateAggregationFunctions(functionConfigs);
    addDataShiftingFunctions(functionConfigs);
    addDefaultPropertyFunctions(functionConfigs);
    functionConfigs.add(functionConfiguration(AnalyticOptionDefaultCurveFunction.class, SECONDARY));

    functionConfigs.add(functionConfiguration(ISDAApproxCDSPriceFlatSpreadFunction.class));
    functionConfigs.add(functionConfiguration(ISDAApproxCDSPriceHazardCurveFunction.class));
    functionConfigs.add(functionConfiguration(ISDAApproxFlatSpreadFunction.class));
    functionConfigs.add(functionConfiguration(ISDAApproxDiscountCurveFunction.class));

    functionConfigs.addAll(FinancialFunctions.DEFAULT.getRepositoryConfiguration().getFunctions());

    final RepositoryConfiguration repoConfig = new RepositoryConfiguration(functionConfigs);

    if (OUTPUT_REPO_CONFIGURATION) {
      final FudgeMsg msg = OpenGammaFudgeContext.getInstance().toFudgeMsg(repoConfig).getMessage();
      FudgeMsgFormatter.outputToSystemOut(msg);
      try {
        final FudgeXMLSettings xmlSettings = new FudgeXMLSettings();
        xmlSettings.setEnvelopeElementName(null);
        final FudgeMsgWriter msgWriter = new FudgeMsgWriter(new FudgeXMLStreamWriter(FudgeContext.GLOBAL_DEFAULT, new OutputStreamWriter(System.out), xmlSettings));
        msgWriter.setDefaultMessageProcessingDirectives(0);
        msgWriter.setDefaultMessageVersion(0);
        msgWriter.setDefaultTaxonomyId(0);
        msgWriter.writeMessage(msg);
        msgWriter.flush();
      } catch (final Exception e) {
        // Just swallow it.
      }
    }
    return repoConfig;
  }

  private static void addPnLCalculators(final List<FunctionConfiguration> functionConfigs) {
    final String defaultCurveCalculationMethod = PRESENT_VALUE_STRING;
    final String defaultReturnCalculatorName = TimeSeriesReturnCalculatorFactory.SIMPLE_NET_LENIENT;
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingCalculatorName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    functionConfigs.add(functionConfiguration(TradeExchangeTradedPnLFunction.class, DEFAULT_CONFIG_NAME, HTS_PRICE_FIELD, COST_OF_CARRY_FIELD));
    functionConfigs.add(functionConfiguration(TradeExchangeTradedDailyPnLFunction.class, DEFAULT_CONFIG_NAME, HTS_PRICE_FIELD, COST_OF_CARRY_FIELD));
    functionConfigs.add(functionConfiguration(PositionExchangeTradedDailyPnLFunction.class, DEFAULT_CONFIG_NAME, HTS_PRICE_FIELD, COST_OF_CARRY_FIELD));
    functionConfigs.add(functionConfiguration(SecurityPriceSeriesFunction.class, DEFAULT_CONFIG_NAME, MarketDataRequirementNames.MARKET_VALUE));
    functionConfigs.add(functionConfiguration(SecurityPriceSeriesDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(EquityPnLFunction.class));
    functionConfigs.add(functionConfiguration(EquityPnLDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName, defaultReturnCalculatorName));
    functionConfigs.add(functionConfiguration(SimpleFuturePnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SimpleFuturePnLDefaultPropertiesFunction.class, SECONDARY, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(SimpleFXFuturePnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SimpleFXFuturePnLDefaultPropertiesFunction.class, SECONDARY, SECONDARY,
        defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(YieldCurveNodePnLFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(YieldCurveNodeSensitivityPnLDefaultsDeprecated.class, SECONDARY, SECONDARY, defaultCurveCalculationMethod, defaultSamplingPeriodName,
        defaultScheduleName, defaultSamplingCalculatorName, "AUD", USD, "CAD", "DKK", "EUR", "GBP", "JPY", "NZD", "CHF"));
    functionConfigs.add(functionConfiguration(ValueGreekSensitivityPnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(ValueGreekSensitivityPnLDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName,
        defaultReturnCalculatorName));
  }

  private static void addVaRCalculators(final List<FunctionConfiguration> functionConfigs) {
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingCalculatorName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    final String defaultMeanCalculatorName = StatisticsCalculatorFactory.MEAN;
    final String defaultStdDevCalculatorName = StatisticsCalculatorFactory.SAMPLE_STANDARD_DEVIATION;
    final String defaultConfidenceLevelName = "0.99";
    final String defaultHorizonName = "1";
    functionConfigs.add(functionConfiguration(NormalHistoricalVaRFunction.class));
    functionConfigs.add(functionConfiguration(NormalHistoricalVaRDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName,
        defaultMeanCalculatorName, defaultStdDevCalculatorName, defaultConfidenceLevelName, defaultHorizonName, PriorityClass.ABOVE_NORMAL.name()));
  }

  private static void addEquityDerivativesCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldForwardFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldPresentValueFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldPV01FuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldSpotFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldValueDeltaFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldValueRhoFuturesFunction.class.getName()));
    functionConfigs.add(functionConfiguration(EquityDividendYieldFuturesYCNSFunction.class));
    functionConfigs.add(functionConfiguration(EquityForwardFromSpotAndYieldCurveFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationVegaFunction.class));
  }

  private static void addBondFutureCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(BondFutureGrossBasisFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureNetBasisFromCurvesFunction.class));
  }

  private static void addBondCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(BondCouponPaymentDiaryFunction.class));
    functionConfigs.add(functionConfiguration(BondTenorFunction.class));
    functionConfigs.add(functionConfiguration(BondMarketCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(BondMarketDirtyPriceFunction.class));
    functionConfigs.add(functionConfiguration(BondMarketYieldFunction.class));
    functionConfigs.add(functionConfiguration(BondYieldFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondCleanPriceFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondDirtyPriceFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondMacaulayDurationFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondModifiedDurationFromCurvesFunction.class));
    functionConfigs.add(functionConfiguration(BondCleanPriceFromYieldFunction.class));
    functionConfigs.add(functionConfiguration(BondDirtyPriceFromYieldFunction.class));
    functionConfigs.add(functionConfiguration(BondMacaulayDurationFromYieldFunction.class));
    functionConfigs.add(functionConfiguration(BondModifiedDurationFromYieldFunction.class));
    functionConfigs.add(functionConfiguration(BondZSpreadFromCurveCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(BondZSpreadFromMarketCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(BondZSpreadPresentValueSensitivityFromCurveCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(BondZSpreadPresentValueSensitivityFromMarketCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(NelsonSiegelSvenssonBondCurveFunction.class));
    functionConfigs.add(functionConfiguration(BondDefaultCurveNamesFunction.class, PriorityClass.ABOVE_NORMAL.name(),
        "USD", "Discounting", "DefaultTwoCurveUSDConfig", "Discounting", "DefaultTwoCurveUSDConfig",
        "EUR", "Discounting", "DefaultTwoCurveEURConfig", "Discounting", "DefaultTwoCurveEURConfig",
        "GBP", "Discounting", "DefaultTwoCurveGBPConfig", "Discounting", "DefaultTwoCurveGBPConfig"));
  }

  private static void addForexOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(ExampleForexSpotRateMarketDataFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackCurrencyExposureFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaMatrixFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaQuoteMatrixFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackPresentValueCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackYCNSFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackDefaultsDeprecated.class, SECONDARY, SECONDARY, PAR_RATE_STRING, SECONDARY,
        SECONDARY, PAR_RATE_STRING, SECONDARY, "DoubleQuadratic", "LinearExtrapolator", "LinearExtrapolator", "USD", "EUR"));
    functionConfigs.add(functionConfiguration(FXOptionBlackDefaultsDeprecated.class, SECONDARY, SECONDARY, PAR_RATE_STRING, SECONDARY,
        SECONDARY, PAR_RATE_STRING, SECONDARY, "DoubleQuadratic", "LinearExtrapolator", "LinearExtrapolator", "EUR", "USD"));
  }

  private static void addForexForwardCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(FXForwardPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXForwardCurrencyExposureFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXForwardYCNSFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXForwardPresentValueCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "USD", "EUR"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "EUR", "USD"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "USD", "GBP"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "GBP", "USD"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "USD", "JPY"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "JPY", "USD"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "USD", "CHF"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "CHF", "USD"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "EUR", "GBP"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "GBP", "EUR"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "EUR", "JPY"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "JPY", "EUR"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "EUR", "CHF"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "GBP", "CHF"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "JPY", "CHF"));
    functionConfigs.add(functionConfiguration(FXForwardDefaultsDeprecated.class, "SECONDARY", "SECONDARY", PAR_RATE_STRING, "SECONDARY", "SECONDARY",
        PAR_RATE_STRING, "CHF", "JPY"));
  }

  private static void addInterestRateFutureCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateFuturePresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateFuturePV01FunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureYieldCurveNodeSensitivitiesFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureDefaultValuesFunctionDeprecated.class, SECONDARY, SECONDARY, PAR_RATE_STRING, USD, "EUR"));
  }

  private static void addInterestRateFutureOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRPresentValueFunction.class));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRSensitivitiesFunction.class, ValueRequirementNames.PRESENT_VALUE_SABR_ALPHA_SENSITIVITY));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRSensitivitiesFunction.class, ValueRequirementNames.PRESENT_VALUE_SABR_NU_SENSITIVITY));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRSensitivitiesFunction.class, ValueRequirementNames.PRESENT_VALUE_SABR_RHO_SENSITIVITY));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRVegaFunction.class));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionSABRYieldCurveNodeSensitivitiesFunction.class));
    //    functionConfigs.add(functionConfiguration(InterestRateFutureOptionDefaultValuesFunctionDeprecated.class, SECONDARY, SECONDARY, "DEFAULT", PAR_RATE_STRING, USD, "EUR"));
    //    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresIRFutureOptionSurfaceFittingFunction.class));
    //    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresIRFutureSurfaceDefaultValuesFunction.class, "DEFAULT"));
  }

  private static void addDeprecatedSABRCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingFunction.class));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, "USD", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunctionDeprecated.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunctionDeprecated.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunctionDeprecated.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationVegaFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationYCNSFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunctionDeprecated.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunctionDeprecated.Nu.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunctionDeprecated.Rho.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationVegaFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationYCNSFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunctionDeprecated.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunctionDeprecated.Nu.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunctionDeprecated.Rho.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationVegaFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationYCNSFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationDefaultsDeprecated.class, SECONDARY, SECONDARY, SECONDARY, "NonLinearLeastSquares", PAR_RATE_STRING, "USD"));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationDefaultsDeprecated.class, SECONDARY, SECONDARY, SECONDARY, "NonLinearLeastSquares", PAR_RATE_STRING, "0.07", "10.0", "USD"));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationVegaDefaultsDeprecated.class, SECONDARY, SECONDARY, SECONDARY, "NonLinearLeastSquares", PAR_RATE_STRING,
        "Linear", "FlatExtrapolator", "FlatExtrapolator", "Linear", "FlatExtrapolator", "FlatExtrapolator", USD));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationVegaDefaultsDeprecated.class, SECONDARY, SECONDARY, SECONDARY, "NonLinearLeastSquares", PAR_RATE_STRING,
        "0.07", "10.0", "Linear", "FlatExtrapolator", "FlatExtrapolator", "Linear", "FlatExtrapolator", "FlatExtrapolator", USD));
  }

  private static void addSABRCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingFunction.class));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, USD, SECONDARY));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationVegaFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationVegaFunction.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRNodeSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRNodeSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationPVSABRNodeSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationVegaFunction.class));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationDefaults.class, PriorityClass.ABOVE_NORMAL.name(), SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        USD, "DefaultTwoCurveUSDConfig", SECONDARY));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationDefaults.class, PriorityClass.ABOVE_NORMAL.name(), SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        "0.07", "10.0",
        USD, "DefaultTwoCurveUSDConfig", SECONDARY));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationVegaDefaults.class, PriorityClass.ABOVE_NORMAL.name(), SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR,
        USD, "DefaultTwoCurveUSDConfig", SECONDARY));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationVegaDefaults.class, PriorityClass.ABOVE_NORMAL.name(), SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        "0.07", "10.0", LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR,
        USD, "DefaultTwoCurveUSDConfig", SECONDARY));

  }

  private static void addDeprecatedFixedIncomeInstrumentCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPresentValueFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateParallelCurveSensitivityFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPV01FunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentYieldCurveNodeSensitivitiesFunctionDeprecated.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentDefaultCurveNameFunctionDeprecated.class, "ParRate", SECONDARY, SECONDARY, "AUD", "CAD", "CHF", "DKK", "EUR",
        "GBP", "JPY", "NZD", USD));
  }

  private static void addFixedIncomeInstrumentCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateParallelCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPV01Function.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentDefaultPropertiesFunction.class, PriorityClass.ABOVE_NORMAL.name(), "false",
        "USD", "DefaultTwoCurveUSDConfig",
        "GBP", "DefaultTwoCurveGBPConfig",
        "EUR", "DefaultTwoCurveEURConfig",
        "JPY", "DefaultTwoCurveJPYConfig",
        "CHF", "DefaultTwoCurveCHFConfig"));
  }

  private static void addPortfolioAnalysisCalculators(final List<FunctionConfiguration> functionConfigs) {
    final String returnCalculator = TimeSeriesReturnCalculatorFactory.SIMPLE_NET_STRICT;
    final String samplingPeriod = "P2Y";
    final String schedule = ScheduleCalculatorFactory.DAILY;
    final String samplingFunction = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    final String stdDevCalculator = StatisticsCalculatorFactory.SAMPLE_STANDARD_DEVIATION;
    final String covarianceCalculator = StatisticsCalculatorFactory.SAMPLE_COVARIANCE;
    final String varianceCalculator = StatisticsCalculatorFactory.SAMPLE_VARIANCE;
    final String excessReturnCalculator = StatisticsCalculatorFactory.MEAN;

    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(CAPMBetaModelPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(CAPMBetaModelPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionModelFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SharpeRatioDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator));
    functionConfigs.add(functionConfiguration(SharpeRatioDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator));
    functionConfigs.add(functionConfiguration(SharpeRatioPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SharpeRatioPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TreynorRatioDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(TreynorRatioDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(TreynorRatioPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TreynorRatioPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(JensenAlphaDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(JensenAlphaDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator, covarianceCalculator, varianceCalculator));
    functionConfigs.add(functionConfiguration(JensenAlphaFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaDefaultPropertiesPortfolioNodeFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaDefaultPropertiesPositionFunction.class, samplingPeriod, schedule, samplingFunction,
        returnCalculator, stdDevCalculator, excessReturnCalculator));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
  }

  private static void addExternallyProvidedSensitivitiesFunctions(final List<FunctionConfiguration> functionConfigs) {
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingCalculatorName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesNonYieldCurveFunction.class));

    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesCreditFactorsFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivityPnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivityPnLDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSecurityMarkFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurvePV01Function.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesDefaultPropertiesFunction.class, PriorityClass.ABOVE_NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig",
        "GBP", "DefaultTwoCurveGBPConfig",
        "EUR", "DefaultTwoCurveEURConfig",
        "JPY", "DefaultTwoCurveJPYConfig",
        "CHF", "DefaultTwoCurveCHFConfig"));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurveCS01Function.class));
  }

  //-------------------------------------------------------------------------
  public static RepositoryConfigurationSource constructRepositoryConfigurationSource() {
    return new SimpleRepositoryConfigurationSource(constructRepositoryConfiguration());
  }

  @Override
  protected RepositoryConfigurationSource createObject() {
    return constructRepositoryConfigurationSource();
  }

}
