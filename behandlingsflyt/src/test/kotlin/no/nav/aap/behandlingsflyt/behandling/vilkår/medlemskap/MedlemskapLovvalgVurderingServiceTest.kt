package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemKode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.KildesystemMedl
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.Unntak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Personopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Statsborgerskap
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedlemskapLovvalgVurderingServiceTest {
    private val service = MedlemskapLovvalgVurderingService()

    @Test
    fun `automatisk om alle krav er oppfylt`() {
        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
                            verdi = Unntak(
                                "unntak",
                                "statusaarsak",
                                true,
                                "grunnlag",
                                "lovvalg",
                                false,
                                EØSLandEllerLandMedAvtale.NOR.toString(),
                                null
                            )
                        )
                    )
                ),
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)),
                1,
                null,
                PersonStatus.bosatt,
                listOf(Statsborgerskap("NOR"))
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(
                harBoddINorgeSiste5År = true,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = null
            )
        )

        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        assertEquals(true, resultat.kanBehandlesAutomatisk)
    }

    @Test
    fun `kan håndtere flere statsborgerskap`() {
        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
                            verdi = Unntak(
                                "unntak",
                                "statusaarsak",
                                true,
                                "grunnlag",
                                "lovvalg",
                                false,
                                EØSLandEllerLandMedAvtale.NOR.toString(),
                                null
                            )
                        )
                    )
                ),
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)), 1, null, PersonStatus.bosatt,
                listOf(
                    Statsborgerskap("XUK"),
                    Statsborgerskap("NOR"),
                )
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(
                harBoddINorgeSiste5År = true,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = null
            )
        )

        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        assertEquals(true, resultat.kanBehandlesAutomatisk)
    }

    @Test
    fun `manuell om lovvalgsland ikke er Norge`() {
        val grunnlag = MedlemskapLovvalgGrunnlag(
            medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
                medlemskapGrunnlag = MedlemskapUnntakGrunnlag(
                    unntak = listOf(
                        Segment(
                            periode = Periode(LocalDate.now().minusMonths(1), LocalDate.now()),
                            verdi = Unntak(
                                "unntak",
                                "statusaarsak",
                                true,
                                "grunnlag",
                                "lovvalg",
                                false,
                                EØSLandEllerLandMedAvtale.SWE.toString(),
                                KildesystemMedl(KildesystemKode.MEDL, "MEDL")
                            )
                        )
                    )
                ),
                inntekterINorgeGrunnlag = emptyList(),
                arbeiderINorgeGrunnlag = emptyList(),
                manuellVurdering = null
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)), 1, null, PersonStatus.bosatt, listOf(
                    Statsborgerskap("XUK"),
                )
            ),
            nyeSoknadGrunnlag = UtenlandsOppholdData(
                harBoddINorgeSiste5År = true,
                harArbeidetINorgeSiste5År = true,
                arbeidetUtenforNorgeFørSykdom = false,
                iTilleggArbeidUtenforNorge = false,
                utenlandsOpphold = null
            )
        )

        val resultat = service.vurderTilhørighet(grunnlag, Periode(LocalDate.now().minusYears(1), LocalDate.now()))
        assertEquals(false, resultat.kanBehandlesAutomatisk)
    }
}
