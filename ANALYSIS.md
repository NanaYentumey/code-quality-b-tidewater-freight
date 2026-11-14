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
