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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.OverstyrInnsynsregler
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Sakstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Tema
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.Variantformat
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.MeldekortV0
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.UUID

internal fun meldekortJournalpost(
    sak: Sak,
    meldeperiode: Periode,
    meldekort: MeldekortV0,
    enhet: String,
    pdf: ByteArray,
    korrigert: Boolean,
): Journalpost {
    val uke1 = meldeperiode.fom.get(uke)
    val uke2 = meldeperiode.tom.get(uke)
    val fra = meldeperiode.fom.format(dateFormatter)
    val til = meldeperiode.tom.format(dateFormatter)
    val prefix = if (korrigert) "Korrigert meldekort" else "Meldekort"
    val tittel = "$prefix for uke $uke1 - $uke2 ($fra - $til)"

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
        journalfoerendeEnhet = enhet,
        tema = Tema.AAP,
        tittel = tittel,
        eksternReferanseId = UUID.randomUUID().toString(),
        // Overstyrer for å vise notat på Mine AAP
        overstyrInnsynsregler = OverstyrInnsynsregler.VISES_MANUELT_GODKJENT,
        dokumenter = listOf(
            Dokument(
                tittel = tittel,
                brevkode = if (korrigert) "NAV 00-10.03" else "NAV 00-10.02",
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
                    ),
                ),
            ),
        ),
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val uke = WeekFields.of(Locale.of("nb", "NO")).weekOfWeekBasedYear()
