package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ArbeidINorgeGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.Arbeidsforholdtype
import no.nav.aap.behandlingsflyt.behandling.lovvalg.EnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektInformasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.ArbeidsInntektMåned
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Inntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning.Virksomhet
import java.time.LocalDate
import java.time.YearMonth

internal fun forutgåendeRegisterdata(beløp: Double = 10_000.0) =
    ForutgåendeMedlemskapInformasjonskrav.MedlemsskapReggisterdata(
        medlemskapPerioder = listOf(
            MedlemskapDataIntern(
                unntakId = 1,
                ident = "12345678901",
                fraOgMed = "2020-01-01",
                tilOgMed = "2021-01-01",
                status = "GYLD",
                statusaarsak = null,
                medlem = true,
                grunnlag = "grunnlag",
                lovvalg = "ENDL",
                helsedel = true,
                lovvalgsland = "NOR",
                kilde = null
            )
        ),
        arbeidGrunnlag = listOf(
            ArbeidINorgeGrunnlag(
                identifikator = "999999999",
                arbeidsforholdKode = Arbeidsforholdtype.ORDINAERT_ARBEIDSFORHOLD,
                startdato = LocalDate.of(2020, 1, 1),
                sluttdato = null
            )
        ),
        inntektGrunnlag = listOf(
            ArbeidsInntektMåned(
                årMåned = YearMonth.of(2020, 6),
                arbeidsInntektInformasjon = ArbeidsInntektInformasjon(
                    listOf(
                        Inntekt(
                            beløp = beløp,
                            opptjeningsland = null,
                            skattemessigBosattLand = null,
                            opptjeningsperiodeFom = null,
                            opptjeningsperiodeTom = null,
                            virksomhet = Virksomhet(identifikator = "999999999"),
                            beskrivelse = null
                        )
                    )
                )
            )
        ),
        enhetGrunnlag = listOf(EnhetGrunnlag(orgnummer = "999999999", orgNavn = "Testbedrift AS"))
    )
