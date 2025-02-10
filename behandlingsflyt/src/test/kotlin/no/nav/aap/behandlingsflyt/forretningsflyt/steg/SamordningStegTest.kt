package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemorySamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.testutil.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SamordningStegTest {
    @Test
    fun `om det finnes tilfeller av samordning, skal det opprettes et avklaringsbehov`() {
        val samordningYtelseVurderingRepository = InMemorySamordningYtelseVurderingRepository()
        val steg = SamordningSteg(
            samordningService = SamordningService(
                samordningYtelseVurderingRepository = samordningYtelseVurderingRepository,
                underveisRepository = InMemoryUnderveisRepository
            ),
            samordningRepository = InMemorySamordningRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )

//        samordningYtelseVurderingRepository.lagreYtelser(
//            BehandlingId(0), listOf(
//                SamordningYtelse(
//                    ytelseType = Ytelse.SYKEPENGER,
//                    ytelsePerioder = listOf(
//                        SamordningYtelsePeriode(
//                            periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
//                            gradering = Prosent(50),
//                            kronesum = 1234
//                        )
//                    ),
//                    kilde = "xxxx",
//                    saksRef = "xxx"
//                )
//            )
//        )

        samordningYtelseVurderingRepository.lagreVurderinger(
            BehandlingId(0), listOf(
                SamordningVurdering(
                    ytelseType = Ytelse.SYKEPENGER,
                    vurderingPerioder = listOf(
                        SamordningVurderingPeriode(
                            Periode(
                                LocalDate.now().minusWeeks(1),
                                LocalDate.now()
                            ), Prosent(50), 0
                        )
                    )
                )
            )
        )

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = SakId(0),
                behandlingId = BehandlingId(0),
                behandlingType = TypeBehandling.Førstegangsbehandling,
                perioderTilVurdering = setOf()
            )
        )

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))
    }
}