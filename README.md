# Tidewater Freight — Shipping Cost Service

[![CI](https://github.com/NanaYentumey/code-quality-b-tidewater-freight/actions/workflows/ci.yml/badge.svg)](https://github.com/NanaYentumey/code-quality-b-tidewater-freight/actions/workflows/ci.yml)

Repository for the **Code Quality & Testing** applied lab.

## What this is

A small shipping-cost service. It was first generated quickly with an AI assistant, with tests
that passed without proving anything. This repository brings it up to AmaliTech standard.
[ANALYSIS.md](ANALYSIS.md) records what was wrong with the inherited code and tests (written
before any change) and, in its Decisions section, what was done about it and why.

## How it is put together

| Type | Job |
| --- | --- |
| `Shipment` | A validated request: zone, weight, service level |
| `ServiceLevel` | Standard or express, and its price multiplier |
| `ZonePricing` / `PerKilogramPricing` | The pricing rules of one zone |
| `RateRepository` / `InMemoryRateRepository` | Where zone pricing is looked up |
| `SurchargePolicy` / `HeavyItemSurcharge` | What a shipment pays on top of its zone's cost |
| `ShippingCalculator` | Adds zone cost and surcharge, applies the service level, rounds |
| `TidewaterPricing` | Today's zones, rates and surcharge, and the wiring of the parts |
| `FreightException` and subtypes | Every deliberate failure; nothing returns a made-up cost |

### Adding a zone

- **Priced like the others** (a rate per kg plus a fee): add one entry to
  `TidewaterPricing.currentZones()`.
- **Priced by a different kind of rule** (a minimum charge, weight bands): write a class that
  implements `ZonePricing`, then add it to `TidewaterPricing.currentZones()` the same way.

Either way `ShippingCalculator` does not change. A change to the surcharge rules is a new
`SurchargePolicy`, wired in `TidewaterPricing`.

## Running it

You need JDK 21 and Maven.

**See the service run (a small demo):**

```bash
mvn compile exec:java -Dexec.mainClass=com.tidewater.freight.Demo
```

**Run the tests:**

```bash
mvn test
```

**Run the full quality gate:**

```bash
mvn verify
```

`mvn verify` fails unless line and branch coverage are 100% and every PIT mutant is killed.
Reports: `target/site/jacoco/index.html` and `target/pit-reports/index.html`.

One class is left out of both checks on purpose: `Demo`, which only prints sample quotes and
holds no business rule. The exclusion is in `pom.xml`, next to each check.

The same command runs on GitHub Actions for every push to `main` and every pull request; the
badge at the top shows the result of the latest run.
