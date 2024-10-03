package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetsplikt
import no.nav.aap.komponenter.type.Periode

data class AktivitetspliktVurdering(
    val periode: Periode,
    val muligeSanksjoner: List<BruddAktivitetsplikt.Paragraf>,
    val saksbehandlersØnsketSanksjon: BruddAktivitetsplikt.Paragraf,
)