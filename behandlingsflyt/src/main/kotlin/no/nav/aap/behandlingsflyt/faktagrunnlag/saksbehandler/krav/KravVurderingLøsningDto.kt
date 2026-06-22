package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "kravType",
)
sealed class KravVurderingLøsningDto(
    val kravType: KravType,
)

@JsonTypeName(value = "NYTT_KRAV_AAP")
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

@JsonTypeName(value = "GJENOPPTAK")
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

@JsonTypeName(value = "TRUKKET_SØKNAD")
data class TrukketSøknadKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TRUKKET_SØKNAD)

@JsonTypeName(value = "KLAGE")
data class KlageKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.KLAGE)

@JsonTypeName(value = "TILLEGGSOPPLYSNING")
data class TilleggsopplysningKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
) : KravVurderingLøsningDto(KravType.TILLEGGSOPPLYSNING)
