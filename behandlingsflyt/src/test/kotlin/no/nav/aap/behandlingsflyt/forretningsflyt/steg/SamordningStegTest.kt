package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class SamordningStegTest {
    @ParameterizedTest
    @EnumSource(
        Ytelse::class,
        names = ["SYKEPENGER", "SVANGERSKAPSPENGER", "PLEIEPENGER_.+"],
        mode = EnumSource.Mode.MATCH_ANY
    )
    fun `om det finnes tilfeller av samordning med Sykepenger, Svangerskapspenger, Pleiepenger, skal det opprettes et avklaringsbehov`(
        ytelse: Ytelse
    ) {
        val steg = settOppRessurser(ytelse)

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

    @ParameterizedTest
    @EnumSource(
        Ytelse::class,
        names = ["FORELDREPENGER", "OMSORGSPENGER", "OPPLÆRINGSPENGER"],
        mode = EnumSource.Mode.MATCH_ANY
    )
    fun `foreldrepenger, omsorgspenger, opplæringspenger avklares automatisk`(ytelse: Ytelse) {
        val steg = settOppRessurser(ytelse)

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = SakId(0),
                behandlingId = BehandlingId(0),
                behandlingType = TypeBehandling.Førstegangsbehandling,
                perioderTilVurdering = setOf()
            )
        )

        assertThat(res).isEqualTo(Fullført)
    }

    private fun settOppRessurser(ytelse: Ytelse): SamordningSteg {
        val samordningYtelseVurderingRepository = InMemorySamordningYtelseVurderingRepository()
        val steg = SamordningSteg(
            samordningService = SamordningService(
                samordningYtelseVurderingRepository = samordningYtelseVurderingRepository,
                underveisRepository = InMemoryUnderveisRepository()
            ),
            samordningRepository = InMemorySamordningRepository(),
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )

        lagreYtelseGrunnlag(samordningYtelseVurderingRepository, ytelse)
        return steg
    }

    private fun lagreYtelseGrunnlag(
        samordningYtelseVurderingRepository: InMemorySamordningYtelseVurderingRepository,
        ytelse: Ytelse
    ) {
        samordningYtelseVurderingRepository.lagreYtelser(
            BehandlingId(0), listOf(
                SamordningYtelse(
                    ytelseType = ytelse,
                    ytelsePerioder = listOf(
                        SamordningYtelsePeriode(
                            periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                            gradering = Prosent(50),
                            kronesum = 1234
                        )
                    ),
                    kilde = "xxxx",
                    saksRef = "xxx"
                )
            )
        )
    }
}