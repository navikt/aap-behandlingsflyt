package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

class Medlemskapvilkåret(vilkårsresultat: Vilkårsresultat): Vilkårsvurderer<MedlemskapLovvalgGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: MedlemskapLovvalgGrunnlag) {
        // Super service for vurdering (Servicen skal både DENNE og FE bruke for å få ut alle de vurderingene) -- INGEN DB
        // Krev 1
        /*
        Bruker søker AAP mens de mottar sykepenger (eller dagpenger) // SOKNAD/SP?
        Bruker har arbeidsforhold og inntekt i Norge (usikker på behov på grad?) Hvor lenge har arbeidsforholdet vart?
        Vedtak i MEDL om pliktig eller frivillig medlemskap (får perioder fra MEDL om hvilke type vedtak det er) // MEDL

        // Manuell om du treffer 1
        Hvis bruker oppgir at de jobber i utlandet
        Hvis bruker har oppgitt at de har oppholdt seg i utlandet (må oppgi hvilket land og for hvilken periode? Hvilke regler ønsker vi her?)
        Hvis bruker har en registrert adresse (uansett type) i utlandet / har utvandret status // !BOSATT -> MANUELL (PDL)
        Vedtak i MEDL om at lovvalgslandet ikke er Norge
        Hvis det "beste" statsborgerskapet er tredjeland, skal vi stoppe for manuell håndtering. Manuell prosess for sjekk mot UDI (oppholdstillatelse) - omformulering: Hvis man ikke har EØS statsborgerskap, så skal det være manuell håndtering
        */
    }

    private fun lagre(
        periode: Periode,
        grunnlag: MedlemskapLovvalgGrunnlag,
        vurderingsResultat: VurderingsResultat
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon()
            )
        )
    }
}