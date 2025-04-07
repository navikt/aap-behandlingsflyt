package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface InformasjonskravGrunnlag {
    /**
     * @param kravliste En liste med [Informasjonskrav] som skal oppdateres.
     * @param kontekst Den gjeldende flytkonteksten.
     * @return En liste over informasjonskrav som har endret seg siden forrige kall.
     */
    fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør>
}
