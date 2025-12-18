package no.nav.aap.behandlingsflyt.repository.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class BrevbestillingRepositoryImplTest {
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
    fun `lagrer, henter og oppdaterer status`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val sakId = opprettSak(connection)
            val behandlingId = opprettBehandling(sakId, connection)
            val typeBrev = TypeBrev.VEDTAK_INNVILGELSE
            val referanse = BrevbestillingReferanse(UUID.randomUUID())

            assertThat(brevbestillingRepository.hent(behandlingId)).isEmpty()

            brevbestillingRepository.lagre(behandlingId, typeBrev, referanse, Status.FORHÅNDSVISNING_KLAR)

            val brevbestilling = brevbestillingRepository.hent(behandlingId)
            assertThat(brevbestilling).hasSize(1)
            brevbestilling.first().let {
                assertThat(it.behandlingId).isEqualTo(behandlingId)
                assertThat(it.typeBrev).isEqualTo(typeBrev)
                assertThat(it.referanse).isEqualTo(referanse)
                assertThat(it.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
            }

            brevbestillingRepository.oppdaterStatus(
                behandlingId, referanse,
                Status.FULLFØRT
            )

            val oppdatertBrevbestilling = brevbestillingRepository.hent(behandlingId)
            assertThat(oppdatertBrevbestilling).hasSize(1)
            assertThat(oppdatertBrevbestilling.first().status)
                .isEqualTo(Status.FULLFØRT)
            assertThat(oppdatertBrevbestilling.first().typeBrev).isEqualTo(typeBrev)

            val behandling2 = opprettBehandling(sakId, connection)
            brevbestillingRepository.lagre(behandling2, typeBrev, BrevbestillingReferanse(UUID.randomUUID()), Status.FORHÅNDSVISNING_KLAR)

            val behandling3 = opprettBehandling(opprettSak(connection), connection) // annen sak
            brevbestillingRepository.lagre(behandling3, typeBrev, BrevbestillingReferanse(UUID.randomUUID()), Status.FORHÅNDSVISNING_KLAR)

            assertThat(brevbestillingRepository.hent(sakId, typeBrev)).hasSize(2)
        }
    }

    private fun opprettSak(connection: DBConnection): SakId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident()))
        return SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
    }

    private fun opprettBehandling(sakId: SakId, connection: DBConnection): BehandlingId {
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            TypeBehandling.Førstegangsbehandling,
            null,
            VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            )
        ).id
    }
}