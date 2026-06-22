package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentInfo
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.JournalpostResponse
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.verdityper.Bruker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class FakeDokarkivGateway : DokarkivGateway {

    override fun oppdater(
        journalpost: Journalpost,
        oppdatertAv: Bruker,
        forsøkFerdigstill: Boolean,
    ): JournalpostResponse {
        val id = idSequence.getAndIncrement()
        journalposter[id] = journalpost
        return JournalpostResponse(
            journalpostId = id,
            melding = null,
            journalpostferdigstilt = forsøkFerdigstill,
            dokumenter = listOf(DokumentInfo(dokumentInfoId = id))
        )
    }

    companion object : Factory<DokarkivGateway> {
        private val idSequence = AtomicLong(1)

        val journalposter: MutableMap<Long, Journalpost> = ConcurrentHashMap()

        fun nullstillJournalposter() {
            journalposter.clear()
            idSequence.set(1)
        }

        override fun konstruer(): DokarkivGateway = FakeDokarkivGateway()
    }
}
