# Tidewater Freight — Shipping Cost Service

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
| `ShippingCalculator` | Combines zone cost, heavy-item surcharge and service level |
| `FreightException` and subtypes | Every deliberate failure; nothing returns a made-up cost |

To add a zone, register its pricing in `InMemoryRateRepository.withCurrentZones()`. The
calculator does not change.

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
