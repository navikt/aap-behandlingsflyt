package no.nav.aap.behandlingsflyt.behandling.journalføring

import no.nav.aap.behandlingsflyt.behandling.meldekort.PdfgenGateway
import no.nav.aap.behandlingsflyt.behandling.meldekort.tilPdfRequest
import no.nav.aap.behandlingsflyt.behandling.journalføring.journalposter.meldekortJournalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate

class JournalføringService(
    private val dokarkivGateway: DokarkivGateway,
    private val pdfgenGateway: PdfgenGateway,
) {
    constructor(gatewayProvider: GatewayProvider) : this(
        dokarkivGateway = gatewayProvider.provide(),
        pdfgenGateway = gatewayProvider.provide(),
    )

    fun journalførMeldekort(
        sak: Sak,
        meldeperiode: Periode,
        meldekort: MeldekortV0,
        oppdatertAv: Bruker,
        enhet: String?,
        tidspunkt: Instant,
        meldeDato: LocalDate,
        korrigert: Boolean,
    ): JournalpostId {
        val pdf = pdfgenGateway.genererMeldekortPdf(
            meldekort.tilPdfRequest(
                ident = sak.person.aktivIdent().identifikator,
                meldeperiode = meldeperiode,
                utførtAv = oppdatertAv.ident,
                tidspunkt = tidspunkt,
                meldeDato = meldeDato,
                korrigert = korrigert,
            )
        )

        return journalfør(
            oppdatertAv = oppdatertAv,
            journalpost = meldekortJournalpost(
                sak = sak,
                meldeperiode = meldeperiode,
                meldekort = meldekort,
                enhet = enhet ?: "9999",
                pdf = pdf,
                korrigert = korrigert,
            )
        )
    }

    private fun journalfør(
        journalpost: Journalpost,
        oppdatertAv: Bruker,
        forsøkFerdigstill: Boolean = true
    ): JournalpostId {
        val response = dokarkivGateway.oppdater(
            journalpost = journalpost,
            oppdatertAv = oppdatertAv,
            forsøkFerdigstill = forsøkFerdigstill
        )

        return JournalpostId(response.journalpostId.toString())
    }
}
