package no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.verdityper.Bruker

interface DokarkivGateway : Gateway {

    fun oppdater(
        journalpost: Journalpost,
        oppdatertAv: Bruker,
        forsøkFerdigstill: Boolean,
    ): JournalpostResponse
}