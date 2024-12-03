package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySakRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class PerioderTilVurderingServiceTest {

    @Test
    fun `ved førstegangsbehandling skal hele perioden vurderes`() {
        val periode = Periode(
            LocalDate.now(),
            LocalDate.now().plusMonths(1)
        )
        val sak = InMemorySakRepository.finnEllerOpprett(
            Person(
                identifikator = UUID.randomUUID(),
                identer = listOf(),
                id = 0
            ), periode
        )


        val perioderTilVurderingService = PerioderTilVurderingService(
            sakService = SakService(
                sakRepository = InMemorySakRepository
            ),
            behandlingRepository = InMemoryBehandlingRepository
        )

        val res = perioderTilVurderingService.utled(
            FlytKontekst(
                sakId = sak.id,
                behandlingId = BehandlingId(0),
                behandlingType = TypeBehandling.Førstegangsbehandling
            ),
            stegType = StegType.AVKLAR_SYKDOM
        )

        assertThat(res).hasSize(1)
        assertThat(res.first().årsaker.first()).isEqualTo(ÅrsakTilBehandling.MOTTATT_SØKNAD)
        assertThat(res.first().periode).isEqualTo(periode)
    }
}