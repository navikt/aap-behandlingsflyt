package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.InntektTyper
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapUnntakGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.PersonopplysningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.komponenter.json.DefaultJsonMapper

class MedlemskapLovvalgService {
    fun vurderTilhørighet(grunnlag: MedlemskapLovvalgGrunnlag): KanBehandlesAutomatiskVurdering{
        val førsteDelVurderinger = vurderFørsteDelKriteier(grunnlag)
        val andreDelVurdering = vurderAndreDelKriterier(grunnlag)

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
    private fun vurderAndreDelKriterier(grunnlag: MedlemskapLovvalgGrunnlag): List<TilhørighetVurdering> {
        return listOf()
    }

    //SØKNAD
    private fun oppgittJobbetIUtland(grunnlag: UtenlandsOppholdData): TilhørighetVurdering {
        //Hvis bruker oppgir at de jobber i utlandet
        TODO()
    }

    //SØKNAD
    private fun oppgittUtenlandsOpphold(grunnlag: UtenlandsOppholdData): TilhørighetVurdering {
        //Hvis bruker har oppgitt at de har oppholdt seg i utlandet(må oppgi hvilket land og for hvilken periode ? Hvilke regler ønsker vi her ? )
        TODO()
    }

    //PDL (MÅ SJEKKE OM PERIODE ER BRA NOK)
    private fun utelandskAdresse(grunnlag: PersonopplysningGrunnlag): TilhørighetVurdering {
        // Hvis bruker har en registrert adresse (uansett type) i utlandet / har utvandret status // !BOSATT -> MANUELL (PDL)
        TODO()
    }

    //MEDL
    private fun lovvalgslandIkkeErNorge(grunnlag: MedlemskapUnntakGrunnlag): TilhørighetVurdering {
        //Vedtak i MEDL om at lovvalgslandet ikke er Norge
        TODO()
    }

    //PDL (MÅ SJEKKE OM PERIODE ER BRA NOK)
    private fun manglerStatsborgerskapIEØS(grunnlag: PersonopplysningGrunnlag): TilhørighetVurdering {
        //Hvis man ikke har EØS statsborgerskap, så skal det være manuell håndtering
        TODO()
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
        // orgnummer må være samme...hva med periode?
        /*val aaregData = grunnlag?.arbeiderINorgeGrunnlag?.first.identifikator
        val ainntektData = grunnlag?.inntekterINorgeGrunnlag?.first.identifikator
        val fleredata1 = grunnlag?.inntekterINorgeGrunnlag?.first.opptjeningsLand
        val fleredata2 = grunnlag?.inntekterINorgeGrunnlag?.first.skattemessigBosattLand


        val arbeidInntektINorgeGrunnlag = grunnlag?.let { DefaultJsonMapper.toJson(it) } // TODO: Her må vi faktisk lande hva vi vil ha ut
        return TilhørighetVurdering(listOf(Kilde.A_INNTEKT, Kilde.AA_REGISTERET), Indikasjon.I_NORGE, "Arbeid og inntekt i Norge", )
*/
        TODO()
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