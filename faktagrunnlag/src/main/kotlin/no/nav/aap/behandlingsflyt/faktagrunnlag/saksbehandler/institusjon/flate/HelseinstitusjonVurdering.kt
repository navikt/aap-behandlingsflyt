package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

data class HelseinstitusjonVurdering(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val begrunnelse: String,
    val faarFriKostOgLosji: Boolean,
    val forsoergerEktefelle: Boolean? = null,
    val harFasteUtgifter: Boolean? = null,
    val periode: Periode
)