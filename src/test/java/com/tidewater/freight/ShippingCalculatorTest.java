package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The calculator on its own. The rate source is a mock, so every expected cost below follows from
 * the pricing stubbed in the same test and from the calculator's own rules.
 */
@ExtendWith(MockitoExtension.class)
class ShippingCalculatorTest {

  private static final ZonePricing FIVE_PER_KG_NO_FEE = PerKilogramPricing.of("5.00", "0.00");

  @Mock private RateRepository rates;

  private ShippingCalculator calculator;

  @BeforeEach
  void createCalculator() {
    calculator = new ShippingCalculator(rates);
  }

  @Test
  void chargesTheZoneRateForEachKilogram() {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);

    BigDecimal cost = calculator.calculateCost(standard("DOMESTIC", "10"));

    assertEquals(new BigDecimal("50.00"), cost);
  }

  @Test
  void includesTheZoneHandlingFee() {
    when(rates.findPricing("REGIONAL")).thenReturn(PerKilogramPricing.of("8.00", "10.00"));

    BigDecimal cost = calculator.calculateCost(standard("REGIONAL", "5"));

    assertEquals(new BigDecimal("50.00"), cost);
  }

  @ParameterizedTest(name = "{0} kg costs {1}")
  @CsvSource({"29.99, 149.95", "30,    150.00", "30.01, 170.05", "40,    220.00"})
  void addsTheHeavyItemSurchargeOnlyAboveThirtyKilograms(String weightKg, String expectedCost) {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);

    BigDecimal cost = calculator.calculateCost(standard("DOMESTIC", weightKg));

    assertEquals(new BigDecimal(expectedCost), cost);
  }

  @Test
  void expressMultipliesTheFeeAndSurchargeAsWellAsTheRate() {
    when(rates.findPricing("INTERNATIONAL")).thenReturn(PerKilogramPricing.of("15.00", "25.00"));
    Shipment heavyExpress =
        new Shipment("INTERNATIONAL", new BigDecimal("40"), ServiceLevel.EXPRESS);

    BigDecimal cost = calculator.calculateCost(heavyExpress);

    assertEquals(new BigDecimal("967.50"), cost);
  }

  @Test
  void roundsHalfAPesewaUp() {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);
    Shipment shipment = new Shipment("DOMESTIC", new BigDecimal("1.11"), ServiceLevel.EXPRESS);

    BigDecimal cost = calculator.calculateCost(shipment);

    assertEquals(new BigDecimal("8.33"), cost);
  }

  @Test
  void pricesAZoneWithItsOwnKindOfRuleWithoutAnyChangeToTheCalculator() {
    ZonePricing flatNinetyNine = weightKg -> new BigDecimal("99.00");
    when(rates.findPricing("ISLAND")).thenReturn(flatNinetyNine);

    BigDecimal cost = calculator.calculateCost(standard("ISLAND", "12"));

    assertEquals(new BigDecimal("99.00"), cost);
  }

  @Test
  void letsAnUnknownZoneFailureReachTheCaller() {
    UnknownZoneException unknownZone = new UnknownZoneException("MOON");
    when(rates.findPricing("MOON")).thenThrow(unknownZone);

    UnknownZoneException failure =
        assertThrows(
            UnknownZoneException.class, () -> calculator.calculateCost(standard("MOON", "10")));

    assertSame(unknownZone, failure);
  }

  @Test
  void wrapsARateSourceFailureAndKeepsItsCause() {
    IllegalStateException connectionLost = new IllegalStateException("connection lost");
    when(rates.findPricing("DOMESTIC")).thenThrow(connectionLost);

    RateLookupException failure =
        assertThrows(
            RateLookupException.class, () -> calculator.calculateCost(standard("DOMESTIC", "10")));

    assertAll(
        () -> assertEquals("Could not look up pricing for zone DOMESTIC", failure.getMessage()),
        () -> assertSame(connectionLost, failure.getCause()));
  }

  @Test
  void refusesToPriceWhenTheRateSourceReturnsNothing() {
    when(rates.findPricing("DOMESTIC")).thenReturn(null);

    RateLookupException failure =
        assertThrows(
            RateLookupException.class, () -> calculator.calculateCost(standard("DOMESTIC", "10")));

    assertEquals("Rate source returned no pricing for zone DOMESTIC", failure.getMessage());
  }

  @Test
  void refusesToQuoteANegativeBaseCostFromAFaultyZoneRule() {
    ZonePricing faultyRule = weightKg -> new BigDecimal("-0.01");
    when(rates.findPricing("ISLAND")).thenReturn(faultyRule);

    RateLookupException failure =
        assertThrows(
            RateLookupException.class, () -> calculator.calculateCost(standard("ISLAND", "10")));

    assertEquals("Pricing for zone ISLAND gave an invalid base cost: -0.01", failure.getMessage());
  }

  @Test
  void refusesToQuoteWhenAZoneRuleGivesNoBaseCost() {
    ZonePricing faultyRule = weightKg -> null;
    when(rates.findPricing("ISLAND")).thenReturn(faultyRule);

    RateLookupException failure =
        assertThrows(
            RateLookupException.class, () -> calculator.calculateCost(standard("ISLAND", "10")));

    assertEquals("Pricing for zone ISLAND gave an invalid base cost: null", failure.getMessage());
  }

  @Test
  void acceptsAZoneRuleWhoseBaseCostIsZero() {
    ZonePricing freeZone = weightKg -> BigDecimal.ZERO;
    when(rates.findPricing("DEPOT")).thenReturn(freeZone);

    BigDecimal cost = calculator.calculateCost(standard("DEPOT", "10"));

    assertEquals(new BigDecimal("0.00"), cost);
  }

  @Test
  void rejectsAMissingShipment() {
    NullPointerException failure =
        assertThrows(NullPointerException.class, () -> calculator.calculateCost(null));

    assertEquals("shipment", failure.getMessage());
  }

  @Test
  void rejectsAMissingRateSource() {
    NullPointerException failure =
        assertThrows(NullPointerException.class, () -> new ShippingCalculator(null));

    assertEquals("rates", failure.getMessage());
  }

  private static Shipment standard(String zone, String weightKg) {
    return new Shipment(zone, new BigDecimal(weightKg), ServiceLevel.STANDARD);
  }
}
