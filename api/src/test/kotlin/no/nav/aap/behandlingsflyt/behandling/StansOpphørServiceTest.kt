package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.help.finnEllerOpprettBehandling
import no.nav.aap.behandlingsflyt.help.sak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStansOpphørRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakslengdeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Fakes
class StansOpphørServiceTest {

    private val service =
        StansOpphørService(InMemoryVedtakslengdeRepository, InMemoryUnderveisRepository, InMemoryStansOpphørRepository)

    @Test
    fun `skal beholde stans-opphør som er innenfor vedtaksperioden`() {
        val behandling = nyBehandling(LocalDate.now())
        lagreVedtakslengde(behandling, sluttdato = LocalDate.now().plusYears(1))
        lagreUnderveis(behandling, LocalDate.now(), LocalDate.now().plusYears(3))
        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    stansEntry(behandling, LocalDate.now().plusYears(3), Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP),
                    stansEntry(behandling, LocalDate.now().plusMonths(6), Avslagsårsak.BRUKER_OVER_67)
                )
            )
        )

        val resultat = service.vedtattStansOpphør(behandling.id)

        assertThat(resultat).isNotEmpty()
        assertThat(resultat.size).isEqualTo(1)
        assertThat(resultat.first().vurdering.årsaker).contains(Avslagsårsak.BRUKER_OVER_67)
    }

    @Test
    fun `opphør én dag etter stans skal med`() {
        val søknadsdato = 1 april 2026
        val behandling = nyBehandling(søknadsdato)

        // Skal bli ignorert, siden vi har vedtakslengde
        lagreUnderveis(behandling, søknadsdato, rettTilOgMed = søknadsdato.plusYears(3))
        lagreVedtakslengde(behandling, sluttdato = 1 juni 2026)

        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    stansEntry(
                        behandling,
                        2 juni 2026,
                        Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
                    )
                )
            )
        )

        val res = service.vedtattStansOpphør(behandling.id)
        assertThat(res).hasSize(1)
        assertThat(res.first().fom).isEqualTo(2 juni 2026)
        assertThat(res.first().vurdering.årsaker).containsExactlyInAnyOrder(Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP)
    }

    @Test
    fun `skal filtrere bort stans-opphør som er for langt fram i tid`() {
        val behandling = nyBehandling(LocalDate.now())
        lagreVedtakslengde(behandling, sluttdato = LocalDate.now().plusYears(1))
        lagreUnderveis(behandling, LocalDate.now(), LocalDate.now().plusYears(3))
        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    stansEntry(
                        behandling,
                        LocalDate.now().plusYears(3),
                        Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP
                    )
                )
            )
        )

        assertThat(service.vedtattStansOpphør(behandling.id)).isEmpty()
    }

    @Test
    fun `om vedtakslengde ikke er lagret, velg siste dag med rett fra underveis-tidslinjen som vedtakslengde`() {
        val søknadsdato = 10 april 2026
        val behandling = nyBehandling(søknadsdato)
        InMemoryStansOpphørRepository.lagre(
            behandling.id, StansOpphørGrunnlag(
                stansOgOpphør = setOf(
                    stansEntry(
                        behandling,
                        søknadsdato.plusMonths(6).plusDays(1),
                        Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
                    )
                )
            )
        )
        lagreUnderveis(behandling, søknadsdato, søknadsdato.plusMonths(6))

        val resultat = service.vedtattStansOpphør(behandling.id)

        assertThat(resultat).isNotEmpty()
        assertThat(resultat.size).isEqualTo(1)
        assertThat(resultat.first().fom).isEqualTo(søknadsdato.plusMonths(6).plusDays(1))
    }

    private fun lagreUnderveis(
        behandling: Behandling,
        søknadsdato: LocalDate,
        rettTilOgMed: LocalDate
    ) {
        InMemoryUnderveisRepository.lagre(
            behandling.id, listOf(
                Underveisperiode(
                    periode = Periode(søknadsdato, rettTilOgMed),
                    meldePeriode = Periode(søknadsdato, søknadsdato.plusWeeks(2)),
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.SYKEPENGEERSTATNING,
                    avslagsårsak = null,
                    grenseverdi = Prosent(80),
                    institusjonsoppholdReduksjon = Prosent(0),
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = `0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`100_PROSENT`,
                        gradering = Prosent.`100_PROSENT`,
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    meldepliktStatus = MeldepliktStatus.MELDT_SEG,
                    meldepliktGradering = Prosent(100),
                ),
            ),
            input = object : Faktagrunnlag {}
        )
    }

    private fun nyBehandling(søknadsdato: LocalDate): Behandling {
        val sak = sak(inMemoryRepositoryProvider, søknadsdato)
        return finnEllerOpprettBehandling(inMemoryRepositoryProvider, testGatewayProvider(), sak.saksnummer)
    }

    private fun lagreVedtakslengde(behandling: Behandling, sluttdato: LocalDate) {
        InMemoryVedtakslengdeRepository.lagre(
            behandling.id, listOf(
                VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.TREDJE_ÅR,
                    vurdertAv = Bruker("saksbehandler"),
                    vurdertIBehandling = behandling.id,
                    opprettet = Instant.now(),
                    begrunnelse = "..."
                )
            )
        )
    }

    private fun stansEntry(behandling: Behandling, fom: LocalDate, årsak: Avslagsårsak) =
        GjeldendeStansEllerOpphør(
            fom = fom,
            opprettet = Instant.now(),
            vurdertIBehandling = behandling.id,
            vurdering = Stans(årsaker = setOf(årsak))
        )

}