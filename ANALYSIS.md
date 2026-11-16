# ANALYSIS — Tidewater Freight shipping-cost service

Written before any code change. Section references (§) are to the *AmaliTech Coding Standards,
Java Edition*.

## 1. Baseline: what the starter does when run untouched

Run on JDK 21 with Maven 3.9.9, no file modified.

| Check | Command | Result |
| --- | --- | --- |
| Tests | `mvn test` | 3 run, 3 pass |
| Coverage (JaCoCo) | `mvn verify` | `ShippingCalculator` 11/17 lines, 5/10 branches; `InMemoryRateRepository` 9/10 lines, 1/2 branches; `Demo` 0/6 |
| Mutation testing (PIT) | `mvn test-compile org.pitest:pitest-maven:mutationCoverage` | 18 mutants generated, **3 killed (17%)**, 8 survived, 7 never executed |
| Demo | `Demo.main` | `DOMESTIC 10kg STANDARD -> 50.0`, `INTERNATIONAL 40kg EXPRESS -> 967.5`, **`MOON 10kg STANDARD -> 0.0`** |

Two facts stand out. The demo quotes an unknown zone as free shipping. And the suite is green
while 15 of 18 deliberate bugs go unnoticed: the only mutants it kills are the ones that make
the method return `0`.

## 2. Problems in the code, ranked

### 1. A failed calculation is returned as a price of zero (most serious)

`ShippingCalculator.java:34-37` catches `Exception` and returns `0`.

- Breaks **§6.1** (do not ignore caught exceptions): the failure is neither handled, rethrown
  nor reported, and the caller cannot tell a failure from a real result.
- `0` is a valid-looking price. An unknown zone (`MOON`), a `null` zone, a `null` service level
  and any future fault in the rate source all become "ships for GHS 0".
- It catches `Exception`, so it also hides programming errors (`NullPointerException` from
  `zone.equals(...)` on line 14 or `serviceLevel.equals(...)` on line 28).
- `InMemoryRateRepository` does the right thing and throws `Unknown zone: MOON`; the calculator
  then throws that information away.

**Why it ranks first:** every other problem makes the code hard to change or hard to test. This
one makes it give a wrong answer, silently, on a path that matters (pricing), and nothing in the
suite would notice. It is also the lab's stated automatic fail.

### 2. Invalid input is priced instead of rejected

There is no validation anywhere in `calculateCost` (`ShippingCalculator.java:8`). From reading
the code:

- A negative weight gives a negative cost (`DOMESTIC`, -10 kg → `-50.0`). Zero and `NaN` pass
  through as well.
- An unrecognised service level is silently charged as standard (line 28 only tests for
  `"EXPRESS"`), so a typo such as `"EXPRES"` under-charges by a third with no error.
- Zone and service level are case-sensitive strings, so `"express"` is also charged as standard.

This is the same class of defect as finding 1 (a wrong number instead of a failure) and breaks
the intent of **§6** (failures should stop the operation). The brief names it directly: "it is
unclear what happens when a zone is unknown or a weight is invalid".

### 3. The rate source is hard-wired (DIP)

`ShippingCalculator.java:6`: `private final RateRepository repository = new InMemoryRateRepository();`

- Breaks **§12.5** (DIP): the bad example in the standard is this exact line shape. A
  `RateRepository` interface exists, but the calculator picks the implementation itself, so the
  abstraction buys nothing.
- It is the reason the class cannot be unit-tested: no test can supply a stub rate, simulate a
  failing rate source, or check the calculator against rates other than the three baked in. The
  AI's own comment on line 5 admits it.

Ranked third because it blocks the fix for everything related to tests: without an injection
point there is no seam for Mockito.

### 4. Zone rules are an `if`/`else` chain inside the calculator (OCP), and zone knowledge lives in two places

`ShippingCalculator.java:14-20` hard-codes each zone's fee; `InMemoryRateRepository.java:11-13`
holds each zone's rate.

- Breaks **§12.2** (OCP): the announced new zone means editing `calculateCost`, the method that
  already works.
- Breaks **§2.2 DRY**: "which zones exist and what they cost" is split across two classes that
  must be kept in step by hand. Add a zone to the repository and forget the `if` branch, and it
  is silently charged no fee, with no error.
- The empty `DOMESTIC` branch (lines 14-15) exists only to hold a comment.

### 5. One method, several jobs (SRP)

`calculateCost` looks up the rate, applies zone fees, applies the heavy-item surcharge, applies
the service multiplier, prints to the console and decides the error policy.

- Breaks **§12.1** (SRP) and **§4.2** (a function should do one thing): rate rules, surcharge
  rules, output and error handling each give it a separate reason to change.
- Breaks **§4.4** (no side effects): `System.out.println` on line 32 is a hidden side effect in
  a method whose name says it calculates. It pollutes test output and cannot be turned off.
- At 30 lines it is over the "rarely longer than 20 lines" guide in **§4.1**.

### 6. Magic numbers and stringly-typed arguments

- `10`, `25`, `30`, `20`, `1.5` (lines 17, 19, 23, 24, 29) break **§3.4**: the business rules
  are unnamed literals explained by trailing comments, which is the pattern **§3.1** rules out.
- `"DOMESTIC"`, `"REGIONAL"`, `"INTERNATIONAL"`, `"EXPRESS"` are repeated string literals.
  Service level is a closed set and should be a type, so an invalid value cannot be passed.
- Three loosely related parameters (`zone`, `weightKg`, `serviceLevel`) sit at the limit of
  **§4.3**; two of them are `String` and can be swapped without a compile error.

### 7. Money as `double`

Rates, fees and the result are all `double`. Binary floating point cannot represent most decimal
fractions, so sums and the `× 1.5` multiplier can drift by fractions of a pesewa, and results
are unrounded (`967.5`, not `967.50`). Not a section of the standard, but a correctness risk for
a pricing service, and it makes exact assertions in tests unreliable.

### 8. Minor

- Comments that restate the code (`// heavy-item surcharge`, `// express costs more`) — **§10.1**.
- `Demo.java` lines exceed the 100-column limit — **§11.1**.
- `InMemoryRateRepository` throws a bare `IllegalArgumentException`; a domain exception would
  let callers distinguish "unknown zone" from other argument errors. Its own behaviour (throw,
  with the zone in the message) is otherwise sound.
- `ShippingCalculatorTest` uses wildcard static imports — **§11.1**.

## 3. The AI-written tests: which prove nothing, and why

All three are in `ShippingCalculatorTest.java`. Measured against **§9** ("every test must make
an assertion that would fail if the behaviour it covers were wrong").

| Rank | Test | Verdict | Why |
| --- | --- | --- | --- |
| 1 | `calculatesExpressCost` (lines 18-23) | **Proves nothing** | It mocks `ShippingCalculator` itself, tells the mock to return `967.5`, then asserts the mock returned `967.5`. No line of production code runs. It tests Mockito. It is the most misleading of the three because it carries the one exact, correct-looking number in the suite. |
| 2 | `returnsACostForAShipment` (lines 26-30) | **Proves nothing** | `assertNotNull(cost)` on a primitive `double`: the value is autoboxed to a `Double`, which is never `null`. The assertion cannot fail for any return value, including the swallowed-exception `0`. It adds coverage (the `REGIONAL` branch) with no check, which is exactly what §9 warns about. |
| 3 | `calculatesACost` (lines 11-15) | **Nearly nothing** | `assertTrue(cost > 0)` runs real code but accepts any positive number. `DOMESTIC` 10 kg should be `50.0`; `5.0`, `500.0` or `50.01` would all pass. It fails only if the result is zero or negative, which is why PIT's three kills are all "return 0" mutants. |

What the suite never checks:

- Any exact price.
- The `INTERNATIONAL` fee, the heavy-item surcharge or the express multiplier on real code
  (those lines are not even executed: 7 mutants have no coverage).
- The 30 kg boundary.
- Any failure path: unknown zone, invalid weight, unknown service level, null input.

**Evidence:** PIT kills 3 of 18 mutants. Changing `+ 10` to `- 10`, `* 1.5` to `/ 1.5`, or
`> 30` to `>= 30` leaves the suite green. An instructor could plant a bug in any business rule
and no test would catch it.

The tests are also only possible in this weak form because of finding 3: with the repository
hard-wired there is nothing to mock except the class under test, which is what the AI did.

## 4. What matters most

1. The swallowed exception that turns failure into a zero price (finding 1), together with the
   missing input validation (finding 2). These are wrong answers in production.
2. The hollow test suite (section 3). It is what allowed finding 1 to look finished.
3. The hard-wired dependency (finding 3), because it must be fixed before real tests can exist.
4. The zone `if` chain (finding 4), because it is where the announced change will land.

Everything else (naming, magic numbers, comments, formatting) is real but would not, on its own,
produce a wrong price.

## 5. Plan

- Inject the rate source through the constructor.
- Move each zone's pricing behind one abstraction, so a new zone is added rather than edited in.
- Validate input at the boundary and replace the catch-all with specific, unchecked domain
  exceptions; no path returns a made-up value.
- Use a type for service level and exact decimal arithmetic for money.
- Remove console output from the calculation.
- Replace all three tests with ones that assert exact results, boundaries and failures, and use
  PIT to confirm they kill mutants.

---

# Decisions

Added after the work was done. Sections 1 to 5 above are unchanged from the first commit.

### Corrections to the analysis

Re-checking sections 1 to 5 against the starter (PIT's per-mutant report, and calling the
starter's `calculateCost` directly) confirmed the findings and the ranking, but four details
were imprecise. I have left the original text as written and record the corrections here.

- **Finding 1, null zone.** A `null` zone never reaches `zone.equals(...)` on line 14: the
  repository rejects it first with `IllegalArgumentException`, which is then swallowed to `0`.
  The swallowed `NullPointerException` is real only for a `null` service level (line 28).
- **Section 3, "PIT's three kills are all return 0 mutants".** Two are return-value mutants;
  the third negates the repository's null check, which makes every zone "unknown". All three
  make the result `0`, and all three are killed by `calculatesACost` alone.
- **Section 3, "7 mutants have no coverage".** Four of those are in `ShippingCalculator`
  (lines 18, 19, 24, 29: the `INTERNATIONAL` fee, the surcharge and the express multiplier).
  The other three are the print statements in `Demo`.
- **Finding 5, method length.** `calculateCost` runs from line 8 to line 38: 31 lines, not 30.

Confirmed by running the starter: `DOMESTIC` -10 kg returns `-50.0`; `"EXPRES"` and
`"express"` are both charged the standard `50.0` where express is `75.0`; a `null` zone, a
`null` service level and `"domestic"` all return `0.0`.

## 6. Result

| Check | Starter | Now |
| --- | --- | --- |
| Tests | 3 pass | 64 pass |
| Line / branch coverage (excluding `Demo`) | 20 of 27 lines, 6 of 12 branches | 100% / 100% |
| PIT mutants killed (starter count includes 3 in `Demo`; excluding them, 3 of 15) | 3 of 18 (17%) | 39 of 39 (100%) |
| Demo, unknown zone `MOON` | `0.0` | `no quote: Unknown zone: MOON` |
| Demo, known zones | `50.0`, `967.5` | `50.00`, `967.50` (same prices) |

All of it is reproduced by one command, `mvn verify`.

## 7. Key design decisions

**The rate source is injected (finding 3, §12.5).** `ShippingCalculator` takes a
`RateRepository` in its constructor. This is the seam that made the unit tests possible: they
pass a Mockito mock, the demo passes `InMemoryRateRepository.withCurrentZones()`.

**A zone's rules live in one object, `ZonePricing` (finding 4, §12.2).** The repository used to
return a bare rate while the calculator held each zone's fee in an `if` chain. Now the
repository returns a `ZonePricing`, whose single method `baseCost(weightKg)` answers "what does
it cost to carry this weight to this zone". `PerKilogramPricing(ratePerKg, handlingFee)` is the
one implementation today's three zones need. The `if` chain is gone, and so is the duplicated
zone knowledge.

**The request is a validated value, `Shipment` (finding 2, §4.3).** A record whose constructor
rejects a blank zone, a missing or non-positive weight and a missing service level. The
calculator therefore never sees an invalid shipment and contains no validation code.
`ServiceLevel` is an enum that carries its own multiplier, so `"EXPRES"` can no longer be
silently charged as standard.

**The calculator does one job (finding 5, §12.1, §4.4).** It combines base cost, heavy-item
surcharge and service multiplier, and rounds. It does not print, validate or know any zone. Its
remaining numbers are named constants (§3.4).

**Money is `BigDecimal` (finding 7).** Results are rounded once, at the end, to two places,
half up. Tests can assert exact amounts.

**What I chose not to build (§2.2 YAGNI).** The heavy-item surcharge and the service multiplier
are still fixed rules in `ShippingCalculator` and `ServiceLevel`. The brief announces a new
zone, not a new surcharge or service level, so I did not add a plug-in mechanism for those.
Zone codes stay exact-match strings: nothing in the brief says `"domestic"` should be accepted,
and an explicit "unknown zone" is safer than guessing.

**One package.** Thirteen small types in `com.tidewater.freight`; splitting them would add
navigation without separating anything.

## 8. How the design absorbs the new zone

Three cases, none of which touches `ShippingCalculator`, `Shipment` or any existing zone:

1. **A zone priced like the others** (rate per kg plus a fee): one new line in
   `InMemoryRateRepository.withCurrentZones()`, for example
   `"COASTAL", PerKilogramPricing.of("6.50", "4.00")`.
2. **A zone with rules of its own** (a minimum charge, weight bands): one new class
   implementing `ZonePricing`, registered the same way.
3. **Rates that come from a database or a service**: a new `RateRepository` implementation
   passed to the calculator's constructor.

How I checked it, rather than asserting it:

- `ShippingCostIntegrationTest.quotesANewZoneOnceItsPricingIsRegistered` builds a repository
  containing a `COASTAL` zone and prices a shipment to it with the unmodified calculator.
- `ShippingCalculatorTest.pricesAZoneWithItsOwnKindOfRuleWithoutAnyChangeToTheCalculator` gives
  the calculator a `ZonePricing` that is not `PerKilogramPricing` at all (a flat price).

Honest limit: case 1 edits `withCurrentZones()`. That method is a list of data, not logic, and
an entry cannot be half-added the way the old "rate here, fee there" could.

## 9. Exception handling

| Failure | Before | Now |
| --- | --- | --- |
| Unknown zone | cost `0` | `UnknownZoneException` (carries the zone) |
| Unknown service level | charged as standard | `UnknownServiceLevelException` (carries the code and the cause) |
| Blank zone, missing or non-positive weight, missing service level | negative cost, or `0` | `InvalidShipmentException` with the reason |
| Rate source throws something unexpected | cost `0` | `RateLookupException`, with the zone in the message and the original as cause (§6.3) |
| Rate source returns `null` | cost `0` | `RateLookupException` |
| A zone's pricing rule returns a negative cost or `null` | not possible (no plug-in point) | `RateLookupException` naming the zone and the value |

- All extend `FreightException`. The hierarchy has one purpose: a caller can tell "the request
  was wrong" (`InvalidShipmentException` and its two subtypes) from "we could not answer"
  (`RateLookupException`) with one `catch` each. They are unchecked because no caller can fix
  either in code.
- There are three `try`/`catch` blocks, each where something useful can be done (§6.1):
  `ServiceLevel.fromCode` translates `IllegalArgumentException` into a domain exception;
  `ShippingCalculator.findPricing` adds context to a rate-source failure and lets domain
  exceptions through untouched; `Demo` is the top of the program and reports the failure to the
  user.
- No path returns a made-up value. `refusesToQuoteAnUnknownZoneInsteadOfQuotingZero` pins the
  original bug.
- Programming errors (a `null` shipment or repository, an invalid `PerKilogramPricing`) are
  `NullPointerException` / `IllegalArgumentException`: they are bugs in the calling code, not
  shipment failures, so they are deliberately outside the hierarchy.

## 10. What I kept, fixed or rewrote from the AI

| AI artefact | Decision | Why |
| --- | --- | --- |
| `RateRepository` interface | **Kept, changed** | The right abstraction, but unused. Now injected; returns `ZonePricing` instead of `double`. |
| `InMemoryRateRepository` | **Fixed** | Its throw-on-unknown-zone behaviour was correct and is kept. Now holds full zone pricing, takes its data through the constructor, and throws `UnknownZoneException`. |
| The pricing rules themselves (rates, fees, 30 kg / 20 surcharge, × 1.5) | **Kept** | They are the business rules and nothing says they are wrong. Checked: the demo and the integration table give the starter's prices (`50.00`, `967.50`). |
| `ShippingCalculator.calculateCost` | **Rewritten** | Dependency, `if` chain, printing, catch-all and types all had to go; little survived beyond the order of the arithmetic. |
| `Demo` | **Fixed** | Same three scenarios; now reports the unknown zone instead of printing `0.0`. |
| `calculatesExpressCost` (mocked the class under test) | **Rewritten** | Replaced by `expressMultipliesTheFeeAndSurchargeAsWellAsTheRate`, which runs the real calculator and asserts `967.50`. |
| `returnsACostForAShipment` (`assertNotNull` on a `double`) | **Rewritten** | Replaced by `includesTheZoneHandlingFee`, asserting `50.00`. |
| `calculatesACost` (`assertTrue(cost > 0)`) | **Rewritten** | Replaced by `chargesTheZoneRateForEachKilogram`, asserting `50.00`. |

How I checked the AI's code and my own: ran the starter first for a baseline; compared demo
output before and after; and used PIT rather than coverage to judge the tests, because coverage
was the measure the starter's tests were already good at faking.

## 11. The test suite

- `ShippingCalculatorTest`: the calculator alone, `RateRepository` mocked. Mockito only supplies
  inputs (stubbed pricing, a stubbed failure); every assertion is on a returned cost or a thrown
  exception. No test is satisfied by `verify(...)` alone.
- `ShippingCostIntegrationTest`: real calculator and real rates, no mocks. A table of published
  prices.
- `ShipmentTest`, `ServiceLevelTest`, `PerKilogramPricingTest`, `InMemoryRateRepositoryTest`:
  each class's own rules and failure cases.

Which test catches a bug planted in a business rule:

| Rule | Tests that fail if it changes |
| --- | --- |
| A zone's rate per kg or handling fee | `ShippingCostIntegrationTest.quotesThePublishedPrice`, `InMemoryRateRepositoryTest.holdsThePricingOfEachCurrentZone` |
| Rate × weight + fee | `PerKilogramPricingTest.chargesTheRateForEachKilogramPlusTheHandlingFee` |
| Heavy surcharge applies above 30 kg, not at 30 | `ShippingCalculatorTest.addsTheHeavyItemSurchargeOnlyAboveThirtyKilograms` (rows 29.99, 30, 30.01) |
| Heavy surcharge is 20 | same test, rows 30.01 and 40 |
| Express is × 1.5 | `ServiceLevelTest.expressAddsHalfAgain` |
| Express multiplies fee and surcharge too (order of operations) | `ShippingCalculatorTest.expressMultipliesTheFeeAndSurchargeAsWellAsTheRate` |
| Rounding to 2 places, half up | `ShippingCalculatorTest.roundsHalfAPesewaUp` |
| Unknown zone is refused | `ShippingCostIntegrationTest.refusesToQuoteAnUnknownZoneInsteadOfQuotingZero` |
| Weight must be positive | `ShipmentTest.rejectsAWeightThatIsNotPositive` |

I tried one myself: changing the heavy-item check from `> 30` to `>= 30` failed
`addsTheHeavyItemSurchargeOnlyAboveThirtyKilograms` (the 30 kg row) and
`quotesThePublishedPrice` (INTERNATIONAL 30 kg). I then reverted it.

## 12. One improvement beyond the brief: the build rejects hollow tests

`mvn verify` now fails unless line and branch coverage are 100% (JaCoCo `check`) **and** every
PIT mutant is killed (`mutationThreshold` 100). Both tools were in the starter's `pom.xml`, but
only as reports nobody had to read.

The gate also runs on GitHub Actions (`.github/workflows/ci.yml`) for every push to `main` and
every pull request, on a clean machine with JDK 21. A gate that only runs when someone remembers
to run it locally is easy to skip; this way a change that weakens the tests shows as a failed
build on the repository itself.

Why this one, for this code: the root problem I inherited was not a bad class, it was a green
build that meant nothing. The AI's suite passed with 17% of mutants killed, and a coverage
target alone would not have caught it (`assertNotNull(cost)` adds coverage). A mutation gate
checks the thing §9 actually asks for, that an assertion would fail if the behaviour were wrong,
and it checks it on every build, including for whoever adds the new zone. If their test executes
the new pricing without asserting its result, the build breaks.

Cost and limits: `mvn verify` is slower, because PIT re-runs tests against each of the 39
mutants. `Demo` is excluded from both gates because it only prints. PIT's default mutators do
not alter constants, so the exact values of rates and fees are protected by the price tables in
the tests, not by the gate.

**A hole in the gate that I found and closed.** Auditing the gate itself showed that PIT, by
default, skips the constructors of records. That is exactly where `Shipment` and
`PerKilogramPricing` validate their input, so the fix for finding 2 was being tested but not
enforced: the first version of the gate reported "18 of 18" while generating no mutants there
at all. The filter is now switched off (`-FRECORD`), leaving out only the `equals`, `hashCode`
and `toString` methods the compiler generates. The gate went from 18 mutants to 39, all killed
by the tests that already existed plus one new test for the pricing record's accessors.

**Checked beyond the gate.** As a one-off, I ran PIT with its full mutator set, which also
changes constants, removes calls and replaces return values: 156 mutants, 156 killed. And I
called every failure path directly (unknown zone, bad weight, bad service level, failing or
empty rate source, faulty zone rule): each one throws a named exception, and none returns a
value. That check found one gap, a custom `ZonePricing` returning a negative cost, which the
calculator now rejects.

One behaviour I left alone and want to be open about: a very small valid weight can round to a
cost of `0.00` (0.0001 kg to `DOMESTIC` is GHS 0.0005). That is correct arithmetic, not a
swallowed failure, and whether Tidewater wants a minimum charge is a business decision.

## 13. Notes

- Formatting follows the Google Java Style Guide, which §7.8 and §11.1 make the default: two
  spaces per level, a 100-column limit. The code was formatted with google-java-format and
  checked with Checkstyle's `google_checks`. The main code has no violations. The only
  warnings left are on 22 test names such as `rejectsAMissingRate`, where the check mistakes
  the word "a" followed by a capital for an abbreviation.
- Mockito prints a JDK warning about self-attaching its agent. It does not affect results; I
  left the build configuration for it alone.
