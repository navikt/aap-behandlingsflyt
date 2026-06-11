package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument

object KravValidering {
    fun erKravVurderingTilstrekkeligVurdert(søknaderIBehandling: Set<MottattDokument>, kravVurderinger: Set<KravVurdering>): Boolean {
        val erAlleSøknaderIBehandlingVurdert =
            søknaderIBehandling.all { søknad -> kravVurderinger.any { it.journalpostId == søknad.referanse.asJournalpostId } }

        return erAlleSøknaderIBehandlingVurdert
    }
}