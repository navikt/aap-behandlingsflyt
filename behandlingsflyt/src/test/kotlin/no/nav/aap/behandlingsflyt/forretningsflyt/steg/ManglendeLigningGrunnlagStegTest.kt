package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBeregningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode
import java.time.LocalDateTime
import java.time.Year
import kotlin.random.Random

class ManglendeLigningGrunnlagStegTest {

    private val sisteÅr = Year.of(2025)

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes inntekt for siste relevante år`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder { this.behandling = behandling }

        lagreBeregningstidspunkt(behandling)
        lagreInntekter(behandling, setOf(sisteÅr, sisteÅr.minusYears(1), sisteÅr.minusYears(2)))
        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter avklaringsbehov når det mangler inntekt for siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder { this.behandling = behandling }

        lagreBeregningstidspunkt(behandling)
        lagreInntekter(behandling, setOf(sisteÅr.minusYears(1), sisteÅr.minusYears(2)))

        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det mangler inntekt men dette ikke er 3 siste relevante år og tidligere vurdering tilsier mulig rett til AAP`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder { this.behandling = behandling }

        lagreBeregningstidspunkt(behandling)

        lagreInntekter(
            behandling, setOf(
                sisteÅr.minusYears(1),
                sisteÅr.minusYears(2),
                sisteÅr.minusYears(5),
                sisteÅr
            )
        )

        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)
        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter ikke avklaringsbehov når det finnes manuell inntekt for 3 siste relevante år`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder { this.behandling = behandling }

        lagreBeregningstidspunkt(behandling)
        // Legger til tidligere avklaringsbehov som er løst og avsluttet for å sjekke at det ikke opprettes nytt behov
        leggTilLøstOgAvsluttetAvklaringsbehov(behandling)

        lagreInntekter(behandling, emptySet())
        InMemoryManuellInntektGrunnlagRepository.lagre(behandling.id, manuelleVurderinger().toSet())

        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `ingen avklaringsbehov hvis tidligere vurdering tilsier ingen rett til AAP`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder { this.behandling = behandling }

        lagreBeregningstidspunkt(behandling)
        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        lagreInntekter(behandling, emptySet())

        val resultat = steg(
            FakeTidligereVurderinger(
                Tidslinje(
                    sak.rettighetsperiode,
                    TidligereVurderinger.UunngåeligAvslag
                )
            ).apply { avslagEllerIngenBehandlingsgrunnlag = true }
        ).utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @ParameterizedTest
    @EnumSource(VurderingType::class, mode = Mode.EXCLUDE, names = ["FØRSTEGANGSBEHANDLING", "REVURDERING"])
    fun `oppretter ikke avklaringsbehov for vurderingstyper som ikke er FØRSTEGANGSBEHANDLING eller REVURDERING`(
        vurderingType: VurderingType
    ) {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val flytKontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = vurderingType
        }

        lagreBeregningstidspunkt(behandling)

        // Vil i utgangspunktet opprette avklaringsbehov dersom ingen inntekter finnes
        lagreInntekter(behandling, emptySet())

        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNull()
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `oppretter avklaringsbehov ved manuell revurdering og tidligere vurdering tilsier mulig rett til AAP`() {
        val (_, behandling) = opprettInMemorySakOgBehandling(
            vurderingsbehov = listOf(
                VurderingsbehovMedPeriode(
                    Vurderingsbehov.BARNETILLEGG
                )
            )
        )
        val flytKontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.behandlingType = TypeBehandling.Revurdering

        }
        lagreBeregningstidspunkt(behandling)

        lagreInntekter(
            behandling, setOf(
                sisteÅr.minusYears(2),
                sisteÅr.minusYears(1)
            )
        )

        val resultat = steg().utfør(flytKontekst)

        val avklaringsbehov = hentAvklaringsbehovet(behandling)

        assertThat(avklaringsbehov).isNotNull
        assertThat(resultat).isEqualTo(Fullført)
    }

    private fun leggTilLøstOgAvsluttetAvklaringsbehov(behandling: Behandling) {
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        avklaringsbehovene.leggTil(Definisjon.FASTSETT_MANUELL_INNTEKT, StegType.MANGLENDE_LIGNING, null, null)
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.FASTSETT_MANUELL_INNTEKT, "begrunnelse", "saksbehandler")
        avklaringsbehovene.løsAvklaringsbehov(
            Definisjon.FASTSETT_MANUELL_INNTEKT,
            begrunnelse = "",
            endretAv = "Krongov"
        )
    }

    private fun steg(
        tidligereVurderinger: TidligereVurderinger = FakeTidligereVurderinger()
    ): ManglendeLigningGrunnlagSteg = ManglendeLigningGrunnlagSteg(
        manuellInntektGrunnlagRepository = inMemoryRepositoryProvider.provide(),
        uføreRepository = inMemoryRepositoryProvider.provide(),
        tidligereVurderinger = tidligereVurderinger,
        beregningService = BeregningService(inMemoryRepositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(inMemoryRepositoryProvider, minimalGatewayProvider()),
        inntektGrunnlagRepository = inMemoryRepositoryProvider.provide(),
        beregningVurderingRepository = inMemoryRepositoryProvider.provide(),
        unleashGateway = minimalGatewayProvider().provide(),
    )

    private fun lagreBeregningstidspunkt(behandling: Behandling) {
        InMemoryBeregningVurderingRepository.lagre(
            behandling.id, BeregningstidspunktVurdering(
                begrunnelse = "...",
                nedsattArbeidsevneEllerStudieevneDato = sisteÅr.plusYears(1).atDay(1),
                ytterligereNedsattBegrunnelse = "...",
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = "..."
            )
        )
    }

    private fun hentAvklaringsbehovet(behandling: Behandling): Avklaringsbehov? {
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, behandling.id)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FASTSETT_MANUELL_INNTEKT))
            .firstOrNull { it.erÅpent() }
        return avklaringsbehov
    }

    private fun lagreInntekter(behandling: Behandling, forÅr: Set<Year>) {
        InMemoryInntektGrunnlagRepository.lagre(
            behandling.id,
            forÅr.map
            { InntektPerÅr(it, Beløp(Random.nextInt(100_000, 500_000))) }.toSet(), emptySet()
        )
    }

    private fun manuelleVurderinger(): Set<ManuellInntektVurdering> = setOf(
        ManuellInntektVurdering(
            år = sisteÅr,
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        ), ManuellInntektVurdering(
            år = sisteÅr.minusYears(1),
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        ), ManuellInntektVurdering(
            år = sisteÅr.minusYears(2),
            begrunnelse = "begrunnelse",
            belop = Beløp(350_000),
            vurdertAv = "saksbehandler",
            opprettet = LocalDateTime.now()
        )
    )
}
