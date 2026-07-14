package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.KandidatForPurringRepositoryImpl
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class KandidatForPurringRepositoryTest {
    private val clockTreUkerOgEnDagFremITid = fixedClock(LocalDate.now().plusWeeks(3).plusDays(1))

    companion object {
        private lateinit var dataSource: TestDataSource

        @BeforeAll
        @JvmStatic
        fun setup() {
            dataSource = TestDataSource()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() = dataSource.close()
    }

    @Test
    fun `skal plukke ut behandlinger som har ventet i akkurat tre uker`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling1.id,
                status = Status.UTREDES
            )

            // avklaringsbehov opprettes i dag og behandling skal bli kandidat for purring om tre uker og en dag
            val avklaringsbehov1 = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling1.id)
            avklaringsbehov1.leggTil(
                Definisjon.BESTILL_LEGEERKLÆRING,
                StegType.AVKLAR_SYKDOM,
                null,
                null,
                frist = LocalDate.now(clockTreUkerOgEnDagFremITid).plusDays(14)
            )

            val kandidaterTreUkerFraIDag = KandidatForPurringRepositoryImpl(connection).finnKandidaterForPurring(LocalDate.now(clockTreUkerOgEnDagFremITid))
            assertThat(kandidaterTreUkerFraIDag).hasSize(1)
            assertThat(kandidaterTreUkerFraIDag.first()).isEqualTo(behandling1.referanse)


            val clockToUkerFremITid = fixedClock(LocalDate.now().plusWeeks(2))
            val kandidaterToUkerFraIDag = KandidatForPurringRepositoryImpl(connection).finnKandidaterForPurring(LocalDate.now(clockToUkerFremITid))
            assertThat(kandidaterToUkerFraIDag).isEmpty()

            val clockFireUkerFremITid = fixedClock(LocalDate.now().plusWeeks(4))
            val kandidaterFireUkerFraIDag = KandidatForPurringRepositoryImpl(connection).finnKandidaterForPurring(LocalDate.now(clockFireUkerFremITid))
            assertThat(kandidaterFireUkerFraIDag).isEmpty()
        }
    }

    @Test
    fun `skal ikke plukke behandlinger som ikke lenger er på vent`() {
        dataSource.transaction { connection ->
            val sak1 = sak(connection)
            val behandling1 = finnEllerOpprettBehandling(connection, sak1)
            BehandlingRepositoryImpl(connection).oppdaterBehandlingStatus(
                behandlingId = behandling1.id,
                status = Status.UTREDES
            )

            // avklaringsbehov opprettes og lukkes i dag
            val avklaringsbehov1 = AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling1.id)
            avklaringsbehov1.leggTil(
                Definisjon.BESTILL_LEGEERKLÆRING,
                StegType.AVKLAR_SYKDOM,
                null,
                null,
                frist = LocalDate.now(clockTreUkerOgEnDagFremITid).plusDays(14)
            )
            avklaringsbehov1.løsAvklaringsbehov(
                definisjon = Definisjon.BESTILL_LEGEERKLÆRING,
                begrunnelse = "tatt av vent manuelt",
                endretAv = "saksbehandler",
            )

            val kandidaterTreUkerFraIDag = KandidatForPurringRepositoryImpl(connection).finnKandidaterForPurring(LocalDate.now(clockTreUkerOgEnDagFremITid))
            assertThat(kandidaterTreUkerFraIDag).isEmpty()
        }

    }
}