package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface Informasjonskrav {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }

    fun oppdater(kontekst: FlytKontekstMedPerioder): Endret
}
