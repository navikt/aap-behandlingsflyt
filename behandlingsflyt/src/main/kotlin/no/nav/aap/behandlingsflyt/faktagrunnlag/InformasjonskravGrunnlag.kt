package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface InformasjonskravGrunnlag {
    /**
     * @param kravkonstruktører En liste med [Informasjonskrav] som skal oppdateres.
     * @param kontekst Den gjeldende flytkonteksten.
     * @return En liste over informasjonskrav som har endret seg siden forrige kall.
     */
    fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør>

    fun flettOpplysningerFraAtomærBehandling(
        kontekst: FlytKontekst,
        informasjonskravkonstruktørere: List<Informasjonskravkonstruktør>
    ): List<Informasjonskravkonstruktør>
}
