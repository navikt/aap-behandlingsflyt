package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.verdityper.Bruker
import java.util.concurrent.atomic.AtomicLong

class FakeDokarkivGateway : DokarkivGateway {
    private val idSequence = AtomicLong(1)

    override fun oppdater(
        journalpost: DokarkivGateway.Journalpost,
        bruker: Bruker,
        forsøkFerdigstill: Boolean,
    ): DokarkivGateway.JournalpostResponse {
        val id = idSequence.getAndIncrement()
        return DokarkivGateway.JournalpostResponse(
            journalpostId = id,
            melding = null,
            journalpostferdigstilt = forsøkFerdigstill,
            dokumenter = listOf(DokarkivGateway.DokumentInfo(dokumentInfoId = id))
        )
    }

    companion object : Factory<DokarkivGateway> {
        override fun konstruer(): DokarkivGateway = FakeDokarkivGateway()
    }
}
