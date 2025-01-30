package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektTyper
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode

class MedlemskapLovvalgService {
    fun vurderTilhørighet(grunnlag: MedlemskapLovvalgGrunnlag, rettighetsPeriode: Periode): KanBehandlesAutomatiskVurdering{
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag, rettighetsPeriode)

        val oppfyltMinstEttKrav = førsteDelVurderinger.any{it.resultat}
        val ingenInntruffet = andreDelVurdering.all{!it.resultat}

        return KanBehandlesAutomatiskVurdering(
            oppfyltMinstEttKrav && ingenInntruffet,
            førsteDelVurderinger + andreDelVurdering
        )
    }

    // Minst én må oppfylles
    private fun vurderFørsteDelKriteier(grunnlag: MedlemskapLovvalgGrunnlag): List<TilhørighetVurdering> {
        val mottarSykepengerVurdering = mottarSykepenger(grunnlag.medlemskapArbeidInntektGrunnlag)
        val arbeidInntektINorgeVurdering = harArbeidInntektINorge(grunnlag.medlemskapArbeidInntektGrunnlag)
        val vedtakIMedl = harVedtakIMEDL(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)

        return listOf(mottarSykepengerVurdering, arbeidInntektINorgeVurdering, vedtakIMedl)
    }

    // Ingen kan inntreffe
    private fun vurderAndreDelKriterier(grunnlag: MedlemskapLovvalgGrunnlag, rettighetsPeriode: Periode): List<TilhørighetVurdering> {
        val harJobbetIUtland = oppgittJobbetIUtland(grunnlag.nyeSoknadGrunnlag!!, rettighetsPeriode )
        val harHattUtenlandsOpphold = oppgittUtenlandsOpphold(grunnlag.nyeSoknadGrunnlag!!, rettighetsPeriode)
        val harUtenlandsAdresse = utenlandskAdresse(grunnlag.personopplysningGrunnlag)
        val annetLovvalgsland = lovvalgslandIkkeErNorge(grunnlag.medlemskapArbeidInntektGrunnlag?.medlemskapGrunnlag)
        val utenforEØS = manglerStatsborgerskapIEØS(grunnlag.personopplysningGrunnlag)

        return listOf(harJobbetIUtland, harHattUtenlandsOpphold, harUtenlandsAdresse, annetLovvalgsland, utenforEØS)
    }

    private fun oppgittJobbetIUtland(grunnlag: UtenlandsOppholdData, rettighetsPeriode: Periode): TilhørighetVurdering {
        val arbeidUtlandIRelevantPeriode = grunnlag.utenlandsOpphold?.filter {
            it.iArbeid && (
                (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(it.fraDato))
            )
        }
        val arbeidetUtenforNorge = grunnlag.iTilleggArbeidUtenforNorge || grunnlag.arbeidetUtenforNorgeFørSykdom || !arbeidUtlandIRelevantPeriode.isNullOrEmpty()
        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag) // TODO: Her må vi faktisk lande hva vi vil ha ut

        return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Arbeid i utland", arbeidetUtenforNorge, jsonGrunnlag)
    }

    private fun oppgittUtenlandsOpphold(grunnlag: UtenlandsOppholdData, rettighetsPeriode: Periode): TilhørighetVurdering {
        val arbeidUtlandIRelevantPeriode = grunnlag.utenlandsOpphold?.filter {
            it.iArbeid && (
                (it.tilDato != null && rettighetsPeriode.inneholder(it.tilDato)) || (it.fraDato != null && rettighetsPeriode.inneholder(it.fraDato))
            )
        }

        val fantUtenlandsOpphold = grunnlag.arbeidetUtenforNorgeFørSykdom
            || grunnlag.iTilleggArbeidUtenforNorge
            || !grunnlag.harBoddINorgeSiste5År
            || !arbeidUtlandIRelevantPeriode.isNullOrEmpty()

        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag) // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.SØKNAD), Indikasjon.UTENFOR_NORGE, "Opphold i utland", fantUtenlandsOpphold, jsonGrunnlag)
    }

    private fun utenlandskAdresse(grunnlag: PersonopplysningGrunnlag): TilhørighetVurdering {
        val bosattUtenforNorge = grunnlag.brukerPersonopplysning.status != PersonStatus.bosatt
        val jsonGrunnlag = DefaultJsonMapper.toJson(grunnlag)
        return TilhørighetVurdering(listOf(Kilde.PDL), Indikasjon.UTENFOR_NORGE, "Utenlandsk adresse", bosattUtenforNorge, jsonGrunnlag)
    }

    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val lovvalgslandErIkkeNorge = grunnlag?.unntak?.firstOrNull{it.verdi.lovvalgsland != "NOR"}
        val jsonGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.MEDL), Indikasjon.UTENFOR_NORGE, "Vedtak om annet lovvalgsland finnes", lovvalgslandErIkkeNorge != null, jsonGrunnlag)
    }

    private fun manglerStatsborgerskapIEØS(grunnlag: PersonopplysningGrunnlag): TilhørighetVurdering {
        val manglerEØS = grunnlag.brukerPersonopplysning.land !in enumValues<EØSLand>().map { it.name }
        val jsonGrunnlag = grunnlag.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.PDL), Indikasjon.UTENFOR_NORGE, "Mangler statsborgerskal i EØS", manglerEØS, jsonGrunnlag)
    }

    private fun mottarSykepenger(grunnlag: MedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering{
        val mottarSykepenger = grunnlag?.inntekterINorgeGrunnlag?.firstOrNull{
                inntekt -> inntekt.inntektType?.uppercase() in enumValues<InntektTyper>().map { it.name }
        }
        val inntekterINorgeGrunnlag = grunnlag?.inntekterINorgeGrunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.A_INNTEKT), Indikasjon.I_NORGE, "Mottar sykepenger", mottarSykepenger != null, inntekterINorgeGrunnlag)
    }

    // TODO: SKAL DENNE SPLITTES I TO?
    private fun harArbeidInntektINorge(grunnlag: MedlemskapArbeidInntektGrunnlag?): TilhørighetVurdering {
        val eksistererArbeidsforhold = grunnlag?.arbeiderINorgeGrunnlag?.any() ?: false // TODO: Skal vi sammenligne denne med org-nr i inntekt?

        val opptjeningsLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.opptjeningsLand == EØSLand.NOR.toString()} ?: false
        val skattemessigBosattLandErNorge = grunnlag?.inntekterINorgeGrunnlag?.any{it.skattemessigBosattLand == EØSLand.NOR.toString()} ?: false

        val harArbeidInntektINorge = skattemessigBosattLandErNorge
            || opptjeningsLandErNorge
            || eksistererArbeidsforhold
        val jsonGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut

        return TilhørighetVurdering(listOf(Kilde.A_INNTEKT, Kilde.AA_REGISTERET), Indikasjon.I_NORGE, "Arbeid og inntekt i Norge", harArbeidInntektINorge, jsonGrunnlag)
    }

    private fun harVedtakIMEDL(grunnlag: MedlemskapUnntakGrunnlag?): TilhørighetVurdering {
        val erMedlem = grunnlag?.unntak?.firstOrNull{it.verdi.medlem}
        val medlemskapINorgeGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.MEDL), Indikasjon.I_NORGE, "Vedtak om pliktig eller frivillig medlemskap finnes i MEDL", erMedlem != null, medlemskapINorgeGrunnlag)
    }
}

data class KanBehandlesAutomatiskVurdering(
    val kanBehandlesAutomatisk: Boolean,
    val tilhørighetVurdering: List<TilhørighetVurdering>
)

data class TilhørighetVurdering (
    val kilde: List<Kilde>,
    val indikasjon: Indikasjon,
    val opplysning: String,
    val resultat: Boolean,
    val fordypelse: String?
)

enum class Kilde {
    SØKNAD, PDL, MEDL, AA_REGISTERET, A_INNTEKT
}

enum class Indikasjon {
    I_NORGE, UTENFOR_NORGE
}

// TODO: Dette er usunne greier, finn ut hvor vi finner dette på en fornuftig måte
enum class EØSLand{
    BEL,
    BGR,
    DNK,
    EST,
    FIN,
    FRA,
    GRC,
    IRL,
    ISL,
    ITA,
    HRV,
    CYP,
    LVA,
    LIE,
    LTU,
    LUX,
    MLT,
    NLD,
    NOR,
    POL,
    PRT,
    ROU,
    SVK,
    SVN,
    ESP,
    CHE,
    SWE,
    CZE,
    DEU,
    HUN,
    AUT,
}