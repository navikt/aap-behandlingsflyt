package no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv

import no.nav.aap.komponenter.gateway.Gateway

interface DokarkivGateway : Gateway {

    fun oppdater(
        journalpost: Journalpost,
        bruker: no.nav.aap.komponenter.verdityper.Bruker,
        forsøkFerdigstill: Boolean,
    ): JournalpostResponse
}