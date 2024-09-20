package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

interface Informasjonskrav {
    fun harIkkeGjortOppdateringNå(kontekst: FlytKontekstMedPerioder): Boolean
}
