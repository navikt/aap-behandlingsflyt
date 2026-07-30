package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
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
import java.time.Year

class InntektInformasjonskravTest {

    private val år = Year.of(2025)
    private val inntektFraPopp = InntektPerÅrFraRegister(år, Beløp(500_000))
    private val lagretInntekt = InntektPerÅr(år, Beløp(300_000))
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
        every { innhent(any(), any()) } returns setOf(inntektFraPopp)
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
    fun `henter inntekt fra POPP når forrige behandling ikke har manuell inntekt`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = false)

        val registerdata = hentData(kontekst)

        assertThat(registerdata.inntekter).containsExactly(inntektFraPopp)
        verify(exactly = 1) { poppGateway.innhent(any(), any()) }
    }

    @Test
    fun `henter inntekt fra POPP når forrige behandling har manuell inntekt men ikke ble innvilget`() {
        val kontekst = oppsett(innvilget = false, manuellInntektPåForrige = true)

        hentData(kontekst)

        verify(exactly = 1) { poppGateway.innhent(any(), any()) }
    }

    @Test
    fun `viderefører lagret årsinntekt og henter fortsatt A-inntekt når forrige behandling er innvilget med manuell inntekt`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = true)
        InMemoryInntektGrunnlagRepository.lagre(kontekst.behandlingId, setOf(lagretInntekt), emptySet())

        val registerdata = hentData(kontekst)

        assertThat(registerdata.inntekter)
            .containsExactly(InntektPerÅrFraRegister(lagretInntekt.år, lagretInntekt.beløp))
        assertThat(registerdata.inntektsperioder).isNotEmpty()
        verify(exactly = 0) { poppGateway.innhent(any(), any()) }
        verify(exactly = 1) { aInntektGateway.hentAInntekt(any(), any(), any()) }
    }

    @Test
    fun `informasjonsgrunnlag er uendret når årsinntekter videreføres fra lagret grunnlag`() {
        val kontekst = oppsett(innvilget = true, manuellInntektPåForrige = true)
        val førsteRegisterdata = hentData(kontekst)
        InMemoryInntektGrunnlagRepository.lagre(
            kontekst.behandlingId,
            setOf(lagretInntekt),
            førsteRegisterdata.inntektsperioder
        )

        val input = informasjonskrav.klargjør(kontekst)
        val resultat = informasjonskrav.oppdater(input, informasjonskrav.hentData(input), kontekst)

        assertThat(resultat).isEqualTo(Informasjonskrav.Endret.IKKE_ENDRET)
    }

    private fun hentData(kontekst: FlytKontekstMedPerioder) =
        informasjonskrav.hentData(informasjonskrav.klargjør(kontekst))

    private fun oppsett(innvilget: Boolean, manuellInntektPåForrige: Boolean) =
        opprettInMemorySakOgRevurdering().let { (_, forrige, revurdering) ->
            lagreBeregningstidspunkt(revurdering)
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

    private fun lagreBeregningstidspunkt(behandling: Behandling) {
        InMemoryBeregningVurderingRepository.lagre(
            behandling.id, BeregningstidspunktVurdering(
                begrunnelse = "...",
                nedsattArbeidsevneEllerStudieevneDato = år.plusYears(1).atDay(1),
                ytterligereNedsattBegrunnelse = "...",
                ytterligereNedsattArbeidsevneDato = år.plusYears(1).atDay(1),
                vurdertAv = Bruker("..."),
            )
        )
    }
}
