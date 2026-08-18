package no.nav.aap.behandlingsflyt.behandling.journalføring.journalposter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.BrukerIdType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivBruker
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Dokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokumentVariant
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.FagsaksSystem
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Filetype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Journalposttype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Sakstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Tema
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Variantformat
import no.nav.aap.behandlingsflyt.dokumentasjon.vilkårsvurderingOppsummeringTittel
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import java.util.UUID

internal fun vilkårsvurderingOppsummeringJournalpost(
    sak: Sak,
    pdf: ByteArray
): Journalpost {
    val tittel = vilkårsvurderingOppsummeringTittel(sak.saksnummer)

    return Journalpost(
        journalposttype = Journalposttype.NOTAT,
        bruker = DokarkivBruker(
            id = sak.person.aktivIdent().identifikator,
            idType = BrukerIdType.FNR,
        ),
        sak = DokarkivSak(
            sakstype = Sakstype.FAGSAK,
            fagsaksystem = FagsaksSystem.KELVIN,
            fagsakId = sak.saksnummer.toString(),
        ),
        journalfoerendeEnhet = "9999",
        tema = Tema.AAP,
        tittel = tittel,
        eksternReferanseId = UUID.randomUUID().toString(),
        dokumenter = listOf(
            Dokument(
                tittel = tittel,
                dokumentvarianter = listOf(
                    DokumentVariant(
                        filtype = Filetype.PDF,
                        variantformat = Variantformat.ARKIV,
                        fysiskDokument = pdf,
                    )
                ),
            )
        ),
    )
}
