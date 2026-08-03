package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "kravType",
)
sealed class KravVurderingLøsningDto(
    val kravType: KravType,
) {
    fun tilVurdering(
        behandlingId: BehandlingId,
        bruker: Bruker,
        opprettetTid: Instant,
    ): KravVurdering {
        return when (this) {
            is RelevantKravLøsningDto -> RelevantKrav(
                referanse = referanse?.let(::Kravreferanse) ?: Kravreferanse.ny(),
                journalpostId = journalpostId,
                vurdertAv = bruker,
                begrunnelse = begrunnelse,
                vurdertIBehandling = behandlingId,
                opprettet = opprettetTid,
                søknadsdato = søknadsdato,
                overstyrMuligRettFra = overstyrMuligRettFra,
                muligRettFra = muligRettFra()
            )

            is TilleggsopplysningKravLøsningDto -> Tilleggsopplysning(
                referanse = referanse?.let(::Kravreferanse) ?: Kravreferanse.ny(),
                journalpostId = journalpostId,
                vurdertAv = bruker,
                begrunnelse = begrunnelse,
                vurdertIBehandling = behandlingId,
                opprettet = opprettetTid,
            )

            is TrukketSøknadKravLøsningDto, is KlageKravLøsningDto -> throw UgyldigForespørselException(
                "Kelvin støtter foreløpig ikke ${this.kravType}."
            )
        }

    }
}

interface KravMedDatoDto {
    val søknadsdato: Søknadsdato
    val overstyrMuligRettFra: OverstyrMuligRettFra?
}

@JsonTypeName(value = "RELEVANT_KRAV")
data class RelevantKravLøsningDto(
    val referanse: UUID? = null,
    val journalpostId: JournalpostId,
    val begrunnelse: String,
    override val søknadsdato: Søknadsdato,
    override val overstyrMuligRettFra: OverstyrMuligRettFra?,
) : KravVurderingLøsningDto(KravType.RELEVANT_KRAV), KravMedDatoDto {
    fun muligRettFra(): LocalDate {
        if (overstyrMuligRettFra != null) {
            return overstyrMuligRettFra.dato
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
