package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

interface InformasjonskravGrunnlag {

    fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekstMedPerioder
    ): List<Informasjonskravkonstruktør>
}
