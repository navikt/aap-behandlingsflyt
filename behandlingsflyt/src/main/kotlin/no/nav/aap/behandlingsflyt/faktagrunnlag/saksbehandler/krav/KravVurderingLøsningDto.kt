package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate


sealed class KravVurderingLøsningDto(
    val kravType: KravType
)

data class NyttKravLøsningDto(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val søknadsdato: Søknadsdato,
    val muligRettFra: MuligRettFra?,
) : KravVurderingLøsningDto(KravType.NYTT_KRAV_AAP) {
    fun kravDato(): LocalDate {
        if (muligRettFra != null) {
            return muligRettFra.dato
        }
        return søknadsdato.dato
    }
}

data class GjenopptakKravLøsningDto(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val søknadsdato: Søknadsdato,
    val muligRettFra: MuligRettFra?,
) : KravVurderingLøsningDto(KravType.GJENOPPTAK) {
    fun kravDato(): LocalDate {
        if (muligRettFra != null) {
            return muligRettFra.dato
        }
        return søknadsdato.dato
    }
}

data class TrukketSøknadKravLøsningDto(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TRUKKET_SØKNAD)

data class KlageKravLøsningDto(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.KLAGE)

data class TilleggsopplysningKravLøsningDto(
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TILLEGGSOPPLYSNING)
