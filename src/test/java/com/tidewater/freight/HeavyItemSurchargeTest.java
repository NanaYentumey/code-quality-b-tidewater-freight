package com.tidewater.freight;

import static com.tidewater.freight.MoneyAssertions.assertAmount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HeavyItemSurchargeTest {

  private static final BigDecimal THIRTY_KG = new BigDecimal("30");
  private static final BigDecimal TWENTY = new BigDecimal("20.00");

  @ParameterizedTest(name = "{0} kg pays a surcharge of {1}")
  @CsvSource({"29.99, 0", "30, 0", "30.01, 20.00", "40, 20.00"})
  void chargesOnlyShipmentsHeavierThanTheThreshold(String weightKg, String expectedSurcharge) {
    SurchargePolicy policy = new HeavyItemSurcharge(THIRTY_KG, TWENTY);

    assertAmount(expectedSurcharge, policy.surchargeFor(shipmentWeighing(weightKg)));
  }

  @Test
  void treatsEveryShipmentAsHeavyWhenTheThresholdIsZero() {
    SurchargePolicy policy = new HeavyItemSurcharge(BigDecimal.ZERO, TWENTY);

    assertAmount("20.00", policy.surchargeFor(shipmentWeighing("0.01")));
  }

  @Test
  void chargesNothingWhenTheSurchargeIsZero() {
    SurchargePolicy policy = new HeavyItemSurcharge(THIRTY_KG, BigDecimal.ZERO);

    assertAmount("0", policy.surchargeFor(shipmentWeighing("40")));
  }

  @Test
  void rejectsAMissingThreshold() {
    InvalidPricingException failure =
        assertThrows(InvalidPricingException.class, () -> new HeavyItemSurcharge(null, TWENTY));

    assertEquals("Heavy-item threshold must not be negative, but was null", failure.getMessage());
  }

  @Test
  void rejectsANegativeThreshold() {
    InvalidPricingException failure =
        assertThrows(
            InvalidPricingException.class,
            () -> new HeavyItemSurcharge(new BigDecimal("-1"), TWENTY));

    assertEquals("Heavy-item threshold must not be negative, but was -1", failure.getMessage());
  }

  @Test
  void rejectsAMissingSurcharge() {
    InvalidPricingException failure =
        assertThrows(InvalidPricingException.class, () -> new HeavyItemSurcharge(THIRTY_KG, null));

    assertEquals("Heavy-item surcharge must not be negative, but was null", failure.getMessage());
  }

  @Test
  void rejectsANegativeSurcharge() {
    InvalidPricingException failure =
        assertThrows(
            InvalidPricingException.class,
            () -> new HeavyItemSurcharge(THIRTY_KG, new BigDecimal("-0.01")));

    assertEquals("Heavy-item surcharge must not be negative, but was -0.01", failure.getMessage());
  }

  private static Shipment shipmentWeighing(String weightKg) {
    return new Shipment("DOMESTIC", new BigDecimal(weightKg), ServiceLevel.STANDARD);
  }
}
