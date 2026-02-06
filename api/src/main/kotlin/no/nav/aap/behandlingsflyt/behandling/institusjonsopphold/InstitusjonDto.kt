package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class InstitusjonsoppholdDto(
    val oppholdId: String,
    val institusjonstype: String,
    val oppholdstype: String,
    val status: String,
    val oppholdFra: LocalDate,
    val avsluttetDato: LocalDate?,
    val kildeinstitusjon: String
) {
    companion object {
        fun institusjonToDto(institusjonsopphold: Segment<Institusjon>) =
            InstitusjonsoppholdDto(
                oppholdId = lagOppholdId(institusjonsopphold.verdi.navn, institusjonsopphold.periode.fom),
                institusjonstype = institusjonsopphold.verdi.type.beskrivelse,
                oppholdstype = institusjonsopphold.verdi.kategori.beskrivelse,
                status =
                    if (institusjonsopphold.tom() >
                        LocalDate.now()
                    ) {
                        StatusDto.AKTIV.toString()
                    } else {
                        StatusDto.AVSLUTTET.toString()
                    },
                // TODO skal muligens være start av rettighetsperiode i seteden for dd
                kildeinstitusjon = institusjonsopphold.verdi.navn,
                oppholdFra = institusjonsopphold.periode.fom,
                avsluttetDato = institusjonsopphold.periode.tom
            )
    }
}

data class HelseinstitusjonGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val opphold: List<InstitusjonsoppholdDto>,
    val vurderinger: List<HelseoppholdDto>,
    val vedtatteVurderinger: List<HelseoppholdDto>
)

data class HelseoppholdDto(
    val periode: Periode,
    val oppholdId: String,
    val vurderinger: List<HelseinstitusjonVurderingDto>?
)

data class HelseinstitusjonVurderingDto(
    val oppholdId: String,
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val periode: Periode,
    val vurdertAv: VurdertAvResponse?
)

data class SoningsGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val soningsforhold: List<InstitusjonsoppholdDto>,
    val vurderinger: List<SoningsforholdDto>,
    val vurdertAv: VurdertAvResponse?
)

data class SoningsforholdDto(
    val vurderingsdato: LocalDate,
    val vurdering: SoningsvurderingDto?,
    val status: OppholdVurderingDto
)

data class SoningsvurderingDto(
    val skalOpphøre: Boolean,
    val begrunnelse: String,
    val fraDato: LocalDate,
)

enum class StatusDto {
    AKTIV,
    AVSLUTTET
}

enum class OppholdVurderingDto {
    AVSLÅTT,
    GODKJENT,
    UAVKLART
}

fun OppholdVurdering.toDto() = when (this) {
    OppholdVurdering.AVSLÅTT -> OppholdVurderingDto.AVSLÅTT
    OppholdVurdering.GODKJENT -> OppholdVurderingDto.GODKJENT
    OppholdVurdering.UAVKLART -> OppholdVurderingDto.UAVKLART
}