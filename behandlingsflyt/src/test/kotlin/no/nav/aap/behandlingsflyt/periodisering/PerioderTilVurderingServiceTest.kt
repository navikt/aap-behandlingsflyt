package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
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
                identer = emptyList(),
            ), periode
        )

        val behandling = InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            TypeBehandling.Førstegangsbehandling,
            null,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                ÅrsakTilOpprettelse.SØKNAD
            )
        )

        val flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(
            inMemoryRepositoryProvider,
            createGatewayProvider { }
        )

        val res = flytKontekstMedPeriodeService.utled(
            FlytKontekst(
                sakId = sak.id,
                behandlingId = behandling.id,
                forrigeBehandlingId = behandling.forrigeBehandlingId,
                behandlingType = TypeBehandling.Førstegangsbehandling
            ),
            stegType = StegType.AVKLAR_SYKDOM
        )

        assertThat(res).isNotNull
        assertThat(res.vurderingsbehovRelevanteForSteg.first()).isEqualTo(Vurderingsbehov.MOTTATT_SØKNAD)
        assertThat(res.rettighetsperiode).isEqualTo(periode)
    }

}