package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class BrevbestillingRepositoryTest {

    companion object {
        val dataSource = InitTestDatabase.dataSource
    }

    @Test
    fun `lagrer, henter og oppdaterer status`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepository(connection)

            val behandlingId = opprettBehandling(connection)
            val typeBrev = TypeBrev.VEDTAK_INNVILGELSE
            val referanse = BrevbestillingReferanse(UUID.randomUUID())

            assertThat(brevbestillingRepository.hent(behandlingId, typeBrev) ).isNull()
            assertThat(brevbestillingRepository.hent(behandlingId, typeBrev)).isNull()

            brevbestillingRepository.lagre(behandlingId, typeBrev, referanse, Status.SENDT)

            val brevbestilling = brevbestillingRepository.hent(behandlingId, typeBrev)
            assertThat(brevbestilling).isNotNull()
            assertThat(brevbestilling)
                .isEqualTo(
                    Brevbestilling(
                        brevbestilling!!.id,
                        behandlingId,
                        typeBrev,
                        referanse,
                        Status.SENDT
                    )
                )
            assertThat(brevbestillingRepository.hent(behandlingId, typeBrev))
                .isEqualTo(brevbestilling)
            assertThat(brevbestillingRepository.hent(behandlingId, TypeBrev.VEDTAK_AVSLAG)).isNull()

            brevbestillingRepository.oppdaterStatus(behandlingId, referanse, Status.FORHÅNDSVISNING_KLAR)

            assertThat(brevbestillingRepository.hent(behandlingId, typeBrev)?.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
        }
    }

    private fun opprettBehandling(connection: DBConnection): BehandlingId {
        val person = PersonRepositoryImpl(connection).finnEllerOpprett(listOf(Ident("ident", true)))
        val sakId = SakRepositoryImpl(connection).finnEllerOpprett(
            person,
            Periode(LocalDate.now(), LocalDate.now().plusDays(5))
        ).id
        return BehandlingRepositoryImpl(connection).opprettBehandling(
            sakId,
            listOf(),
            TypeBehandling.Førstegangsbehandling,
            null
        ).id
    }
}
