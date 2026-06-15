package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.verdityper.dokument.JournalpostId

data class Avslag11_27VurderingDto(
    val journalpostId: String,
    val begrunnelse: String,
    val harAnnenFullYtelse: Boolean,
    val brukersYtelse: Ytelse? = null,
    val harSykepengegrunnlagOver2G: Boolean? = null, // Kun for sykepenger
    val skalAvslås1127: Boolean,
)
