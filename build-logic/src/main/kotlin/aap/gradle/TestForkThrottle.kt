package aap.gradle

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Tom build service som kun brukes som en semafor for å begrense hvor mange `Test`-tasks som kjører
 * samtidig på tvers av moduler.
 *
 * Bakgrunn: `org.gradle.parallel=true` gjør at `:api:test`, `:app:test`, `:behandlingsflyt:test` og
 * `:repository:test` kan kjøre samtidig, og hver av dem bruker `maxParallelForks = antall kjerner`.
 * På en 8-kjerners CI-runner ga det opptil ~32 test-JVM-er og et tilsvarende antall Testcontainers
 * (Postgres + Kafka) samtidig. Resultatet var CPU- og IO-svelt, som igjen slo ut som *timeouts* i
 * tidssensitive tester (Kafka-konsumenter, polling mot behandlingsstatus) — altså flakiness, ikke
 * funksjonelle feil.
 *
 * Ved å sette `maxParallelUsages = 1` kjører kun én `Test`-task om gangen, mens hver task fortsatt
 * får bruke alle kjernene internt. Total mengde arbeid er den samme, men vi unngår overbooking.
 */
abstract class TestForkThrottle : BuildService<BuildServiceParameters.None>
