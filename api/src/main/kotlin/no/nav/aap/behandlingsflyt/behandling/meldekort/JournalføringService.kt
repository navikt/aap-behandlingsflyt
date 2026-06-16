package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.BrukerIdType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivBruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Dokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentVariant
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.FagsaksSystem
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Filetype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalposttype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.OverstyrInnsynsregler
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Sakstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Tema
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Variantformat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class JournalføringService(
    private val dokarkivGateway: DokarkivGateway,
    private val pdfgenGateway: PdfgenGateway,
) {
    constructor(gatewayProvider: GatewayProvider) : this(
        dokarkivGateway = gatewayProvider.provide(),
        pdfgenGateway = gatewayProvider.provide(),
    )

    fun journalfør(
        sak: Sak,
        meldeperiode: Periode,
        meldekort: MeldekortV0,
        oppdatertAv: Bruker,
        enhet: String,
        tidspunkt: Instant,
    ): JournalpostId {
        val meldekortPdfRequest = meldekort.tilPdfRequest(
            ident = sak.person.aktivIdent().identifikator,
            meldeperiode = meldeperiode,
            utførtAv = oppdatertAv.ident,
            tidspunkt = tidspunkt
        )

        val pdf = pdfgenGateway.genererMeldekortPdf(
            meldekortPdfRequest
        )

        val journalpost = journalpost(
            ident = sak.person.aktivIdent(),
            enhet = enhet,
            meldeperiode = meldeperiode,
            meldekort = meldekort,
            tidspunkt = tidspunkt,
            pdf = pdf,
            sak = sak,
        )

        val response = dokarkivGateway.oppdater(
            journalpost,
            oppdatertAv = oppdatertAv,
            forsøkFerdigstill = true
        )

        return JournalpostId(response.journalpostId.toString())
    }

    private fun journalpost(
        ident: Ident,
        enhet: String,
        meldeperiode: Periode,
        meldekort: Meldekort,
        tidspunkt: Instant,
        pdf: ByteArray,
        sak: Sak,
    ): Journalpost {
        val uke1 = meldeperiode.fom.get(uke)
        val uke2 = meldeperiode.tom.get(uke)
        val fra = meldeperiode.fom.format(dateFormatter)
        val til = meldeperiode.tom.format(dateFormatter)
        val tittelsuffix =
            "for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av NAV"
        val tittel = "Korrigert meldekort $tittelsuffix"

        return Journalpost(
            journalposttype = Journalposttype.NOTAT,
            bruker = DokarkivBruker(
                id = ident.identifikator,
                idType = BrukerIdType.FNR,
            ),
            sak = DokarkivSak(
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = FagsaksSystem.KELVIN,
                fagsakId = sak.saksnummer.toString()
            ),
            journalfoerendeEnhet = enhet,
            tema = Tema.AAP,
            tittel = tittel,
            eksternReferanseId = UUID.randomUUID().toString(),
            datoMottatt = tidspunkt.toString(),
            overstyrInnsynsregler = OverstyrInnsynsregler.VISES_MANUELT_GODKJENT,
            dokumenter = listOf(
                Dokument(
                    tittel = tittel,
                    brevkode = "NAV 00-10.03", // Korrigering
                    dokumentvarianter = listOf(
                        DokumentVariant(
                            filtype = Filetype.PDF,
                            variantformat = Variantformat.ARKIV,
                            fysiskDokument = pdf,
                        ),
                        DokumentVariant(
                            filtype = Filetype.JSON,
                            variantformat = Variantformat.ORIGINAL,
                            fysiskDokument = DefaultJsonMapper.toJson(meldekort).encodeToByteArray(),
                        )
                    ),
                )
            ),
        )
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val uke = WeekFields.of(Locale.of("nb", "NO")).weekOfWeekBasedYear()
    }
}