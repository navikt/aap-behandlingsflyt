package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.util.UUID

sealed class KravVurderingLøsningDto(
    val kravType: KravType
)

data class NyttKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val søknadsdato: Søknadsdato,
    val overstyrMuligRettFra: OverstyrMuligRettFra?,
) : KravVurderingLøsningDto(KravType.NYTT_KRAV_AAP) {
    fun muligRettFra(): LocalDate {
        if (overstyrMuligRettFra != null) {
            return overstyrMuligRettFra.dato
        }
        return søknadsdato.dato
    }
}

data class GjenopptakKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    val søknadsdato: Søknadsdato,
    val muligRettFra: OverstyrMuligRettFra?,
) : KravVurderingLøsningDto(KravType.GJENOPPTAK) {
    fun muligRettFra(): LocalDate {
        if (muligRettFra != null) {
            return muligRettFra.dato
        }
        return søknadsdato.dato
    }
}

data class TrukketSøknadKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TRUKKET_SØKNAD)

data class KlageKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.KLAGE)

data class TilleggsopplysningKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TILLEGGSOPPLYSNING)
