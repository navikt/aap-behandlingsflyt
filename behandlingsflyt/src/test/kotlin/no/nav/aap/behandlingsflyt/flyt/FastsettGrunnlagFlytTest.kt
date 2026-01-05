package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ÅrsVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.ident
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

class FastsettGrunnlagFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `kan hente inn manuell inntektsdata i grunnlag og benytte i beregning`() {
        val ident = nyPerson(harYrkesskade = false, harUtenlandskOpphold = false, inntekter = mutableListOf())
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val nedsattDato = LocalDate.now()

        // Oppretter vanlig søknad
        val behandling = sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
            .løsFramTilGrunnlag(periode.fom)
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = nedsattDato,
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .containsOnly(Definisjon.FASTSETT_MANUELL_INNTEKT)

            }
            .løsAvklaringsBehov(
                AvklarManuellInntektVurderingLøsning(
                    manuellVurderingForManglendeInntekt = ManuellInntektVurderingDto(
                        begrunnelse = "Mangler ligning",
                        belop = BigDecimal(300000),
                        vurderinger = listOf(
                            ÅrsVurdering(
                                år = Year.now().minusYears(1).value,
                                beløp = BigDecimal(300000),
                                eøsBeløp = null,
                                ferdigLignetPGI = null,
                            ),
                            ÅrsVurdering(
                                år = Year.now().minusYears(2).value,
                                beløp = BigDecimal(400000),
                                eøsBeløp = null,
                                ferdigLignetPGI = null,
                            ),
                            ÅrsVurdering(
                                år = Year.now().minusYears(3).value,
                                beløp = BigDecimal(500000),
                                eøsBeløp = null,
                                ferdigLignetPGI = null,
                            ),
                        )
                    )
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov)
                    .extracting<Definisjon> { it.definisjon }
                    .doesNotContain(Definisjon.FASTSETT_MANUELL_INNTEKT)
            }

        val beregningsGrunnlag = dataSource.transaction {
            BeregningsgrunnlagRepositoryImpl(it).hentHvisEksisterer(behandling.id) as Grunnlag11_19
        }

        assertThat(beregningsGrunnlag.inntekter())
            .extracting(GrunnlagInntekt::år, GrunnlagInntekt::inntektIKroner)
            .containsExactlyInAnyOrder(
                tuple(Year.of(nedsattDato.minusYears(1).year), Beløp(BigDecimal(300_000))),
                tuple(Year.of(nedsattDato.minusYears(2).year), Beløp(BigDecimal(400_000))),
                tuple(Year.of(nedsattDato.minusYears(3).year), Beløp(BigDecimal(500_000)))
            )

        assertThat(beregningsGrunnlag.inntekter()).hasSize(3)
    }

    @Test
    fun `henter ikke inn manuell inntektsdata i grunnlag om inntektsdata eksisterer fra før`() {
        val ident = ident()
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Oppretter vanlig søknad
        sendInnSøknad(
            ident, periode, SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = null,
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )
            .løsFramTilGrunnlag(periode.fom)
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov).noneMatch { it.definisjon == Definisjon.FASTSETT_MANUELL_INNTEKT }
            }
    }

}