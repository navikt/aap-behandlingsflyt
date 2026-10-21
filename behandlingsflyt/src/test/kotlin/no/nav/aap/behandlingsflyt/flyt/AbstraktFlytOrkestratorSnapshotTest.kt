package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbtest.DatabaseSnapshot
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.testutil.ManuellMotorImpl
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KClass

/**
 * Kjører oppsett (f.eks. en førstegangsbehandling) én gang, fryser databasetilstanden som
 * et PostgreSQL-snapshot, og gir hvert enkelt test-metode en fersk kopi av det snapshotet.
 *
 * Bruk:
 * ```kotlin
 * class MinFlytTest {
 *     @Nested
 *     inner class InnvilgetFørstegangsbehandling : AbstraktFlytOrkestratorSnapshotTest() {
 *         private lateinit var sak: Sak
 *         private lateinit var behandling: Behandling
 *
 *         @BeforeAll
 *         fun settOppFGB() = snapshotEtterSetup {
 *             val person = TestPersoner.STANDARD_PERSON()
 *             this.person = person
 *             val (sak, behandling) = sendInnFørsteSøknad(person = person)
 *             this@InnvilgetFørstegangsbehandling.sak = sak
 *             behandling.løsSykdom(fom)...fattVedtak().løsVedtaksbrev()
 *         }
 *
 *         @BeforeEach
 *         fun hentBehandlingFraSnapshot() {
 *             behandling = hentSisteOpprettedeBehandlingForSak(sak.id)
 *         }
 *
 *         @Test
 *         fun `revurdering test 1`() { ... }
 *
 *         @Test
 *         fun `revurdering test 2`() { ... }
 *     }
 * }
 * ```
 *
 * Hvert test-metode får sin egen uavhengige database som er en klon av tilstanden etter
 * [snapshotEtterSetup]-blokken. Tests kan derfor endre databasen uten å påvirke hverandre.
 * Uten et felles oppsett tas et snapshot av den tomme, migrerte databasen før første test,
 * slik at også tester med forskjellige grunnlag slipper å tømme tabellene mellom hver test.
 *
 * Flere felles grunnlag i samme flyttest grupperes i `@Nested inner class`-er som arver
 * denne klassen. Den ytre testklassen skal ikke arve orkestratortestklassene: bare de
 * nestede gruppene skal eie database- og snapshot-livssyklusen. Hver gruppe oppretter
 * sitt grunnlag i `@BeforeAll` og henter mutable behandlingsobjekter på nytt i `@BeforeEach`.
 *
 * Snapshotet isolerer databasen, ikke Kotlin-objekter. `PER_CLASS` beholder feltene mellom
 * testene, og [TestData.person] re-registrerer samme mutable person. Personendringer må
 * derfor nullstilles mellom tester, eller gjøres på nye testpersonobjekter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AbstraktFlytOrkestratorSnapshotTest(
    unleashGateway: KClass<out UnleashGateway> = AlleAvskruddUnleash::class,
) : AbstraktFlytOrkestratorTest(unleashGateway) {

    private lateinit var snapshot: DatabaseSnapshot
    private var motorForTest: ManuellMotorImpl? = null

    data class TestData(var person: TestPerson?)

    private var testData: TestData = TestData(person = null)

    /**
     * Kjør oppsettskoden i [block] (f.eks. fullfør en FGB), ta deretter et snapshot av
     * databasetilstanden. Kall denne i en `@BeforeAll`-metode, eller i
     * `@BeforeParameterizedClassInvocation` for en parameterisert testklasse.
     */
    protected fun snapshotEtterSetup(block: TestData.() -> Unit) {
        motorForTest = null
        resetGatewayProvider()
        if (::snapshot.isInitialized) {
            snapshot.close()
        }
        if (dataSource !is TestDataSource) {
            (dataSource as AutoCloseable).close()
            dataSource = TestDataSource()
        }
        testData = TestData(person = null)
        testData.block()
        snapshot = (dataSource as TestDataSource).createSnapshot()
    }

    /**
     * Før hvert test: bytt ut [dataSource] med en fersk klone av snapshotet, nullstill
     * motoren slik at den bruker den nye databasen, og re-registrer personen som var
     * aktiv under oppsett (siden FakesExtension.beforeEach() nullstiller FakePersoner).
     */
    @BeforeEach
    override fun beforeEachClearDatabase() {
        if (!::snapshot.isInitialized) {
            super.beforeEachClearDatabase()
            snapshotEtterSetup {}
        }
        motorForTest = null
        resetGatewayProvider()
        (dataSource as AutoCloseable).close()
        dataSource = snapshot.newDataSource()
        // Re-register person that was registered during setup
        if (testData.person != null) {
            FakePersoner.leggTil(testData.person!!)
        }
    }

    /**
     * Motor re-initialiseres lazy per test slik at den alltid bruker gjeldende [dataSource].
     */
    override val motor: ManuellMotorImpl
        get() = motorForTest ?: ManuellMotorImpl(
            dataSource,
            jobber = ProsesseringsJobber.alle(),
            repositoryRegistry = postgresRepositoryRegistry,
            gatewayProvider = gatewayProvider,
        ).also { motorForTest = it }

    @AfterAll
    fun lukkSnapshot() {
        if (::snapshot.isInitialized) {
            snapshot.close()
        }
    }
}
