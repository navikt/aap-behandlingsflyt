package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryUnderveisRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVedtakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class VarsleVedtakJobbUtførerTest {
    object SamGateWayFake : SamGateway {
        val varslinger = mutableListOf<SamordneVedtakRequest>()
        override fun varsleVedtak(request: SamordneVedtakRequest): SamordneVedtakRespons {
            varslinger.add(request)
            return SamordneVedtakRespons(true)
        }

        override fun hentSamId(
            ident: Ident,
            sakId: Long,
            vedtakId: VedtakId
        ): List<SamIdOgTpNr> {
            TODO("Not yet implemented")
        }
    }

    object FakeTjenestePensjonGateway : TjenestePensjonGateway {
        val tpForIdent = mutableMapOf<String, List<TjenestePensjonForhold>>()

        fun leggTilTpForhold(ident: String) {
            tpForIdent[ident] = listOf(
                TjenestePensjonForhold(
                    ordning = TjenestePensjonOrdning(
                        navn = "tp",
                        tpNr = "1234",
                        orgNr = "543"
                    ),
                    ytelser = setOf(
                        TjenestePensjonYtelse(
                            innmeldtYtelseFom = LocalDate.now(),
                            ytelseType = YtelseTypeCode.BETINGET_TP,
                            ytelseIverksattFom = LocalDate.now(),
                            ytelseIverksattTom = null,
                            ytelseId = 1234L
                        )
                    )
                )
            )
        }

        override fun hentTjenestePensjon(ident: String): List<TjenestePensjonForhold> = tpForIdent[ident] ?: emptyList()
    }

    @Test
    fun `varsler vedtak ved innvilgelse`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling()

        InMemoryVedtakRepository.lagre(behandling.id, LocalDateTime.now(), LocalDate.now())

        lagreUnderveis(behandling, sak)

        FakeTjenestePensjonGateway.leggTilTpForhold(sak.person.aktivIdent().identifikator)

        val utfører =
            VarsleVedtakJobbUtfører(
                inMemoryRepositoryProvider,
                minimalGatewayProvider {
                    register<SamGateWayFake>()
                    register<FakeTjenestePensjonGateway>()
                })

        val input = JobbInput(VarsleVedtakJobbUtfører.Companion).medPayload(behandling.id)
        utfører.utfør(input)

        assertThat(SamGateWayFake.varslinger.filter { it.pid == sak.person.aktivIdent().identifikator }.size).isEqualTo(
            1
        )
    }

    @Test
    fun `endringITilkjentYtelseTidslinje - begge null gir false`() {
        val resultat = VarsleVedtakJobbUtfører.endringITilkjentYtelseTidslinje(null, null)
        assertFalse(resultat)

    }

    @Test
    fun `endringITilkjentYtelseTidslinje - ingen forrige men nåværende gir true`() {
        val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

        val resultat = VarsleVedtakJobbUtfører.endringITilkjentYtelseTidslinje(null, nåværende)
        assertTrue(resultat)

    }

    @Test
    fun `endringITilkjentYtelseTidslinje - endring fra 0 til ikke-0 gir true`() {
        val forrige = tidslinjeMedGradering(grad = Prosent.`0_PROSENT`)
        val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

        val resultat = VarsleVedtakJobbUtfører.endringITilkjentYtelseTidslinje(forrige, nåværende)
        assertTrue(resultat)

    }

    @Test
    fun `endringITilkjentYtelseTidslinje - samme gradering og dagsats gir false`() {
        val forrige = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)
        val nåværende = tidslinjeMedGradering(grad = Prosent.`50_PROSENT`)

        val resultat = VarsleVedtakJobbUtfører.endringITilkjentYtelseTidslinje(forrige, nåværende)
        assertFalse(resultat)

    }

    private fun lagreUnderveis(
        behandling: Behandling,
        sak: Sak
    ) {
        InMemoryUnderveisRepository.lagre(
            behandling.id, listOf(
                Underveisperiode(
                    sak.rettighetsperiodeEttÅrFraStartDato(),
                    meldePeriode = sak.rettighetsperiodeEttÅrFraStartDato(),
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`66_PROSENT`,
                    institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
                    arbeidsgradering = ArbeidsGradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                        andelArbeid = Prosent.`0_PROSENT`,
                        fastsattArbeidsevne = Prosent.`0_PROSENT`,
                        gradering = Prosent.`0_PROSENT`,
                        opplysningerMottatt = null
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = emptySet(),
                    meldepliktStatus = null,
                    meldepliktGradering = null,
                    backFillStansOpphorEnabled = false,
                )
            ), object : Faktagrunnlag {})
    }

    private fun tidslinjeMedGradering(grad: Prosent): Tidslinje<Tilkjent> {
        val tilkjent = Tilkjent(
            dagsats = Beløp(100),
            gradering = grad,
            graderingGrunnlag = GraderingGrunnlag(
                samordningGradering = Prosent.`0_PROSENT`,
                institusjonGradering = Prosent.`0_PROSENT`,
                arbeidGradering = Prosent.`0_PROSENT`,
                samordningUføregradering = Prosent.`0_PROSENT`,
                samordningArbeidsgiverGradering = Prosent.`0_PROSENT`,
                meldepliktGradering = Prosent.`0_PROSENT`,
            ),
            grunnlagsfaktor = GUnit("1.0"),
            grunnbeløp = Beløp(100),
            barnepensjonDagsats = Beløp(0),
            antallBarn = 0,
            barnetilleggsats = Beløp(0),
            barnetillegg = Beløp(0),
            utbetalingsdato = LocalDate.now(),
            minsteSats = Minstesats.MINSTESATS_UNDER_25,
            redusertDagsats = Beløp(100),
        )

        val periode = Periode(LocalDate.now(), LocalDate.now())
        return Tidslinje(listOf(Segment(periode, tilkjent))).komprimer()
    }
}