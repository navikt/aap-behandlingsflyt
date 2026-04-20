package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentInfo
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.JournalpostResponse
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.verdityper.Bruker
import java.util.concurrent.atomic.AtomicLong

class FakeDokarkivGateway : DokarkivGateway {
    private val idSequence = AtomicLong(1)

    override fun oppdater(
        journalpost: Journalpost,
        bruker: Bruker,
        forsøkFerdigstill: Boolean,
    ): JournalpostResponse {
        val id = idSequence.getAndIncrement()
        return JournalpostResponse(
            journalpostId = id,
            melding = null,
            journalpostferdigstilt = forsøkFerdigstill,
            dokumenter = listOf(DokumentInfo(dokumentInfoId = id))
        )
    }

    companion object : Factory<DokarkivGateway> {
        override fun konstruer(): DokarkivGateway = FakeDokarkivGateway()
    }
}
