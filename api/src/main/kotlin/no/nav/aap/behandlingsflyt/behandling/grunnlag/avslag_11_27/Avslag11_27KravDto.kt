package no.nav.aap.behandlingsflyt.behandling.grunnlag.avslag_11_27

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import java.time.LocalDate

data class Avslag11_27KravDto(
    val referanse: String,
    val søknadsdokument: String,
    val type: String,
    val søknadsdato: LocalDate,
    val muligRettighetFra: LocalDate
) {
    companion object {
        fun avslag11_27TilDto(kravListe: List<RelevantKrav>): List<Avslag11_27KravDto> {
            return kravListe.map { krav ->
                val (referanse, journalpostId, kravType) = Triple(
                    krav.referanse,
                    krav.journalpostId,
                    KravType.RELEVANT_KRAV
                )
                Avslag11_27KravDto(
                    referanse = referanse.verdi.toString(),
                    søknadsdokument = journalpostId.identifikator,
                    type = kravType.name,
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
    val brukersYtelseAlternativer: List<Ytelse> = Ytelse.entries,

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
