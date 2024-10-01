package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Paragraf
import no.nav.aap.komponenter.type.Periode

data class AktivitetspliktVurdering(
    val periode: Periode,
    val muligeSanksjoner: List<Paragraf>,
    val saksbehandlersØnsketSanksjon: Paragraf,
)