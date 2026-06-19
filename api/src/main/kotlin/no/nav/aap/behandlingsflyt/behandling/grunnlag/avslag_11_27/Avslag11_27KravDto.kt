package no.nav.aap.behandlingsflyt.behandling.grunnlag.avslag_11_27

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.Avslag11_27KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import java.time.LocalDate

data class Avslag11_27KravDto(
    val referanse: String,
    val søknadsdokument: String,
    val type: String,
    val søknadsdato: LocalDate,
    val muligRettighetFra: LocalDate
) {
    companion object {
        fun avslag11_27TilDto(kravListe: List<NyttKrav>): List<Avslag11_27KravDto> {
            return kravListe.map { krav ->
                Avslag11_27KravDto(
                    referanse = krav.referanse.verdi.toString(),
                    søknadsdokument = krav.journalpostId.identifikator,
                    type = KravType.NYTT_KRAV_AAP.name,
                    søknadsdato = krav.søknadsdato.dato,
                    muligRettighetFra = krav.muligRettFra,
                )
            }
        }
    }
}

data class Avslag11_27GrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val krav: List<Avslag11_27KravDto>,
    val vurderinger: List<Avslag11_27VurderingDto>?,
    val vedtatteVurdering: List<Avslag11_27VurderingDto>?,
)

data class Avslag11_27VurderingDto(
    val referanse: String,
    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    val harSykepengegrunnlagOver2G: Boolean? = null, // Kun for sykepenger
    val skalAvslås1127: Boolean,
    val vurderingerMeta: VurderingerMetaResponse?,
)

enum class Avslag11_27KravVurderingDto {
    AVSLÅTT,
    GODKJENT
}

fun Avslag11_27KravVurdering.toDto() = when (this) {
    Avslag11_27KravVurdering.AVSLÅTT -> Avslag11_27KravVurderingDto.AVSLÅTT
    Avslag11_27KravVurdering.GODKJENT -> Avslag11_27KravVurderingDto.GODKJENT
}
