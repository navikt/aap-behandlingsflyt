package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
            val referanse = BrevbestillingReferanse(UUID.randomUUID())

            assertThrows<NoSuchElementException> { brevbestillingRepository.hent(referanse) }
            assertThat(brevbestillingRepository.hent(behandlingId, TypeBrev.VEDTAK_INNVILGELSE)).isNull()

            brevbestillingRepository.lagre(behandlingId, TypeBrev.VEDTAK_INNVILGELSE, referanse, Status.SENDT)

            val brevbestilling = brevbestillingRepository.hent(referanse)
            assertThat(brevbestilling)
                .isEqualTo(
                    Brevbestilling(
                        brevbestilling.id,
                        behandlingId,
                        TypeBrev.VEDTAK_INNVILGELSE,
                        referanse,
                        Status.SENDT
                    )
                )
            assertThat(brevbestillingRepository.hent(behandlingId, TypeBrev.VEDTAK_INNVILGELSE))
                .isEqualTo(brevbestilling)
            assertThat(brevbestillingRepository.hent(behandlingId, TypeBrev.VEDTAK_AVSLAG)).isNull()

            brevbestillingRepository.oppdaterStatus(behandlingId, referanse, Status.FORHÅNDSVISNING_KLAR)

            assertThat(brevbestillingRepository.hent(referanse).status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
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
