package no.nav.aap.behandlingsflyt.behandling.meldekort

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ArbeidIPeriodeV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Meldekort
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

class JournalføringService(
    private val dokarkivGateway: DokarkivGateway,
) {
    constructor(gatewayProvider: GatewayProvider): this(
        dokarkivGateway = gatewayProvider.provide()
    )

    fun journalfør(
        sak: Sak,
        meldekort: MeldekortV0,
    ): JournalpostId {
        val pdf = "".toByteArray() // TODO må få inn pdfgen her

        val journalpost = journalpost(
            ident = sak.person.aktivIdent(),
            meldekort = meldekort,
            pdf = pdf,
            sak = sak,
        )

        val response = dokarkivGateway.oppdater(
            journalpost,
            forsøkFerdigstill = false // postmottak vil lese denne og behandle den på lik linje som andre meldekort
        )

        return JournalpostId(response.journalpostId.toString())
    }

    private fun journalpost(
        ident: Ident,
        meldekort: Meldekort,
        pdf: ByteArray,
        sak: Sak,
    ): DokarkivGateway.Journalpost {
        // TODO trenger meldeperiode for å få dette riktig?
        val uke1 = meldekort.fom()!!.get(uke)
        val uke2 = meldekort.tom()!!.get(uke)
        val fra = meldekort.fom()!!.format(dateFormatter)
        val til = meldekort.tom()!!.format(dateFormatter)
        val tittelsuffix = "for uke $uke1 - $uke2 ($fra - $til) elektronisk mottatt av NAV"
        val tittel = "Korrigert meldekort $tittelsuffix"

        val opprettet = Instant.now()

        return DokarkivGateway.Journalpost(
            journalposttype = DokarkivGateway.Journalposttype.NOTAT,
            bruker = DokarkivGateway.Bruker(
                id = ident.identifikator,
                idType = DokarkivGateway.BrukerIdType.FNR,
            ),
            tema = DokarkivGateway.Tema.AAP,
            tittel = tittel,
            datoMottatt = opprettet.toString(),
            sak = DokarkivGateway.Sak(
                sakstype = DokarkivGateway.Sakstype.FAGSAK,
                fagsaksystem = DokarkivGateway.FagsaksSystem.KELVIN,
                fagsakId = sak.saksnummer.toString()
            ),
            dokumenter = listOf(
                DokarkivGateway.Dokument(
                    tittel = tittel,
                    brevkode = "NAV 00-10.03", // Korrigering
                    dokumentvarianter = listOf(
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.PDF,
                            variantformat = DokarkivGateway.Variantformat.ARKIV,
                            fysiskDokument = pdf,
                        ),
                        DokarkivGateway.DokumentVariant(
                            filtype = DokarkivGateway.Filetype.JSON,
                            variantformat = DokarkivGateway.Variantformat.ORIGINAL,
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