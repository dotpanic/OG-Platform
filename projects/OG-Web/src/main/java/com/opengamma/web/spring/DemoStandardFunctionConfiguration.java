/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.spring;

import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.DOUBLE_QUADRATIC;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.FLAT_EXTRAPOLATOR;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.LINEAR;
import static com.opengamma.analytics.math.interpolation.Interpolator1DFactory.LINEAR_EXTRAPOLATOR;
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
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
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
import com.opengamma.financial.analytics.CurrencyPairsDefaults;
import com.opengamma.financial.analytics.MissingInputsFunction;
import com.opengamma.financial.analytics.cashflow.FixedPayCashFlowFunction;
import com.opengamma.financial.analytics.cashflow.FixedReceiveCashFlowFunction;
import com.opengamma.financial.analytics.cashflow.FloatingPayCashFlowFunction;
import com.opengamma.financial.analytics.cashflow.FloatingReceiveCashFlowFunction;
import com.opengamma.financial.analytics.cashflow.NettedFixedCashFlowFunction;
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
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackDeltaFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackFromFuturePresentValueFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackGammaFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackPV01Function;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackVegaFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionBlackYCNSFunction;
import com.opengamma.financial.analytics.model.bondfutureoption.BondFutureOptionDefaults;
import com.opengamma.financial.analytics.model.cds.ISDAApproxCDSPriceFlatSpreadFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxCDSPriceHazardCurveFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxDiscountCurveFunction;
import com.opengamma.financial.analytics.model.cds.ISDAApproxFlatSpreadFunction;
import com.opengamma.financial.analytics.model.credit.ISDALegacyCDSHazardCurveDefaults;
import com.opengamma.financial.analytics.model.credit.ISDALegacyCDSHazardCurveFunction;
import com.opengamma.financial.analytics.model.credit.ISDALegacyVanillaCDSCleanPriceFunction;
import com.opengamma.financial.analytics.model.credit.ISDALegacyVanillaCDSDefaults;
import com.opengamma.financial.analytics.model.credit.ISDALegacyVanillaCDSDirtyPriceFunction;
import com.opengamma.financial.analytics.model.credit.ISDAYieldCurveDefaults;
import com.opengamma.financial.analytics.model.credit.ISDAYieldCurveFunction;
import com.opengamma.financial.analytics.model.curve.forward.FXForwardCurvePrimitiveDefaults;
import com.opengamma.financial.analytics.model.curve.forward.FXForwardCurveSecurityDefaults;
import com.opengamma.financial.analytics.model.curve.forward.FXForwardCurveTradeDefaults;
import com.opengamma.financial.analytics.model.curve.forward.ForwardCurveValuePropertyNames;
import com.opengamma.financial.analytics.model.equity.AffineDividendFunction;
import com.opengamma.financial.analytics.model.equity.EquityForwardCurveDefaults;
import com.opengamma.financial.analytics.model.equity.EquityForwardCurveFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldForwardFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldFuturesYCNSFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldPV01FuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldPresentValueFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldPricingDefaults;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldSpotFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldValueDeltaFuturesFunction;
import com.opengamma.financial.analytics.model.equity.futures.EquityDividendYieldValueRhoFuturesFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBAWPresentValueFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackFundingCurveSensitivitiesFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackImpliedVolFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackRhoFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackSpotDeltaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackSpotGammaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackSpotVannaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackVegaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackVegaMatrixFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionBlackVommaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionDefaults;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionForwardValueFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityOptionSpotIndexFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionDefaults;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionForwardValueFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionFundingCurveSensitivitiesFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionPresentValueFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionRhoFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionSpotDeltaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionSpotGammaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionSpotIndexFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionSpotVannaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionVegaFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionVegaMatrixFunction;
import com.opengamma.financial.analytics.model.equity.option.EquityVanillaBarrierOptionVommaFunction;
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
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityForwardCalculationDefaults;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityForwardFromSpotAndYieldCurveFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapDefaults;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapPureImpliedVolPVFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapPureLocalVolPVFunction;
import com.opengamma.financial.analytics.model.equity.varianceswap.EquityVarianceSwapStaticReplicationDefaults;
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
import com.opengamma.financial.analytics.model.forex.BloombergFXSpotRateMarketDataFunction;
import com.opengamma.financial.analytics.model.forex.defaultproperties.FXForwardDefaults;
import com.opengamma.financial.analytics.model.forex.defaultproperties.FXOptionBlackCurveDefaults;
import com.opengamma.financial.analytics.model.forex.defaultproperties.FXOptionBlackSurfaceDefaults;
import com.opengamma.financial.analytics.model.forex.forward.FXForwardCurrencyExposureFunction;
import com.opengamma.financial.analytics.model.forex.forward.FXForwardFXPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.forward.FXForwardPV01Function;
import com.opengamma.financial.analytics.model.forex.forward.FXForwardPresentValueCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.forex.forward.FXForwardYCNSFunction;
import com.opengamma.financial.analytics.model.forex.option.BloombergFXOptionSpotRateFunction;
import com.opengamma.financial.analytics.model.forex.option.BloombergFXSpotRatePercentageChangeFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackVannaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackVegaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOneLookBarrierOptionBlackVommaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackCurrencyExposureFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackFXPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackGammaSpotFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackImpliedVolatilityFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackPV01Function;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackPresentValueCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackTermStructureCurrencyExposureFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackThetaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackVannaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackVegaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackVegaMatrixFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackVegaQuoteMatrixFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackVommaFunction;
import com.opengamma.financial.analytics.model.forex.option.black.FXOptionBlackYCNSFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEDualDeltaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEDualGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEForwardDeltaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEForwardGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEForwardVannaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEForwardVegaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEForwardVommaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridDualDeltaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridDualGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridForwardDeltaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridForwardGammaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridForwardVannaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridForwardVegaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridForwardVommaFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridImpliedVolatilityFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEGridPipsPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEImpliedVolatilityFunction;
import com.opengamma.financial.analytics.model.forex.option.localvol.FXOptionLocalVolatilityForwardPDEPipsPresentValueFunction;
import com.opengamma.financial.analytics.model.forex.option.vannavolga.FXOptionVannaVolgaPresentValueFunction;
import com.opengamma.financial.analytics.model.future.BondFutureGrossBasisFromCurvesFunction;
import com.opengamma.financial.analytics.model.future.BondFutureNetBasisFromCurvesFunction;
import com.opengamma.financial.analytics.model.future.InterestRateFutureDefaults;
import com.opengamma.financial.analytics.model.future.InterestRateFuturePV01Function;
import com.opengamma.financial.analytics.model.future.InterestRateFuturePresentValueFunction;
import com.opengamma.financial.analytics.model.future.InterestRateFutureYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketForwardFuturesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketPV01FuturesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketPresentValueFuturesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketSpotFuturesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketValueDeltaFuturesFunction;
import com.opengamma.financial.analytics.model.future.MarkToMarketValueRhoFuturesFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBAWGreeksFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBAWPVFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBjerksundStenslandGreeksFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackDefaults;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackDeltaFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackForwardDeltaFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackForwardGammaFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackGammaFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackPVFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackThetaFunction;
import com.opengamma.financial.analytics.model.futureoption.CommodityFutureOptionBlackVegaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackConstantSpreadThetaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackForwardSlideThetaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackVolatilitySurfaceConstantSpreadThetaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackVolatilitySurfaceForwardSlideThetaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackYieldCurvesConstantSpreadThetaFunction;
import com.opengamma.financial.analytics.model.horizon.FXOptionBlackYieldCurvesForwardSlideThetaFunction;
import com.opengamma.financial.analytics.model.horizon.InterestRateFutureOptionConstantSpreadThetaFunction;
import com.opengamma.financial.analytics.model.horizon.SwaptionBlackThetaDefaults;
import com.opengamma.financial.analytics.model.horizon.SwaptionConstantSpreadThetaFunction;
import com.opengamma.financial.analytics.model.irfutureoption.IRFutureOptionSABRDefaults;
import com.opengamma.financial.analytics.model.irfutureoption.IRFutureOptionSABRPresentValueFunction;
import com.opengamma.financial.analytics.model.irfutureoption.IRFutureOptionSABRSensitivitiesFunction;
import com.opengamma.financial.analytics.model.irfutureoption.IRFutureOptionSABRYCNSFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackDefaults;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackGammaFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackImpliedVolatilityFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackPV01Function;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackPriceFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackVolatilitySensitivityFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionBlackYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionHestonDefaults;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionHestonPresentValueFunction;
import com.opengamma.financial.analytics.model.irfutureoption.InterestRateFutureOptionMarketUnderlyingPriceFunction;
import com.opengamma.financial.analytics.model.option.AnalyticOptionDefaultCurveFunction;
import com.opengamma.financial.analytics.model.option.BlackScholesMertonModelFunction;
import com.opengamma.financial.analytics.model.option.BlackScholesModelCostOfCarryFunction;
import com.opengamma.financial.analytics.model.pnl.EquityPnLDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.pnl.EquityPnLFunction;
import com.opengamma.financial.analytics.model.pnl.FXForwardCurrencyExposurePnLCurrencyConversionFunction;
import com.opengamma.financial.analytics.model.pnl.FXForwardYCNSPnLCurrencyConversionFunction;
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
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadNoExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRCMSSpreadRightExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRNoExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVCurveSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVSABRNodeSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPVSABRSensitivityFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationPresentValueFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationVegaFunction;
import com.opengamma.financial.analytics.model.sabrcube.SABRRightExtrapolationYCNSFunction;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRNoExtrapolationVegaDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationDefaults;
import com.opengamma.financial.analytics.model.sabrcube.defaultproperties.SABRRightExtrapolationVegaDefaults;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSecurityMarkFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesCreditFactorsFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesNonYieldCurveFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurveCS01Function;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.sensitivities.ExternallyProvidedSensitivitiesYieldCurvePV01Function;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFuturePV01Function;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFuturePresentValueFunction;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFuturePriceDeltaFunction;
import com.opengamma.financial.analytics.model.simpleinstrument.SimpleFutureRhoFunction;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackImpliedVolatilityFunction;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackPV01Function;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackPresentValueFunction;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackVolatilitySensitivityFunction;
import com.opengamma.financial.analytics.model.swaption.black.SwaptionBlackYieldCurveNodeSensitivitiesFunction;
import com.opengamma.financial.analytics.model.var.NormalHistoricalVaRDefaultPropertiesFunction;
import com.opengamma.financial.analytics.model.var.NormalHistoricalVaRFunction;
import com.opengamma.financial.analytics.model.volatility.SmileFittingProperties;
import com.opengamma.financial.analytics.model.volatility.cube.SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults;
import com.opengamma.financial.analytics.model.volatility.cube.SABRNonLinearLeastSquaresSwaptionCubeFittingFunction;
import com.opengamma.financial.analytics.model.volatility.local.EquityDupireLocalVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.local.ForexDupireLocalVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.local.defaultproperties.BackwardPDEDefaults;
import com.opengamma.financial.analytics.model.volatility.local.defaultproperties.FXPDECurveDefaults;
import com.opengamma.financial.analytics.model.volatility.local.defaultproperties.ForwardPDEDefaults;
import com.opengamma.financial.analytics.model.volatility.local.defaultproperties.LocalVolatilitySurfaceDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.BlackScholesMertonImpliedVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.surface.HestonFourierIRFutureSurfaceFittingFunction;
import com.opengamma.financial.analytics.model.volatility.surface.SABRIRFutureOptionNLSSDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.SABRNonLinearLeastSquaresIRFutureOptionSurfaceFittingFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.BlackVolatilitySurfaceMixedLogNormalInterpolatorFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.BlackVolatilitySurfacePropertyNamesAndValues;
import com.opengamma.financial.analytics.model.volatility.surface.black.BlackVolatilitySurfaceSABRInterpolatorFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.BlackVolatilitySurfaceSplineInterpolatorFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.CommodityBlackVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.EquityBlackVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.ForexBlackVolatilitySurfaceFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.BlackVolatilitySurfaceMixedLogNormalDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.BlackVolatilitySurfaceSABRDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.BlackVolatilitySurfaceSplineDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.CommodityBlackVolatilitySurfacePrimitiveDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.CommodityBlackVolatilitySurfaceSecurityDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.CommodityBlackVolatilitySurfaceTradeDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.EquityBlackVolatilitySurfacePrimitiveDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.EquityBlackVolatilitySurfaceSecurityDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.EquityBlackVolatilitySurfaceTradeDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.FXBlackVolatilitySurfacePrimitiveDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.FXBlackVolatilitySurfaceSecurityDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.FXBlackVolatilitySurfaceTradeDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.defaultproperties.PureBlackVolatilitySurfaceDefaults;
import com.opengamma.financial.analytics.model.volatility.surface.black.pure.PureBlackVolatilitySurfaceDividendCorrectionFunction;
import com.opengamma.financial.analytics.model.volatility.surface.black.pure.PureBlackVolatilitySurfaceNoDividendCorrectionFunction;
import com.opengamma.financial.analytics.volatility.surface.DefaultVolatilitySurfaceShiftFunction;
import com.opengamma.financial.analytics.volatility.surface.VolatilitySurfaceShiftFunction;
import com.opengamma.financial.currency.BondFutureOptionBlackPnLSeriesCurrencyConversionFunction;
import com.opengamma.financial.currency.CurrencyConversionFunction;
import com.opengamma.financial.currency.CurrencyMatrixConfigPopulator;
import com.opengamma.financial.currency.CurrencyMatrixSourcingFunction;
import com.opengamma.financial.currency.CurrencyPairs;
import com.opengamma.financial.currency.DefaultCurrencyFunction;
import com.opengamma.financial.currency.FXOptionBlackPnLSeriesCurrencyConversionFunction;
import com.opengamma.financial.currency.FixedIncomeInstrumentPnLSeriesCurrencyConversionFunction;
import com.opengamma.financial.currency.YCNSPnLSeriesCurrencyConversionFunction;
import com.opengamma.financial.property.AggregationDefaultPropertyFunction;
import com.opengamma.financial.property.AttributableDefaultPropertyFunction;
import com.opengamma.financial.property.CalcConfigDefaultPropertyFunction;
import com.opengamma.financial.property.DefaultPropertyFunction.PriorityClass;
import com.opengamma.financial.property.PositionDefaultPropertyFunction;
import com.opengamma.financial.value.ForwardPriceRenamingFunction;
import com.opengamma.financial.value.ValueFunction;
import com.opengamma.util.SingletonFactoryBean;
import com.opengamma.util.fudgemsg.OpenGammaFudgeContext;
import com.opengamma.web.spring.defaults.EquityInstrumentDefaultValues;
import com.opengamma.web.spring.defaults.GeneralBlackVolatilityInterpolationDefaults;
import com.opengamma.web.spring.defaults.GeneralLocalVolatilitySurfaceDefaults;
import com.opengamma.web.spring.defaults.TargetSpecificBlackVolatilitySurfaceDefaults;

/**
 * Constructs a standard function repository.
 * <p>
 * This should be replaced by something that loads the functions from the configuration database
 */
public class DemoStandardFunctionConfiguration extends SingletonFactoryBean<RepositoryConfigurationSource> {

  // TODO: Change to inherit from AbstractRepositoryConfigurationBean

  private static final boolean OUTPUT_REPO_CONFIGURATION = false;

  public static <F extends FunctionDefinition> FunctionConfiguration functionConfiguration(final Class<F> clazz, final String... args) {
    if (Modifier.isAbstract(clazz.getModifiers())) {
      throw new IllegalStateException("Attempting to register an abstract class - " + clazz);
    }
    if (args.length == 0) {
      return new StaticFunctionConfiguration(clazz.getName());
    }
    return new ParameterizedFunctionConfiguration(clazz.getName(), Arrays.asList(args));
  }

  protected static void addValueFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(ValueFunction.class));
    functionConfigs.add(functionConfiguration(ForwardPriceRenamingFunction.class));
  }

  protected static void addCurrencyConversionFunctions(final List<FunctionConfiguration> functionConfigs, final String requirementName) {
    functionConfigs.add(functionConfiguration(CurrencyConversionFunction.class, requirementName));
    functionConfigs.add(functionConfiguration(DefaultCurrencyFunction.Permissive.class, requirementName));
  }

  protected static void addCurrencyConversionFunctions(final List<FunctionConfiguration> functionConfigs) {
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.FAIR_VALUE);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.PV01);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.DV01);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.PRESENT_VALUE);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.DAILY_PNL);
    //TODO PRESENT_VALUE_CURVE_SENSITIVITY
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_DELTA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_GAMMA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_VEGA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_THETA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_SPEED);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_VOMMA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_VANNA);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_RHO);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.VALUE_PHI);
    addCurrencyConversionFunctions(functionConfigs, ValueRequirementNames.YIELD_CURVE_NODE_SENSITIVITIES);

    functionConfigs.add(functionConfiguration(CurrencyPairsDefaults.class, CurrencyPairs.DEFAULT_CURRENCY_PAIRS));
    functionConfigs.add(functionConfiguration(CurrencyMatrixSourcingFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(CurrencyMatrixSourcingFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FixedIncomeInstrumentPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FixedIncomeInstrumentPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(YCNSPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(YCNSPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXForwardCurrencyExposurePnLCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXForwardCurrencyExposurePnLCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXForwardYCNSPnLCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXForwardYCNSPnLCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXOptionBlackPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(FXOptionBlackPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.BLOOMBERG_LIVE_DATA));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackPnLSeriesCurrencyConversionFunction.class, CurrencyMatrixConfigPopulator.SYNTHETIC_LIVE_DATA));
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
    functionConfigs.add(functionConfiguration(AggregationDefaultPropertyFunction.class, ValueRequirementNames.DAILY_PNL, MissingInputsFunction.AGGREGATION_STYLE_FULL));
    functionConfigs.add(functionConfiguration(StandardEquityModelFunction.class));

    addPnLCalculators(functionConfigs);
    addVaRCalculators(functionConfigs);
    addPortfolioAnalysisCalculators(functionConfigs);
    addFixedIncomeInstrumentCalculators(functionConfigs);
    addEquityDividendYieldFuturesFunctions(functionConfigs);
    addEquityForwardFunctions(functionConfigs);
    addBondCalculators(functionConfigs);
    addBondFutureCalculators(functionConfigs);
    addSABRCalculators(functionConfigs);
    addForexOptionCalculators(functionConfigs);
    addFXBarrierOptionCalculators(functionConfigs);
    addForexForwardCalculators(functionConfigs);
    addInterestRateFutureCalculators(functionConfigs);
    addGeneralFutureCalculators(functionConfigs);
    addInterestRateFutureOptionCalculators(functionConfigs);
    addBondFutureOptionBlackCalculators(functionConfigs);
    addCommodityFutureOptionCalculators(functionConfigs);
    addEquityOptionCalculators(functionConfigs);
    addEquityBarrierOptionCalculators(functionConfigs);
    addEquityVarianceSwapCalculators(functionConfigs);
    addBlackCalculators(functionConfigs);
    addLocalVolatilityPDEFunctions(functionConfigs);
    addLocalVolatilityPDEGridFunctions(functionConfigs);
    addEquityPureVolatilitySurfaceCalculators(functionConfigs);
    addCDSCalculators(functionConfigs);

    addBlackVolatilitySurface(functionConfigs);
    addCommodityBlackVolatilitySurface(functionConfigs);
    addFXOptionBlackVolatilitySurface(functionConfigs);
    addEquityIndexOptionBlackVolatilitySurface(functionConfigs);
    addLocalVolatilitySurface(functionConfigs);

    addExternallyProvidedSensitivitiesFunctions(functionConfigs);
    addCashFlowFunctions(functionConfigs);
    addCurrencyConversionFunctions(functionConfigs);
    addLateAggregationFunctions(functionConfigs);
    addDataShiftingFunctions(functionConfigs);
    addDefaultPropertyFunctions(functionConfigs);

    functionConfigs.add(functionConfiguration(AnalyticOptionDefaultCurveFunction.class, "FUNDING"));
    functionConfigs.add(functionConfiguration(AnalyticOptionDefaultCurveFunction.class, "SECONDARY"));

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
    final String defaultReturnCalculatorName = TimeSeriesReturnCalculatorFactory.SIMPLE_NET_LENIENT;
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingCalculatorName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    functionConfigs.add(functionConfiguration(TradeExchangeTradedPnLFunction.class, DEFAULT_CONFIG_NAME, "PX_LAST", "COST_OF_CARRY"));
    functionConfigs.add(functionConfiguration(TradeExchangeTradedDailyPnLFunction.class, DEFAULT_CONFIG_NAME, "PX_LAST", "COST_OF_CARRY"));
    functionConfigs.add(functionConfiguration(PositionExchangeTradedDailyPnLFunction.class, DEFAULT_CONFIG_NAME, "PX_LAST", "COST_OF_CARRY"));
    functionConfigs.add(functionConfiguration(SecurityPriceSeriesFunction.class, DEFAULT_CONFIG_NAME, MarketDataRequirementNames.MARKET_VALUE));
    functionConfigs.add(functionConfiguration(SecurityPriceSeriesDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(EquityPnLFunction.class));
    functionConfigs.add(functionConfiguration(EquityPnLDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName, defaultSamplingCalculatorName,
        defaultReturnCalculatorName));
    functionConfigs.add(functionConfiguration(SimpleFuturePnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SimpleFuturePnLDefaultPropertiesFunction.class, "FUNDING", defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(SimpleFXFuturePnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SimpleFXFuturePnLDefaultPropertiesFunction.class, "FUNDING", "FUNDING", defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingCalculatorName));
    functionConfigs.add(functionConfiguration(ValueGreekSensitivityPnLFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(ValueGreekSensitivityPnLDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingCalculatorName, defaultReturnCalculatorName));
  }

  private static void addVaRCalculators(final List<FunctionConfiguration> functionConfigs) {
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingCalculatorName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    final String defaultMeanCalculatorName = StatisticsCalculatorFactory.MEAN;
    final String defaultStdDevCalculatorName = StatisticsCalculatorFactory.SAMPLE_STANDARD_DEVIATION;
    final String defaultConfidenceLevelName = "0.99";
    final String defaultHorizonName = "1";

    //   functionConfigs.add(functionConfiguration(OptionPositionParametricVaRFunction.class, DEFAULT_CONFIG_NAME));
    //functionConfigs.add(functionConfiguration(OptionPortfolioParametricVaRFunction.class, DEFAULT_CONFIG_NAME, startDate, defaultReturnCalculatorName,
    //  defaultScheduleName, defaultSamplingCalculatorName, "0.99", "1", ValueRequirementNames.VALUE_DELTA));
    //functionConfigs.add(functionConfiguration(PositionValueGreekSensitivityPnLFunction.class, DEFAULT_CONFIG_NAME, startDate, defaultReturnCalculatorName,
    //  defaultScheduleName, defaultSamplingCalculatorName, ValueRequirementNames.VALUE_DELTA));
    functionConfigs.add(functionConfiguration(NormalHistoricalVaRFunction.class));
    functionConfigs.add(functionConfiguration(NormalHistoricalVaRDefaultPropertiesFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingCalculatorName, defaultMeanCalculatorName, defaultStdDevCalculatorName, defaultConfidenceLevelName, defaultHorizonName,
        PriorityClass.NORMAL.name()));
  }

  private static void addEquityOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(EquityOptionForwardValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionSpotIndexFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackImpliedVolFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackSpotDeltaFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackFundingCurveSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackVegaMatrixFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackVegaFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackSpotGammaFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackSpotVannaFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackVommaFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBlackRhoFunction.class));
    functionConfigs.add(functionConfiguration(EquityOptionBAWPresentValueFunction.class));
    final List<String> equityIndexOptionDefaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useForwardCurveCalculationConfigNames()
        .useForwardCurveNames()
        .useVolatilitySurfaceNames()
        .useInterpolationMethodNames()
        .createDefaults();
    final List<String> equityIndexOptionDefaultsWithPriority = new ArrayList<String>();
    equityIndexOptionDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityIndexOptionDefaultsWithPriority.addAll(equityIndexOptionDefaults);
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityOptionDefaults.class.getName(), equityIndexOptionDefaultsWithPriority));
  }

  private static void addEquityBarrierOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionSpotIndexFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionForwardValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionRhoFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionSpotDeltaFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionSpotGammaFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionSpotVannaFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionVegaFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionVegaMatrixFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionVommaFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionFundingCurveSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(EquityVanillaBarrierOptionDefaults.class, "0.0", "0.001"));
  }

  private static void addEquityForwardFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(EquityForwardFromSpotAndYieldCurveFunction.class));
    final List<String> equityForwardDefaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useDiscountingCurveNames()
        .useDiscountingCurveCalculationConfigNames()
        .createDefaults();
    final List<String> equityForwardDefaultsWithPriority = new ArrayList<String>();
    equityForwardDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityForwardDefaultsWithPriority.addAll(equityForwardDefaults);
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityForwardCalculationDefaults.class.getName(), equityForwardDefaultsWithPriority));
    final List<String> equityForwardCurveDefaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useDiscountingCurveNames()
        .useDiscountingCurveCalculationConfigNames()
        .useDiscountingCurveCurrency()
        .createDefaults();
    final List<String> equityForwardCurveDefaultsWithPriority = new ArrayList<String>();
    equityForwardCurveDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityForwardCurveDefaultsWithPriority.addAll(equityForwardCurveDefaults);
    functionConfigs.add(functionConfiguration(EquityForwardCurveFunction.class));
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityForwardCurveDefaults.class.getName(), equityForwardCurveDefaultsWithPriority));
  }

  private static void addEquityDividendYieldFuturesFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldForwardFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldPresentValueFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldPV01FuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldSpotFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldValueDeltaFuturesFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDividendYieldValueRhoFuturesFunction.class.getName()));
    functionConfigs.add(functionConfiguration(EquityDividendYieldFuturesYCNSFunction.class));
    final List<String> equityFutureDefaults = EquityInstrumentDefaultValues.builder()
        .useDiscountingCurveCurrency()
        .useDiscountingCurveCalculationConfigNames()
        .useDiscountingCurveNames()
        .createDefaults();
    final List<String> equityFutureDefaultsWithPriority = new ArrayList<String>();
    equityFutureDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityFutureDefaultsWithPriority.addAll(equityFutureDefaults);
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityDividendYieldPricingDefaults.class.getName(), equityFutureDefaultsWithPriority));
  }

  private static void addEquityVarianceSwapCalculators(final List<FunctionConfiguration> functionConfigs) {
    final List<String> equityVarianceSwapStaticReplicationDefaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useDiscountingCurveNames()
        .useDiscountingCurveCalculationConfigNames()
        .useVolatilitySurfaceNames()
        .createDefaults();
    final List<String> equityVarianceSwapStaticReplicationDefaultsWithPriority = new ArrayList<String>();
    equityVarianceSwapStaticReplicationDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityVarianceSwapStaticReplicationDefaultsWithPriority.addAll(equityVarianceSwapStaticReplicationDefaults);
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapStaticReplicationVegaFunction.class));
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityVarianceSwapStaticReplicationDefaults.class.getName(), equityVarianceSwapStaticReplicationDefaultsWithPriority));

    final List<String> equityVarianceSwapDefaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useDiscountingCurveNames()
        .useForwardCurveNames()
        .useForwardCurveCalculationConfigNames()
        .useForwardCurveCalculationMethodNames()
        .useDiscountingCurveCurrency()
        .useVolatilitySurfaceNames()
        .createDefaults();
    final List<String> equityVarianceSwapDefaultsWithPriority = new ArrayList<String>();
    equityVarianceSwapDefaultsWithPriority.add(PriorityClass.NORMAL.name());
    equityVarianceSwapDefaultsWithPriority.addAll(equityVarianceSwapDefaults);
    functionConfigs.add(functionConfiguration(EquityVarianceSwapPureImpliedVolPVFunction.class));
    functionConfigs.add(functionConfiguration(EquityVarianceSwapPureLocalVolPVFunction.class));
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityVarianceSwapDefaults.class.getName(), equityVarianceSwapDefaultsWithPriority));
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
    functionConfigs.add(functionConfiguration(BloombergFXSpotRateMarketDataFunction.class));
    functionConfigs.add(functionConfiguration(BloombergFXSpotRatePercentageChangeFunction.class));
    functionConfigs.add(functionConfiguration(BloombergFXOptionSpotRateFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackFXPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackCurrencyExposureFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaMatrixFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVegaQuoteMatrixFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackGammaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackGammaSpotFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVannaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVommaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackPresentValueCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackPV01Function.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackImpliedVolatilityFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackTermStructureCurrencyExposureFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackConstantSpreadThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVolatilitySurfaceConstantSpreadThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackYieldCurvesConstantSpreadThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackForwardSlideThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackVolatilitySurfaceForwardSlideThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackYieldCurvesForwardSlideThetaFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackYCNSFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionVannaVolgaPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(FXOptionBlackCurveDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig", "Discounting",
        "EUR", "DefaultTwoCurveEURConfig", "Discounting",
        "CAD", "DefaultTwoCurveCADConfig", "Discounting",
        "AUD", "DefaultTwoCurveAUDConfig", "Discounting",
        "CHF", "DefaultTwoCurveCHFConfig", "Discounting",
        "MXN", "DefaultCashCurveMXNConfig", "Cash",
        "JPY", "DefaultTwoCurveJPYConfig", "Discounting",
        "GBP", "DefaultTwoCurveGBPConfig", "Discounting",
        "NZD", "DefaultTwoCurveNZDConfig", "Discounting",
        "HUF", "DefaultCashCurveHUFConfig", "Cash",
        "KRW", "DefaultCashCurveKRWConfig", "Cash",
        "BRL", "DefaultCashCurveBRLConfig", "Cash",
        "HKD", "DefaultCashCurveHKDConfig", "Cash"));
    functionConfigs.add(functionConfiguration(FXOptionBlackSurfaceDefaults.class, PriorityClass.NORMAL.name(), DOUBLE_QUADRATIC, LINEAR_EXTRAPOLATOR,
        LINEAR_EXTRAPOLATOR,
        "USD", "EUR", "TULLETT",
        "USD", "CAD", "TULLETT",
        "USD", "AUD", "TULLETT",
        "USD", "CHF", "TULLETT",
        "USD", "MXN", "TULLETT",
        "USD", "JPY", "TULLETT",
        "USD", "GBP", "TULLETT",
        "USD", "NZD", "TULLETT",
        "USD", "HUF", "TULLETT",
        "USD", "KRW", "TULLETT",
        "USD", "BRL", "TULLETT",
        "EUR", "CHF", "TULLETT",
        "USD", "HKD", "TULLETT",
        "EUR", "JPY", "TULLETT"));
  }

  private static void addFXBarrierOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackGammaFunction.class));
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackVannaFunction.class));
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackVegaFunction.class));
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackVommaFunction.class));
    final String overhedge = "0.0";
    final String relativeStrikeSmoothing = "0.001";
    functionConfigs.add(functionConfiguration(FXOneLookBarrierOptionBlackDefaultPropertiesFunction.class, overhedge, relativeStrikeSmoothing));
  }

  private static void addForexForwardCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(FXForwardPV01Function.class));
    functionConfigs.add(functionConfiguration(FXForwardFXPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(FXForwardCurrencyExposureFunction.class));
    functionConfigs.add(functionConfiguration(FXForwardYCNSFunction.class));
    functionConfigs.add(functionConfiguration(FXForwardPresentValueCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(FXForwardDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig", "Discounting",
        "EUR", "DefaultTwoCurveEURConfig", "Discounting",
        "CHF", "DefaultTwoCurveCHFConfig", "Discounting",
        "RUB", "DefaultCashCurveRUBConfig", "Cash",
        "CAD", "DefaultTwoCurveCADConfig", "Discounting"));
  }

  private static void addGeneralFutureCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(SimpleFuturePresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SimpleFuturePriceDeltaFunction.class));
    functionConfigs.add(functionConfiguration(SimpleFuturePV01Function.class));
    functionConfigs.add(functionConfiguration(SimpleFutureRhoFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketForwardFuturesFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketPV01FuturesFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketPresentValueFuturesFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketSpotFuturesFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketValueDeltaFuturesFunction.class));
    functionConfigs.add(functionConfiguration(MarkToMarketValueRhoFuturesFunction.class));
  }

  private static void addInterestRateFutureCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateFuturePresentValueFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFuturePV01Function.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig",
        "EUR", "DefaultTwoCurveEURConfig",
        "CHF", "DefaultTwoCurveCHFConfig",
        "RUB", "DefaultCashCurveRUBConfig"));
  }


  private static void addInterestRateFutureOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionMarketUnderlyingPriceFunction.class));

    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackVolatilitySensitivityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackImpliedVolatilityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackPV01Function.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackGammaFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackPriceFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionConstantSpreadThetaFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionBlackDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig", "DEFAULT_PRICE"));

    functionConfigs.add(functionConfiguration(IRFutureOptionSABRPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(IRFutureOptionSABRSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(IRFutureOptionSABRYCNSFunction.class));
    functionConfigs.add(functionConfiguration(IRFutureOptionSABRDefaults.class, PriorityClass.ABOVE_NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig", "DEFAULT_PRICE", SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        "EUR", "DefaultTwoCurveEURConfig", "DEFAULT_PRICE", SmileFittingProperties.NON_LINEAR_LEAST_SQUARES));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresIRFutureOptionSurfaceFittingFunction.class));
    functionConfigs.add(functionConfiguration(SABRIRFutureOptionNLSSDefaults.class, PriorityClass.ABOVE_NORMAL.name(),
        LINEAR, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, "false", "true", "false", "false",
        "0.05", "1.0", "0.07", "0.0", "0.001"));

    functionConfigs.add(functionConfiguration(HestonFourierIRFutureSurfaceFittingFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionHestonPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateFutureOptionHestonDefaults.class,
        "USD", "DefaultTwoCurveUSDConfig", "DEFAULT_PRICE",
        "EUR", "DefaultTwoCurveEURConfig", "DEFAULT_PRICE"));
  }

  private static void addBondFutureOptionBlackCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackDeltaFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackGammaFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackPV01Function.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackYCNSFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackVegaFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionBlackFromFuturePresentValueFunction.class));
    functionConfigs.add(functionConfiguration(BondFutureOptionDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "DefaultTwoCurveUSDConfig", "BBG"));
  }

  private static void addCommodityFutureOptionCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackDeltaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackForwardDeltaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackForwardGammaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackGammaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackPVFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackThetaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBlackVegaFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBAWPVFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBAWGreeksFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityFutureOptionBjerksundStenslandGreeksFunction.class.getName()));

    functionConfigs.add(new ParameterizedFunctionConfiguration(CommodityFutureOptionBlackDefaults.class.getName(),
        Arrays.asList("USD", "Discounting", "DefaultTwoCurveUSDConfig", "BBG_S ", "Spline")));
  }

  private static void addLocalVolatilityPDEFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));

    functionConfigs.add(functionConfiguration(ForwardPDEDefaults.class,
        "0.5", "100", "100", "5.0", "0.05", "1.5", "1.0", Interpolator1DFactory.DOUBLE_QUADRATIC));
    functionConfigs.add(functionConfiguration(BackwardPDEDefaults.class,
        "0.5", "100", "100", "5.0", "0.05", "3.5", Interpolator1DFactory.DOUBLE_QUADRATIC));
    functionConfigs.add(functionConfiguration(FXPDECurveDefaults.class,
        "USD", "Discounting", "DefaultTwoCurveUSDConfig",
        "EUR", "Discounting", "DefaultTwoCurveEURConfig"));
  }

  private static void addLocalVolatilityPDEGridFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridDualGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardDeltaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardGammaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVegaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVannaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridForwardVommaFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridImpliedVolatilityFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SPLINE)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.SABR)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXOptionLocalVolatilityForwardPDEGridPipsPresentValueFunction.class.getName(), Arrays
        .asList(BlackVolatilitySurfacePropertyNamesAndValues.MIXED_LOG_NORMAL)));
  }

  //TODO move next few methods into the surface configuration class
  private static void addBlackVolatilitySurface(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(BlackVolatilitySurfaceSABRInterpolatorFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(BlackVolatilitySurfaceMixedLogNormalInterpolatorFunction.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(BlackVolatilitySurfaceSplineInterpolatorFunction.Exception.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(BlackVolatilitySurfaceSplineInterpolatorFunction.Quiet.class.getName()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(BlackVolatilitySurfaceSABRDefaults.class.getName(),
        GeneralBlackVolatilityInterpolationDefaults.getSABRInterpolationDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(BlackVolatilitySurfaceMixedLogNormalDefaults.class.getName(),
        GeneralBlackVolatilityInterpolationDefaults.getMixedLogNormalInterpolationDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(BlackVolatilitySurfaceSplineDefaults.class.getName(),
        GeneralBlackVolatilityInterpolationDefaults.getSplineInterpolationDefaults()));
  }

  private static void addFXOptionBlackVolatilitySurface(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(ForexBlackVolatilitySurfaceFunction.MixedLogNormal.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(ForexBlackVolatilitySurfaceFunction.SABR.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(ForexBlackVolatilitySurfaceFunction.Spline.class.getName()));

    functionConfigs.add(new ParameterizedFunctionConfiguration(FXBlackVolatilitySurfacePrimitiveDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllFXDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXBlackVolatilitySurfaceSecurityDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllFXDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXBlackVolatilitySurfaceTradeDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllFXDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXForwardCurvePrimitiveDefaults.class.getName(),
        Arrays.asList("EURUSD", "DiscountingImplied", ForwardCurveValuePropertyNames.PROPERTY_YIELD_CURVE_IMPLIED_METHOD)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXForwardCurveSecurityDefaults.class.getName(),
        Arrays.asList("EURUSD", "DiscountingImplied", ForwardCurveValuePropertyNames.PROPERTY_YIELD_CURVE_IMPLIED_METHOD)));
    functionConfigs.add(new ParameterizedFunctionConfiguration(FXForwardCurveTradeDefaults.class.getName(),
        Arrays.asList("EURUSD", "DiscountingImplied", ForwardCurveValuePropertyNames.PROPERTY_YIELD_CURVE_IMPLIED_METHOD)));
  }

  private static void addEquityIndexOptionBlackVolatilitySurface(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(EquityBlackVolatilitySurfaceFunction.SABR.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityBlackVolatilitySurfaceFunction.Spline.class.getName()));

    final List<String> defaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useForwardCurveNames()
        .useForwardCurveCalculationMethodNames()
        .useDiscountingCurveCurrency()
        .useForwardCurveCalculationConfigNames()
        .useVolatilitySurfaceNames()
        .createDefaults();
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityBlackVolatilitySurfacePrimitiveDefaults.class.getName(), defaults));
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityBlackVolatilitySurfaceSecurityDefaults.class.getName(), defaults));
    functionConfigs.add(new ParameterizedFunctionConfiguration(EquityBlackVolatilitySurfaceTradeDefaults.class.getName(), defaults));
  }

  private static void addCommodityBlackVolatilitySurface(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(CommodityBlackVolatilitySurfaceFunction.SABR.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(CommodityBlackVolatilitySurfaceFunction.Spline.class.getName()));

    functionConfigs.add(new ParameterizedFunctionConfiguration(CommodityBlackVolatilitySurfacePrimitiveDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllCommodityDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(CommodityBlackVolatilitySurfaceSecurityDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllCommodityDefaults()));
    functionConfigs.add(new ParameterizedFunctionConfiguration(CommodityBlackVolatilitySurfaceTradeDefaults.class.getName(),
        TargetSpecificBlackVolatilitySurfaceDefaults.getAllCommodityDefaults()));
  }

  private static void addLocalVolatilitySurface(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(ForexDupireLocalVolatilitySurfaceFunction.MixedLogNormal.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(ForexDupireLocalVolatilitySurfaceFunction.SABR.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(ForexDupireLocalVolatilitySurfaceFunction.Spline.class.getName()));

    functionConfigs.add(new StaticFunctionConfiguration(EquityDupireLocalVolatilitySurfaceFunction.MixedLogNormal.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDupireLocalVolatilitySurfaceFunction.SABR.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(EquityDupireLocalVolatilitySurfaceFunction.Spline.class.getName()));

    functionConfigs.add(new ParameterizedFunctionConfiguration(LocalVolatilitySurfaceDefaults.class.getName(),
        GeneralLocalVolatilitySurfaceDefaults.getLocalVolatilitySurfaceDefaults()));
  }

  private static void addEquityPureVolatilitySurfaceCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(new StaticFunctionConfiguration(PureBlackVolatilitySurfaceNoDividendCorrectionFunction.Spline.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(PureBlackVolatilitySurfaceDividendCorrectionFunction.Spline.class.getName()));
    functionConfigs.add(new StaticFunctionConfiguration(AffineDividendFunction.class.getName()));

    final List<String> defaults = EquityInstrumentDefaultValues.builder()
        .useEquityName()
        .useDiscountingCurveNames()
        .useDiscountingCurveCurrency()
        .useDiscountingCurveCalculationConfigNames()
        .useVolatilitySurfaceNames()
        .createDefaults();
    functionConfigs.add(new ParameterizedFunctionConfiguration(PureBlackVolatilitySurfaceDefaults.class.getName(), defaults));
  }

  private static void addSABRCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingFunction.class));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, "USD", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, "EUR", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, "GBP", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRNonLinearLeastSquaresSwaptionCubeFittingDefaults.class, "AUD", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationVegaFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadNoExtrapolationYCNSFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Alpha.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Nu.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationPVSABRNodeSensitivityFunction.Rho.class));
    functionConfigs.add(functionConfiguration(SABRCMSSpreadRightExtrapolationVegaFunction.class));
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
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationDefaults.class, PriorityClass.BELOW_NORMAL.name(),
        SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
        "USD", "DefaultTwoCurveUSDConfig", "BLOOMBERG",
        "EUR", "DefaultTwoCurveEURConfig", "BLOOMBERG",
        "AUD", "DefaultTwoCurveAUDConfig", "BLOOMBERG",
        "GBP", "DefaultTwoCurveGBPConfig", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRRightExtrapolationDefaults.class, PriorityClass.BELOW_NORMAL.name(),
        SmileFittingProperties.NON_LINEAR_LEAST_SQUARES, "0.07", "10.0",
        "USD", "DefaultTwoCurveUSDConfig", "BLOOMBERG",
        "EUR", "DefaultTwoCurveEURConfig", "BLOOMBERG",
        "AUD", "DefaultTwoCurveAUDConfig", "BLOOMBERG",
        "GBP", "DefaultTwoCurveGBPConfig", "BLOOMBERG"));
    functionConfigs.add(functionConfiguration(SABRNoExtrapolationVegaDefaults.class, PriorityClass.BELOW_NORMAL.name(),
        SmileFittingProperties.NON_LINEAR_LEAST_SQUARES, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR,
        "USD", "DefaultTwoCurveUSDConfig", "BLOOMBERG",
        "EUR", "DefaultTwoCurveEURConfig", "BLOOMBERG",
        "AUD", "DefaultTwoCurveAUDConfig", "BLOOMBERG",
        "GBP", "DefaultTwoCurveGBPConfig", "BLOOMBERG"));
    functionConfigs
        .add(functionConfiguration(SABRRightExtrapolationVegaDefaults.class, PriorityClass.BELOW_NORMAL.name(), SmileFittingProperties.NON_LINEAR_LEAST_SQUARES,
            "0.07", "10.0", LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR, LINEAR, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR,
            "USD", "DefaultTwoCurveUSDConfig", "BLOOMBERG",
            "EUR", "DefaultTwoCurveEURConfig", "BLOOMBERG",
            "AUD", "DefaultTwoCurveAUDConfig", "BLOOMBERG",
            "GBP", "DefaultTwoCurveGBPConfig", "BLOOMBERG"));
  }

  private static void addBlackCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(SwaptionBlackPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(SwaptionBlackVolatilitySensitivityFunction.class));
    functionConfigs.add(functionConfiguration(SwaptionBlackPV01Function.class));
    functionConfigs.add(functionConfiguration(SwaptionBlackYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(SwaptionBlackImpliedVolatilityFunction.class));
    functionConfigs.add(functionConfiguration(SwaptionConstantSpreadThetaFunction.class));
    functionConfigs.add(functionConfiguration(SwaptionBlackDefaultPropertiesFunction.class, PriorityClass.NORMAL.name(), "EUR", "DefaultTwoCurveEURConfig", "DEFAULT"));
    functionConfigs.add(functionConfiguration(SwaptionBlackThetaDefaults.class, PriorityClass.NORMAL.name(), "1", "EUR", "DefaultTwoCurveEURConfig", "DEFAULT"));
  }

  private static void addFixedIncomeInstrumentCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentParRateParallelCurveSensitivityFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPresentValueFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentPV01Function.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(InterestRateInstrumentDefaultPropertiesFunction.class, PriorityClass.BELOW_NORMAL.name(), "false",
        "EUR", "DefaultTwoCurveEURConfig",
        "USD", "DefaultTwoCurveUSDConfig",
        "CHF", "DefaultTwoCurveCHFConfig",
        "JPY", "DefaultTwoCurveJPYConfig",
        "GBP", "DefaultTwoCurveGBPConfig"));
  }

  private static void addCDSCalculators(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(ISDAYieldCurveFunction.class));
    functionConfigs.add(functionConfiguration(ISDAYieldCurveDefaults.class, PriorityClass.NORMAL.name(),
        "USD", "0"));
    functionConfigs.add(functionConfiguration(ISDALegacyCDSHazardCurveFunction.class));
    functionConfigs.add(functionConfiguration(ISDALegacyCDSHazardCurveDefaults.class, PriorityClass.NORMAL.name(), "100", "1e-15", "0.5",
        "USD", "ISDA", "ISDA", "ISDA"));
    functionConfigs.add(functionConfiguration(ISDALegacyVanillaCDSCleanPriceFunction.class));
    functionConfigs.add(functionConfiguration(ISDALegacyVanillaCDSDirtyPriceFunction.class));
    functionConfigs.add(functionConfiguration(ISDALegacyVanillaCDSDefaults.class, PriorityClass.NORMAL.name(), "30"));
  }

  private static void addPortfolioAnalysisCalculators(final List<FunctionConfiguration> functionConfigs) {
    final String defaultReturnCalculatorName = TimeSeriesReturnCalculatorFactory.SIMPLE_NET_STRICT;
    final String defaultSamplingPeriodName = "P2Y";
    final String defaultScheduleName = ScheduleCalculatorFactory.DAILY;
    final String defaultSamplingFunctionName = TimeSeriesSamplingFunctionFactory.PREVIOUS_AND_FIRST_VALUE_PADDING;
    final String defaultStdDevCalculatorName = StatisticsCalculatorFactory.SAMPLE_STANDARD_DEVIATION;
    final String defaultCovarianceCalculatorName = StatisticsCalculatorFactory.SAMPLE_COVARIANCE;
    final String defaultVarianceCalculatorName = StatisticsCalculatorFactory.SAMPLE_VARIANCE;
    final String defaultExcessReturnCalculatorName = StatisticsCalculatorFactory.MEAN;

    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultCovarianceCalculatorName, defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(CAPMBetaDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultCovarianceCalculatorName, defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(CAPMBetaModelPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(CAPMBetaModelPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName));
    functionConfigs.add(functionConfiguration(CAPMFromRegressionModelFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SharpeRatioDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName));
    functionConfigs.add(functionConfiguration(SharpeRatioDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName));
    functionConfigs.add(functionConfiguration(SharpeRatioPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(SharpeRatioPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TreynorRatioDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName, defaultCovarianceCalculatorName,
        defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(TreynorRatioDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName, defaultCovarianceCalculatorName,
        defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(TreynorRatioPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TreynorRatioPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(JensenAlphaDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName, defaultCovarianceCalculatorName,
        defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(JensenAlphaDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName, defaultCovarianceCalculatorName,
        defaultVarianceCalculatorName));
    functionConfigs.add(functionConfiguration(JensenAlphaFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaDefaultPropertiesPortfolioNodeFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaDefaultPropertiesPositionFunction.class, defaultSamplingPeriodName, defaultScheduleName,
        defaultSamplingFunctionName, defaultReturnCalculatorName, defaultStdDevCalculatorName, defaultExcessReturnCalculatorName));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaPositionFunction.class, DEFAULT_CONFIG_NAME));
    functionConfigs.add(functionConfiguration(TotalRiskAlphaPortfolioNodeFunction.class, DEFAULT_CONFIG_NAME));
  }

  private static void addCashFlowFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(FixedPayCashFlowFunction.class));
    functionConfigs.add(functionConfiguration(FixedReceiveCashFlowFunction.class));
    functionConfigs.add(functionConfiguration(FloatingPayCashFlowFunction.class));
    functionConfigs.add(functionConfiguration(FloatingReceiveCashFlowFunction.class));
    functionConfigs.add(functionConfiguration(NettedFixedCashFlowFunction.class));
  }

  private static void addExternallyProvidedSensitivitiesFunctions(final List<FunctionConfiguration> functionConfigs) {
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurveNodeSensitivitiesFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesNonYieldCurveFunction.class));

    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesCreditFactorsFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSecurityMarkFunction.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurvePV01Function.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesYieldCurveCS01Function.class));
    functionConfigs.add(functionConfiguration(ExternallyProvidedSensitivitiesDefaultPropertiesFunction.class, PriorityClass.BELOW_NORMAL.name(),
        "EUR", "DefaultTwoCurveEURConfig",
        "USD", "DefaultTwoCurveUSDConfig",
        "CHF", "DefaultTwoCurveCHFConfig",
        "JPY", "DefaultTwoCurveJPYConfig",
        "GBP", "DefaultTwoCurveGBPConfig"));
  }

  public static RepositoryConfigurationSource constructRepositoryConfigurationSource() {
    return new SimpleRepositoryConfigurationSource(constructRepositoryConfiguration());
  }

  @Override
  protected RepositoryConfigurationSource createObject() {
    return constructRepositoryConfigurationSource();
  }

}
