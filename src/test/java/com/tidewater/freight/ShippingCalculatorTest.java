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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The calculator on its own. The rate source is a mock and the surcharge policy is a one-line
 * stand-in, so every expected cost below follows from what the same test supplies and from the
 * calculator's own rules: add the parts, apply the service level, round.
 */
@ExtendWith(MockitoExtension.class)
class ShippingCalculatorTest {

  private static final ZonePricing FIVE_PER_KG_NO_FEE = PerKilogramPricing.of("5.00", "0.00");
  private static final SurchargePolicy NO_SURCHARGE = shipment -> BigDecimal.ZERO;
  private static final SurchargePolicy TWENTY_SURCHARGE = shipment -> new BigDecimal("20.00");

  @Mock private RateRepository rates;

  private ShippingCalculator calculator;

  @BeforeEach
  void createCalculator() {
    calculator = new ShippingCalculator(rates, NO_SURCHARGE);
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

  @Test
  void addsTheSurchargeThePolicyGives() {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);
    ShippingCalculator withSurcharge = new ShippingCalculator(rates, TWENTY_SURCHARGE);

    BigDecimal cost = withSurcharge.calculateCost(standard("DOMESTIC", "10"));

    assertEquals(new BigDecimal("70.00"), cost);
  }

  @Test
  void expressMultipliesTheFeeAndSurchargeAsWellAsTheRate() {
    when(rates.findPricing("INTERNATIONAL")).thenReturn(PerKilogramPricing.of("15.00", "25.00"));
    ShippingCalculator withSurcharge = new ShippingCalculator(rates, TWENTY_SURCHARGE);
    Shipment heavyExpress =
        new Shipment("INTERNATIONAL", new BigDecimal("40"), ServiceLevel.EXPRESS);

    BigDecimal cost = withSurcharge.calculateCost(heavyExpress);

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
  void pricesWithAZoneRuleThatIsNotPerKilogram() {
    BigDecimal minimumCharge = new BigDecimal("50.00");
    ZonePricing threePerKgWithAMinimum =
        weightKg -> new BigDecimal("3.00").multiply(weightKg).max(minimumCharge);
    when(rates.findPricing("ISLAND")).thenReturn(threePerKgWithAMinimum);

    assertAll(
        () -> assertEquals(minimumCharge, calculator.calculateCost(standard("ISLAND", "12"))),
        () ->
            assertEquals(
                new BigDecimal("60.00"), calculator.calculateCost(standard("ISLAND", "20"))));
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
  void refusesToQuoteANegativeSurchargeFromAFaultyPolicy() {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);
    ShippingCalculator withFaultyPolicy =
        new ShippingCalculator(rates, shipment -> new BigDecimal("-0.01"));

    RateLookupException failure =
        assertThrows(
            RateLookupException.class,
            () -> withFaultyPolicy.calculateCost(standard("DOMESTIC", "10")));

    assertEquals("Surcharge policy gave an invalid surcharge: -0.01", failure.getMessage());
  }

  @Test
  void refusesToQuoteWhenThePolicyGivesNoSurcharge() {
    when(rates.findPricing("DOMESTIC")).thenReturn(FIVE_PER_KG_NO_FEE);
    ShippingCalculator withFaultyPolicy = new ShippingCalculator(rates, shipment -> null);

    RateLookupException failure =
        assertThrows(
            RateLookupException.class,
            () -> withFaultyPolicy.calculateCost(standard("DOMESTIC", "10")));

    assertEquals("Surcharge policy gave an invalid surcharge: null", failure.getMessage());
  }

  @Test
  void rejectsAMissingShipment() {
    assertThrows(NullPointerException.class, () -> calculator.calculateCost(null));
  }

  @Test
  void rejectsAMissingRateSource() {
    assertThrows(NullPointerException.class, () -> new ShippingCalculator(null, NO_SURCHARGE));
  }

  @Test
  void rejectsAMissingSurchargePolicy() {
    assertThrows(NullPointerException.class, () -> new ShippingCalculator(rates, null));
  }

  private static Shipment standard(String zone, String weightKg) {
    return new Shipment(zone, new BigDecimal(weightKg), ServiceLevel.STANDARD);
  }
}
