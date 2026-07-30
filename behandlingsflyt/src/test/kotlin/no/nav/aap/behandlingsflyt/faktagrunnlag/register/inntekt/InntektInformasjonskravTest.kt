package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektkomponentenGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.InntektskomponentData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.FakeTidligereVurderinger
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBeregningVurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

class InntektInformasjonskravTest {

    private val år = Year.of(2025)
    private val relevanteÅr = setOf(Year.of(2023), Year.of(2024), år)
    private val poppBeløp = Beløp(500_000)
    private val lagredeInntekter = relevanteÅr.map { InntektPerÅr(it, Beløp(300_000)) }.toSet()
    private val periode = Periode(år.atDay(1), år.atMonth(12).atEndOfMonth())

    private val aInntekt = Inntekt(
        beløp = 10_000.0,
        opptjeningsland = null,
        skattemessigBosattLand = null,
        opptjeningsperiodeFom = null,
        opptjeningsperiodeTom = null,
        virksomhet = Virksomhet("123"),
        beskrivelse = null,
    )

    private val manuelleInntekter = setOf(
        ManuellInntektVurdering(
            år = år,
            begrunnelse = "Mangler ligning",
            belop = Beløp(300_000),
            vurdertAv = Bruker("saksbehandler"),
        )
    )

    private val poppGateway = mockk<InntektRegisterGateway> {
        every { innhent(any(), any()) } answers {
            secondArg<Set<Year>>().map { InntektPerÅrFraRegister(it, poppBeløp) }.toSet()
        }
    }

    private val aInntektGateway = mockk<InntektkomponentenGateway> {
        every { hentAInntekt(any(), any(), any()) } answers {
            InntektskomponentData(
                listOf(ArbeidsInntektMåned(secondArg(), ArbeidsInntektInformasjon(listOf(aInntekt))))
            )
        }
    }

    private val informasjonskrav = InntektInformasjonskrav(
        sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository),
        inntektGrunnlagRepository = InMemoryInntektGrunnlagRepository,
        manuellInntektGrunnlagRepository = InMemoryManuellInntektGrunnlagRepository,
        underveisRepository = InMemoryUnderveisRepository,
        beregningVurderingRepository = InMemoryBeregningVurderingRepository,
        inntektRegisterGateway = poppGateway,
        inntektkomponentenGateway = aInntektGateway,
        tidligereVurderinger = FakeTidligereVurderinger(),
    )

    @Test
    fun `innhenter når forrige behandling ikke har manuell inntekt`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = false)
        InMemoryInntektGrunnlagRepository.lagre(kontekst.behandlingId, lagredeInntekter, emptySet())

        assertThat(erRelevant(kontekst)).isTrue()
    }

    @Test
    fun `innhenter når forrige behandling har manuell inntekt men ikke ble innvilget`() {
        val kontekst = oppsett(innvilget = false, manuellInntektPåForrige = true)
        InMemoryInntektGrunnlagRepository.lagre(kontekst.behandlingId, lagredeInntekter, emptySet())

        assertThat(erRelevant(kontekst)).isTrue()
    }

    @Test
    fun `innhenter ikke når forrige behandling er innvilget med manuell inntekt`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = true)
        InMemoryInntektGrunnlagRepository.lagre(kontekst.behandlingId, lagredeInntekter, emptySet())

        assertThat(erRelevant(kontekst)).isFalse()
    }

    @Test
    fun `innhenter når beregningstidspunktet endrer seg slik at relevante år ikke matcher lagret grunnlag`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = true)
        InMemoryInntektGrunnlagRepository.lagre(kontekst.behandlingId, lagredeInntekter, emptySet())
        lagreBeregningstidspunkt(kontekst.behandlingId, nedsattDato = år.plusYears(4).atDay(1))

        assertThat(erRelevant(kontekst)).isTrue()
    }

    @Test
    fun `henter både POPP og A-inntekt når informasjonskravet er relevant`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = false)

        val registerdata = hentData(kontekst)

        assertThat(registerdata.inntekter.map { it.år }).containsExactlyInAnyOrderElementsOf(relevanteÅr)
        assertThat(registerdata.inntektsperioder).isNotEmpty()
        verify(exactly = 1) { poppGateway.innhent(any(), relevanteÅr) }
        verify(exactly = 1) { aInntektGateway.hentAInntekt(any(), any(), any()) }
    }

    private fun erRelevant(kontekst: FlytKontekstMedPerioder) =
        informasjonskrav.erRelevant(kontekst, StegType.MANGLENDE_LIGNING, null)

    private fun hentData(kontekst: FlytKontekstMedPerioder) =
        informasjonskrav.hentData(informasjonskrav.klargjør(kontekst))

    private fun oppsett(innvilget: Boolean, manuellInntektPåForrige: Boolean) =
        opprettInMemorySakOgRevurdering().let { (_, forrige, revurdering) ->
            lagreBeregningstidspunkt(revurdering.id)
            lagreUnderveis(forrige, innvilget)
            if (manuellInntektPåForrige) {
                InMemoryManuellInntektGrunnlagRepository.lagre(forrige.id, manuelleInntekter)
            } else {
                InMemoryManuellInntektGrunnlagRepository.slett(forrige.id)
            }
            flytKontekstMedPerioder { this.behandling = revurdering }
        }

    private fun lagreUnderveis(behandling: Behandling, harRett: Boolean) {
        InMemoryUnderveisRepository.lagre(
            behandling.id,
            listOf(
                Underveisperiode(
                    periode = periode,
                    meldePeriode = periode,
                    utfall = if (harRett) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT,
                    rettighetsType = if (harRett) RettighetsType.BISTANDSBEHOV else null,
                    avslagsårsak = if (harRett) null else UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                    grenseverdi = `100_PROSENT`,
                    institusjonsoppholdReduksjon = `0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = `0_PROSENT`,
                        fastsattArbeidsevne = `100_PROSENT`,
                        gradering = `100_PROSENT`,
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    meldepliktStatus = null,
                    meldepliktGradering = `0_PROSENT`,
                )
            ),
            input = object : Faktagrunnlag {}
        )
    }

    private fun lagreBeregningstidspunkt(
        behandlingId: BehandlingId,
        nedsattDato: LocalDate = år.plusYears(1).atDay(1),
    ) {
        InMemoryBeregningVurderingRepository.lagre(
            behandlingId, BeregningstidspunktVurdering(
                begrunnelse = "...",
                nedsattArbeidsevneEllerStudieevneDato = nedsattDato,
                ytterligereNedsattBegrunnelse = "...",
                ytterligereNedsattArbeidsevneDato = nedsattDato,
                vurdertAv = Bruker("..."),
            )
        )
    }
}
