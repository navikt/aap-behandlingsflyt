package no.nav.aap.behandlingsflyt.periodisering

import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PerioderTilVurderingServiceTest {

    @Test
    fun `ved førstegangsbehandling skal hele perioden vurderes`() {
        val periode = Periode(
            LocalDate.now(),
            Tid.MAKS
        )
        val (sak, behandling) = opprettInMemorySakOgBehandling()

        val flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(
            inMemoryRepositoryProvider,
            createGatewayProvider { }
        )

        val res = flytKontekstMedPeriodeService.utled(
            behandling.flytKontekst(),
            stegType = StegType.AVKLAR_SYKDOM
        )

        assertThat(res).isNotNull
        assertThat(res.vurderingsbehovRelevanteForSteg.first()).isEqualTo(Vurderingsbehov.MOTTATT_SØKNAD)
        assertThat(res.rettighetsperiode).isEqualTo(periode)
    }

}