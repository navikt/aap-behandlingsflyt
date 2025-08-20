package no.nav.aap.behandlingsflyt.repository.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class BrevbestillingRepositoryImplTest {

    companion object {
        private val dataSource = InitTestDatabase.freshDatabase()
    }

    @Test
    fun `lagrer, henter og oppdaterer status`() {
        dataSource.transaction { connection ->
            val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)

            val behandlingId = opprettBehandling(connection)
            val typeBrev = TypeBrev.VEDTAK_INNVILGELSE
            val referanse = BrevbestillingReferanse(UUID.randomUUID())

            Assertions.assertThat(brevbestillingRepository.hent(behandlingId)).isEmpty()

            brevbestillingRepository.lagre(behandlingId, typeBrev, referanse, Status.SENDT)

            val brevbestilling = brevbestillingRepository.hent(behandlingId)
            Assertions.assertThat(brevbestilling).hasSize(1)
            brevbestilling.first().let {
                Assertions.assertThat(it.behandlingId).isEqualTo(behandlingId)
                Assertions.assertThat(it.typeBrev).isEqualTo(typeBrev)
                Assertions.assertThat(it.referanse).isEqualTo(referanse)
                Assertions.assertThat(it.status).isEqualTo(Status.SENDT)
            }

            brevbestillingRepository.oppdaterStatus(behandlingId, referanse,
                Status.FORHÅNDSVISNING_KLAR
            )

            val oppdatertBrevbestilling = brevbestillingRepository.hent(behandlingId)
            Assertions.assertThat(oppdatertBrevbestilling).hasSize(1)
            Assertions.assertThat(oppdatertBrevbestilling.first().status)
                .isEqualTo(Status.FORHÅNDSVISNING_KLAR)
            Assertions.assertThat(oppdatertBrevbestilling.first().typeBrev).isEqualTo(typeBrev)
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
            TypeBehandling.Førstegangsbehandling,
            null,
            VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            )
        ).id
    }
}