package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningMedHistorikkGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode

class ForutgåendeMedlemskapLovvalgVurderingService {
    fun vurderTilhørighet(grunnlag: ForutgåendeMedlemskapGrunnlag, rettighetsPeriode: Periode): KanBehandlesAutomatiskVurdering{
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag)

        val forutgåendePeriode = Periode(rettighetsPeriode.fom.minusYears(5), rettighetsPeriode.tom)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag, forutgåendePeriode)

        val oppfyltMinstEttKrav = førsteDelVurderinger.any{it.resultat}
        val ingenInntruffet = andreDelVurdering.all{!it.resultat}

        return KanBehandlesAutomatiskVurdering(
            oppfyltMinstEttKrav && ingenInntruffet,
            førsteDelVurderinger + andreDelVurdering
        )
    }

    // Minst én må oppfylles
    private fun vurderFørsteDelKriteier(grunnlag: ForutgåendeMedlemskapGrunnlag): List<TilhørighetVurdering> {
        val arbeidInntektINorgeVurdering = harArbeidInntektINorge(grunnlag.medlemskapArbeidInntektGrunnlag)
        val vedtakIMedl = harVedtakIMEDL(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)

        return listOf(arbeidInntektINorgeVurdering, vedtakIMedl)
    }

    // Ingen kan inntreffe
    private fun vurderAndreDelKriterier(grunnlag: ForutgåendeMedlemskapGrunnlag, forutgåendePeriode: Periode): List<TilhørighetVurdering> {
        val harJobbetIUtland = oppgittJobbetIUtland(grunnlag.nyeSoknadGrunnlag, forutgåendePeriode )
        val harHattUtenlandsOpphold = oppgittUtenlandsOpphold(grunnlag.nyeSoknadGrunnlag, forutgåendePeriode)
        val harUtenlandsAdresse = utenlandskAdresse(grunnlag.personopplysningGrunnlag)
        val annetLovvalgsland = lovvalgslandIkkeErNorge(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)
        val utenforEØS = manglerStatsborgerskapIEØSiPerioden(grunnlag.personopplysningGrunnlag)

        return listOf(harJobbetIUtland, harHattUtenlandsOpphold, harUtenlandsAdresse, annetLovvalgsland, utenforEØS)
    }

    private fun oppgittJobbetIUtland(grunnlag: UtenlandsOppholdData?, rettighetsPeriode: Periode): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true)
        }
        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(it.fraDato))
        }
        val arbeidUtlandPerioder: MutableList<OppgittJobbetIUtlandGrunnlag> = mutableListOf()

        val jobbUtenforNorge = when {
            !grunnlag.harBoddINorgeSiste5År -> {
                val harRelevanteUtlandsPerioderIJobb = relevantePerioder?.any { it.iArbeid } == true
                    || (grunnlag.harArbeidetINorgeSiste5År && grunnlag.iTilleggArbeidUtenforNorge && relevantePerioder?.isNotEmpty() == true)

                if (harRelevanteUtlandsPerioderIJobb) {
                    val mappedArbeidUtland = relevantePerioder!!.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                    true
                } else {
                    false
                }
            }

            grunnlag.arbeidetUtenforNorgeFørSykdom -> {
                val mappedArbeidUtland = relevantePerioder?.map { OppgittJobbetIUtlandGrunnlag(it.land, it.fraDato, it.tilDato) }
                if (mappedArbeidUtland != null) {
                    arbeidUtlandPerioder.addAll(mappedArbeidUtland)
                }
                relevantePerioder?.isNotEmpty() == true
            }

            else -> false
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.SØKNAD),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Arbeidet i utlandet siste 5 år",
            resultat = jobbUtenforNorge,
            oppgittJobbetIUtlandGrunnlag = arbeidUtlandPerioder
        )
    }

    private fun oppgittUtenlandsOpphold(grunnlag: UtenlandsOppholdData?, rettighetsPeriode: Periode): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true)
        }

        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(it.fraDato)) && !it.iArbeid
        }

        val oppholdUtlandPerioder = relevantePerioder?.map {
            OppgittUtenlandsOppholdGrunnlag(
                land = it.land,
                tilDato = it.tilDato,
                fraDato = it.fraDato,
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.SØKNAD),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Opphold i utlandet siste 5 år",
            resultat = !grunnlag.harBoddINorgeSiste5År,
            oppgittUtenlandsOppholdGrunnlag = oppholdUtlandPerioder
        )
    }

    private fun utenlandskAdresse(grunnlag: PersonopplysningMedHistorikkGrunnlag): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag.brukerPersonopplysning.folkeregisterStatuser.any{it.status != PersonStatus.bosatt}

        val utenlandsAddresserGrunnlag = grunnlag.brukerPersonopplysning.utenlandsAddresser?.map {
            UtenlandsAdresseGrunnlag(
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed,
                adresseNavn = it.adresseNavn,
                postkode = it.postkode,
                bySted = it.bySted,
                landkode = it.landkode,
                adresseType = it.adresseType
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Har hatt utenlandsk adresse i perioden",
            resultat = bosattUtenforNorge || !utenlandsAddresserGrunnlag.isNullOrEmpty(),
            utenlandsAddresserGrunnlag = utenlandsAddresserGrunnlag
        )
    }

    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val lovvalgslandErIkkeNorge = grunnlag?.unntak?.firstOrNull{it.verdi.lovvalgsland != "NOR"}

        val medlGrunnlag = grunnlag?.unntak?.map {
            VedtakIMEDLGrunnlag(
                periode = Periode(it.periode.fom, it.periode.tom),
                lovvalgsland = it.verdi.lovvalgsland,
                grunnlag = it.verdi.grunnlag,
                kilde = it.verdi.kilde
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.MEDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Vedtak om annet lovvalgsland finnes",
            resultat = lovvalgslandErIkkeNorge != null,
            vedtakImedlGrunnlag = medlGrunnlag
        )
    }

    private fun manglerStatsborgerskapIEØSiPerioden(grunnlag: PersonopplysningMedHistorikkGrunnlag): TilhørighetVurdering {
        val fantStatsborgerskapUtenforEØSiPerioden = grunnlag.brukerPersonopplysning.statsborgerskap.any{it.land !in enumValues<EØSLand>().map { eøsLand -> eøsLand.name }}

        val manglerStatsborgerskapGrunnlag = grunnlag.brukerPersonopplysning.statsborgerskap.map {
            ManglerStatsborgerskapGrunnlag(
                land = it.land,
                gyldigFraOgMed = it.gyldigFraOgMed,
                gyldigTilOgMed = it.gyldigTilOgMed
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.PDL),
            indikasjon = Indikasjon.UTENFOR_NORGE,
            opplysning = "Har statsborgerskap utenfor EØS i perioden",
            resultat = fantStatsborgerskapUtenforEØSiPerioden,
            manglerStatsborgerskapGrunnlag = manglerStatsborgerskapGrunnlag
        )
    }

    private fun harArbeidInntektINorge(grunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        val eksistererArbeidsforhold = grunnlag?.arbeiderINorgeGrunnlag?.any() ?: false
        val opptjeningsLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.opptjeningsLand == EØSLand.NOR.toString()} ?: false
        val skattemessigBosattLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.skattemessigBosattLand == EØSLand.NOR.toString()} ?: false

        val harArbeidInntektINorge = skattemessigBosattLandErNorge
            || opptjeningsLandErNorge
            || eksistererArbeidsforhold

        val arbeidInntektINorgeGrunnlag = if (harArbeidInntektINorge) {
            grunnlag?.inntekterINorgeGrunnlag?.map {
                ArbeidInntektINorgeGrunnlag(
                    virksomhetId = it.identifikator,
                    beloep = it.beloep,
                    periode = Periode(it.periode.fom, it.periode.tom),
                )
            }
        } else null

        return TilhørighetVurdering(
            kilde = listOf(Kilde.A_INNTEKT, Kilde.AA_REGISTERET),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Arbeid og inntekt i Norge siste 5 år",
            resultat = harArbeidInntektINorge,
            arbeidInntektINorgeGrunnlag = arbeidInntektINorgeGrunnlag
        )
    }

    private fun harVedtakIMEDL(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val erMedlem = grunnlag?.unntak?.firstOrNull{it.verdi.medlem}

        val medlGrunnlag = grunnlag?.unntak?.map {
            VedtakIMEDLGrunnlag(
                periode = Periode(it.periode.fom, it.periode.tom),
                lovvalgsland = it.verdi.lovvalgsland,
                grunnlag = it.verdi.grunnlag,
                kilde = it.verdi.kilde
            )
        }

        return TilhørighetVurdering(
            kilde = listOf(Kilde.MEDL),
            indikasjon = Indikasjon.I_NORGE,
            opplysning = "Vedtak om pliktig eller frivillig medlemskap finnes i MEDL for perioden",
            resultat = erMedlem != null,
            vedtakImedlGrunnlag = medlGrunnlag
        )
    }
}