package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySamordningYtelseVurderingRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.util.*

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
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val steg = settOppRessurser(ytelse, behandling.id)

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                behandlingType = TypeBehandling.Revurdering,
                perioderTilVurdering = setOf(
                    Vurdering(
                        type = VurderingType.REVURDERING,
                        årsaker = listOf(ÅrsakTilBehandling.MOTTATT_MELDEKORT),
                        periode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
                    )
                )
            )
        )

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))

        InMemorySamordningYtelseVurderingRepository.lagreVurderinger(
            behandling.id, listOf(
                SamordningVurdering(
                    ytelseType = ytelse,
                    vurderingPerioder = listOf(
                        SamordningVurderingPeriode(
                            periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                            gradering = Prosent(50),
                            kronesum = null
                        )
                    )
                )
            )
        )

        val res2 = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                behandlingType = TypeBehandling.Revurdering,
                perioderTilVurdering = setOf(
                    Vurdering(
                        type = VurderingType.REVURDERING,
                        årsaker = listOf(ÅrsakTilBehandling.MOTTATT_MELDEKORT),
                        periode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
                    )
                )
            )
        )

        assertThat(res2).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(
        Ytelse::class,
        names = ["FORELDREPENGER", "OMSORGSPENGER", "OPPLÆRINGSPENGER"],
        mode = EnumSource.Mode.MATCH_ANY
    )
    fun `foreldrepenger, omsorgspenger, opplæringspenger avklares automatisk`(ytelse: Ytelse) {
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val steg = settOppRessurser(ytelse, behandling.id)

        val res = steg.utfør(
            kontekst = FlytKontekstMedPerioder(
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                behandlingType = TypeBehandling.Førstegangsbehandling,
                perioderTilVurdering = setOf()
            )
        )

        assertThat(res).isEqualTo(Fullført)
    }

    @Test
    fun `om det kommer ny informasjon, avklaringsbehov opprettes igjen`() {
        val behandling = opprettBehandling(nySak(), TypeBehandling.Revurdering)
        val steg = settOppRessurser(Ytelse.SYKEPENGER, behandling.id)
        val kontekst = FlytKontekstMedPerioder(
            sakId = behandling.sakId,
            behandlingId = behandling.id,
            behandlingType = TypeBehandling.Revurdering,
            perioderTilVurdering = setOf(
                Vurdering(
                    type = VurderingType.REVURDERING,
                    årsaker = listOf(ÅrsakTilBehandling.MOTTATT_MELDEKORT),
                    periode = Periode(LocalDate.now().minusYears(1), LocalDate.now())
                )
            )
        )

        val res = steg.utfør(kontekst = kontekst)

        assertThat(res).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))

        InMemorySamordningYtelseVurderingRepository.lagreVurderinger(
            behandling.id, listOf(
                SamordningVurdering(
                    ytelseType = Ytelse.SYKEPENGER,
                    vurderingPerioder = listOf(
                        SamordningVurderingPeriode(
                            periode = Periode(LocalDate.now().minusYears(1), LocalDate.now()),
                            gradering = Prosent(50),
                            kronesum = null
                        )
                    )
                )
            )
        )

        val res2 = steg.utfør(kontekst = kontekst)

        assertThat(res2).isEqualTo(Fullført)

        lagreYtelseGrunnlag(behandling.id, Ytelse.SYKEPENGER, Periode(LocalDate.now().minusYears(2), LocalDate.now()))

        val res3 = steg.utfør(kontekst = kontekst)

        assertThat(res3).isEqualTo(FantAvklaringsbehov(Definisjon.AVKLAR_SAMORDNING_GRADERING))

    }

    private fun settOppRessurser(
        ytelse: Ytelse,
        behandlingId: BehandlingId
    ): SamordningSteg {
        val steg = SamordningSteg(
            samordningService = SamordningService(
                samordningYtelseVurderingRepository = InMemorySamordningYtelseVurderingRepository,
                samordningRepository = InMemorySamordningRepository
            ),
            samordningRepository = InMemorySamordningRepository,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
        )

        lagreYtelseGrunnlag(behandlingId, ytelse, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        return steg
    }

    private fun lagreYtelseGrunnlag(
        behandlingId: BehandlingId,
        ytelse: Ytelse,
        periode: Periode
    ) {
        InMemorySamordningYtelseVurderingRepository.lagreYtelser(
            behandlingId, listOf(
                SamordningYtelse(
                    ytelseType = ytelse,
                    ytelsePerioder = listOf(
                        SamordningYtelsePeriode(
                            periode = periode,
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

    // TODO: trekk disse ut i felles hjelpemetoder i testene
    private fun opprettBehandling(sak: Sak, typeBehandling: TypeBehandling) =
        InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            årsaker = listOf(),
            typeBehandling = typeBehandling,
            forrigeBehandlingId = null,
        )

    private fun nySak() = InMemorySakRepository.finnEllerOpprett(
        person = Person(
            id = 0,
            identifikator = UUID.randomUUID(),
            identer = listOf(Ident("0".repeat(11)))
        ),
        periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
    )
}