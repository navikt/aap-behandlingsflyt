package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

interface Informasjonskrav {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }

    fun oppdater(kontekst: FlytKontekstMedPerioder): Endret
}
