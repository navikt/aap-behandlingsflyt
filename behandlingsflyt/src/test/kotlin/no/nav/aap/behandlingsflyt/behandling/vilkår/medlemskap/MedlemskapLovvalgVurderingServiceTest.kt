package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedlemskapLovvalgVurderingServiceTest {
    private val service = MedlemskapLovvalgVurderingService()

    @Test
    fun automatiskOmAlleKravErOppfylt() {
        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
                            verdi = Unntak("unntak", "statusaarsak", true, "grunnlag", "lovvalg", false, EØSLand.NOR.toString())
                        )
                    )
                ),
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysningGrunnlag = PersonopplysningGrunnlag(
                brukerPersonopplysning = Personopplysning(Fødselsdato(LocalDate.now().minusYears(18)), 1, null, EØSLand.NOR.toString(), LocalDate.now().minusMonths(1), null, PersonStatus.bosatt),
                relatertePersonopplysninger = null
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )

        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        assertEquals(true, resultat.kanBehandlesAutomatisk)
    }

    @Test
    fun manuellOmLovvalgslandIkkeErNorge() {
        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
                            verdi = Unntak("unntak", "statusaarsak", true, "grunnlag", "lovvalg", false, EØSLand.SWE.toString())
                        )
                    )
                ),
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysningGrunnlag = PersonopplysningGrunnlag(
                brukerPersonopplysning = Personopplysning(Fødselsdato(LocalDate.now().minusYears(18)), 1, null, EØSLand.NOR.toString(), LocalDate.now().minusMonths(1), null, PersonStatus.bosatt),
                relatertePersonopplysninger = null
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(true, true, false, false, null)
        )

        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        assertEquals(false, resultat.kanBehandlesAutomatisk)
    }
}
