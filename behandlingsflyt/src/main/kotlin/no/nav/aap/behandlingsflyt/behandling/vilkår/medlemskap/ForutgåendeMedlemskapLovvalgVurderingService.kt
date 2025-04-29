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
        val harHattUtenlandsOpphold = oppgittUtenlandsOpphold(grunnlag.nyeSoknadGrunnlag)
        val harUtenlandsAdresse = utenlandskAdresse(grunnlag.personopplysningGrunnlag)
        val annetLovvalgsland = lovvalgslandIkkeErNorge(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)
        val utenforEØS = manglerStatsborgerskapIEØSiPerioden(grunnlag.personopplysningGrunnlag)

        return listOf(harJobbetIUtland, harHattUtenlandsOpphold, harUtenlandsAdresse, annetLovvalgsland, utenforEØS)
    }

    private fun oppgittJobbetIUtland(grunnlag: UtenlandsOppholdData?, rettighetsPeriode: Periode): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true, "Mangler utenlandsdata fra søknad")
        }
        val relevantePerioder = grunnlag.utenlandsOpphold?.filter {
            (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(it.fraDato))
        }

        val jobbUtenforNorge = when {
            !grunnlag.harBoddINorgeSiste5År ->
                relevantePerioder?.any { it.iArbeid } == true
                    || (grunnlag.harArbeidetINorgeSiste5År && grunnlag.iTilleggArbeidUtenforNorge && relevantePerioder?.isNotEmpty() == true)

            grunnlag.arbeidetUtenforNorgeFørSykdom ->
                relevantePerioder?.isNotEmpty() == true

            else -> false
        }
        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag) // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Arbeidet i utlandet siste 5 år", jobbUtenforNorge, jsonGrunnlag)
    }

    private fun oppgittUtenlandsOpphold(grunnlag: UtenlandsOppholdData?): TilhørighetVurdering {
        if (grunnlag == null) {
            return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Mangler utenlandsdata fra søknad", true, "Mangler utenlandsdata fra søknad")
        }
        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag) // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Opphold i utlandet siste 5 år", !grunnlag.harBoddINorgeSiste5År, jsonGrunnlag)
    }

    private fun utenlandskAdresse(grunnlag: PersonopplysningMedHistorikkGrunnlag): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag.brukerPersonopplysning.folkeregisterStatuser.any{it.status != PersonStatus.bosatt}
        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag)
        return TilhørighetVurdering(listOf(Kilde.PDL), Indikasjon.UTENFOR_NORGE, "Har hatt utenlandsk adresse i perioden", bosattUtenforNorge, jsonGrunnlag)
    }

    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val lovvalgslandErIkkeNorge = grunnlag?.unntak?.firstOrNull{it.verdi.lovvalgsland != "NOR"}
        val jsonGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) }
        return TilhørighetVurdering(listOf(Kilde.MEDL), Indikasjon.UTENFOR_NORGE, "Vedtak om annet lovvalgsland finnes", lovvalgslandErIkkeNorge != null, jsonGrunnlag)
    }

    private fun manglerStatsborgerskapIEØSiPerioden(grunnlag: PersonopplysningMedHistorikkGrunnlag): TilhørighetVurdering {
        val fantStatsborgerskapUtenforEØSiPerioden = grunnlag.brukerPersonopplysning.statsborgerskap.any{it.land !in enumValues<EØSLand>().map { eøsLand -> eøsLand.name }}
        val jsonGrunnlag = grunnlag.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.PDL), Indikasjon.UTENFOR_NORGE, "Har statsborgerskap utenfor EØS i perioden", fantStatsborgerskapUtenforEØSiPerioden, jsonGrunnlag)
    }

    private fun harArbeidInntektINorge(grunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        val eksistererArbeidsforhold = grunnlag?.arbeiderINorgeGrunnlag?.any() ?: false
        val opptjeningsLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.opptjeningsLand == EØSLand.NOR.toString()} ?: false
        val skattemessigBosattLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.skattemessigBosattLand == EØSLand.NOR.toString()} ?: false

        val harArbeidInntektINorge = skattemessigBosattLandErNorge
            || opptjeningsLandErNorge
            || eksistererArbeidsforhold
        val jsonGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut

        return TilhørighetVurdering(listOf(Kilde.A_INNTEKT, Kilde.AA_REGISTERET), Indikasjon.I_NORGE, "Arbeid og inntekt i Norge siste 5 år", harArbeidInntektINorge, jsonGrunnlag)
    }

    private fun harVedtakIMEDL(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val erMedlem = grunnlag?.unntak?.firstOrNull{it.verdi.medlem}
        val medlemskapINorgeGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) }
        return TilhørighetVurdering(listOf(Kilde.MEDL), Indikasjon.I_NORGE, "Vedtak om pliktig eller frivillig medlemskap finnes i MEDL for perioden", erMedlem != null, medlemskapINorgeGrunnlag)
    }
}