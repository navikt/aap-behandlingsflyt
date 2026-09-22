package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder

interface MigrerVurderingFraArena {
    fun migrerVurderingFraArena(kontekst: FlytKontekstMedPerioder): Unit
}
