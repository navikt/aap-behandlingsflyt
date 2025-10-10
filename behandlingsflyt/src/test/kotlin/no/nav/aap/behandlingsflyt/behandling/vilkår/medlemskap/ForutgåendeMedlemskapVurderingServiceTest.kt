package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektINorgeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.FolkeregisterStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikk
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class ForutgåendeMedlemskapVurderingServiceTest {
    private val service = ForutgåendeMedlemskapVurderingService()

    @Test
    fun `automatisk om inntekt er oppfylt`() {
        val grunnlag = lagGrunnlag(false, true, false)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    @Test
    fun `stopp om inntekt har hull sste 5 år`() {
        val grunnlag = lagGrunnlag(false, false, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isFalse
    }

    @Test
    fun `automatisk om medl er oppfylt`() {
        val grunnlag = lagGrunnlag(true, false, true)
        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        assertThat(resultat.kanBehandlesAutomatisk).isTrue
    }

    private fun lagGrunnlag(
        godkjentPgaUnntakIMedl: Boolean,
        godkjentPgaInntekt: Boolean,
        inntektHarHull: Boolean
    ): ForutgåendeMedlemskapGrunnlag {
        val inntekterINorgeGrunnlag = if (godkjentPgaInntekt) {
            listOf(
                InntektINorgeGrunnlag("1", 1.0, "NOR", "NOR", "type", Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)), "orgname"),
                InntektINorgeGrunnlag("1", 1.0, "NOR", "NOR", "type", Periode(LocalDate.now().minusYears(3), LocalDate.now()), "orgname"),
            )
        } else if (inntektHarHull) {
            listOf(
                InntektINorgeGrunnlag("1", 1.0, "NOR", "NOR", "type", Periode(LocalDate.now().minusYears(5), LocalDate.now().minusYears(3)), "orgname"),
                InntektINorgeGrunnlag("1", 1.0, "NOR", "NOR", "type", Periode(LocalDate.now().minusYears(1), LocalDate.now()), "orgname"),
            )
        } else emptyList()

        val medlUnntak = if (godkjentPgaUnntakIMedl) {
            MedlemskapUnntakGrunnlag(
                unntak = listOf(
                    Segment(
                        periode = Periode(LocalDate.now().minusYears(5), LocalDate.now()),
                        verdi = Unntak(
                            "unntak",
                            "statusaarsak",
                            true,
                            "grunnlag",
                            "lovvalg",
                            false,
                            EØSLand.NOR.toString(),
                            null
                        )
                    )
                )
            )
        } else null

        return ForutgåendeMedlemskapGrunnlag(
            medlemskapArbeidInntektGrunnlag = ForutgåendeMedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = medlUnntak,
                inntekterINorgeGrunnlag = inntekterINorgeGrunnlag,
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysningGrunnlag = PersonopplysningMedHistorikkGrunnlag(
                brukerPersonopplysning = PersonopplysningMedHistorikk(
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(18)),
                    id = 1,
                    dødsdato = null,
                    statsborgerskap = listOf(Statsborgerskap("NOR")),
                    folkeregisterStatuser = listOf(
                        FolkeregisterStatus(
                            status = PersonStatus.bosatt,
                            gyldighetstidspunkt = LocalDate.now(),
                            opphoerstidspunkt = LocalDate.now()
                        )
                    )
                ),
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )
    }
}
