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
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MedlemskapLovvalgVurderingServiceTest {
    private val service = MedlemskapLovvalgVurderingService()
    private val unleash = FakeUnleashBaseWithDefaultDisabled(
        enabledFlags = listOf(BehandlingsflytFeature.BosattStatsborgerskapGjennomslipp)
    )

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
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)),
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

        val resultat = service.vurderTilhørighet(
            grunnlag,
            Periode(LocalDate.now().minusYears(1), LocalDate.now()),
            unleashGateway = unleash
        )
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
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)), null, PersonStatus.bosatt,
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

        val resultat = service.vurderTilhørighet(
            grunnlag,
            Periode(LocalDate.now().minusYears(1), LocalDate.now()),
            unleashGateway = unleash
        )
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
            ),
            personopplysning = Personopplysning(
                Fødselsdato(LocalDate.now().minusYears(18)), null, PersonStatus.bosatt, listOf(
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

        val resultat = service.vurderTilhørighet(
            grunnlag,
            Periode(LocalDate.now().minusYears(1), LocalDate.now()),
            unleashGateway = unleash
        )
        assertEquals(false, resultat.kanBehandlesAutomatisk)
    }

    @Test
    fun `bosatt person med gyldig norsk statsborgerskap behandles automatisk`() {
        val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        val grunnlag = grunnlagUtenAndreGjennomslippskriterier(
            statsborgerskap = listOf(
                Statsborgerskap(
                    land = EØSLandEllerLandMedAvtale.NOR.name,
                    gyldigFraOgMed = LocalDate.of(2026, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2026, 12, 31),
                )
            )
        )

        val resultat = service.vurderTilhørighet(
            grunnlag,
            rettighetsperiode,
            unleashGateway = unleash,
        )

        assertEquals(true, resultat.kanBehandlesAutomatisk)
        val vurdering = resultat.tilhørighetVurdering.single {
            it.opplysning == "Bosatt i Norge med norsk statsborgerskap"
        }
        assertEquals(true, vurdering.resultat)
    }

    @Test
    fun `bosatt person uten norsk statsborgerskap behandles ikke automatisk`() {
        val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        val grunnlag = grunnlagUtenAndreGjennomslippskriterier(
            statsborgerskap = listOf(Statsborgerskap(EØSLandEllerLandMedAvtale.SWE.name))
        )

        val resultat = service.vurderTilhørighet(
            grunnlag,
            rettighetsperiode,
            unleashGateway = unleash,
        )

        assertEquals(false, resultat.kanBehandlesAutomatisk)
        val vurdering = resultat.tilhørighetVurdering.single {
            it.opplysning == "Bosatt i Norge med norsk statsborgerskap"
        }
        assertEquals(false, vurdering.resultat)
    }

    @Test
    fun `bosatt og norsk statsborgerskap utenfor rettighetsperioden gir ikke gjennomslipp`() {
        val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        val grunnlag = grunnlagUtenAndreGjennomslippskriterier(
            statsborgerskap = listOf(
                Statsborgerskap(
                    land = EØSLandEllerLandMedAvtale.NOR.name,
                    gyldigFraOgMed = LocalDate.of(2024, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2025, 12, 31),
                )
            )
        )

        val resultat = service.vurderTilhørighet(
            grunnlag,
            rettighetsperiode,
            unleashGateway = unleash,
        )

        assertEquals(false, resultat.kanBehandlesAutomatisk)
        val vurdering = resultat.tilhørighetVurdering.single {
            it.opplysning == "Bosatt i Norge med norsk statsborgerskap"
        }
        assertEquals(false, vurdering.resultat)
        assertEquals(
            emptyList<GyldigStatsborgerskap>(),
            vurdering.bosattStatusOgNorskStatsborgerskap?.statsborgerskap,
        )
    }

    @Test
    fun `norsk statsborger uten bosattstatus får ikke gjennomslipp`() {
        val rettighetsperiode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))
        val grunnlag = grunnlagUtenAndreGjennomslippskriterier(
            personStatus = PersonStatus.ikkeBosatt,
            statsborgerskap = listOf(Statsborgerskap(EØSLandEllerLandMedAvtale.NOR.name)),
        )

        val resultat = service.vurderTilhørighet(
            grunnlag,
            rettighetsperiode,
            unleashGateway = unleash,
        )

        assertEquals(false, resultat.kanBehandlesAutomatisk)
        val vurdering = resultat.tilhørighetVurdering.single {
            it.opplysning == "Bosatt i Norge med norsk statsborgerskap"
        }
        assertEquals(false, vurdering.resultat)
    }

    private fun grunnlagUtenAndreGjennomslippskriterier(
        personStatus: PersonStatus = PersonStatus.bosatt,
        statsborgerskap: List<Statsborgerskap>,
    ) = MedlemskapLovvalgGrunnlag(
        medlemskapArbeidInntektGrunnlag = MedlemskapArbeidInntektGrunnlag(
            medlemskapGrunnlag = MedlemskapUnntakGrunnlag(emptyList()),
            inntekterINorgeGrunnlag = emptyList(),
            arbeiderINorgeGrunnlag = emptyList(),
        ),
        personopplysning = Personopplysning(
            fødselsdato = Fødselsdato(LocalDate.of(1990, 1, 1)),
            status = personStatus,
            statsborgerskap = statsborgerskap,
        ),
        nyeSoknadGrunnlag = UtenlandsOppholdData(
            harBoddINorgeSiste5År = true,
            harArbeidetINorgeSiste5År = true,
            arbeidetUtenforNorgeFørSykdom = false,
            iTilleggArbeidUtenforNorge = false,
            utenlandsOpphold = null,
        ),
    )
}
