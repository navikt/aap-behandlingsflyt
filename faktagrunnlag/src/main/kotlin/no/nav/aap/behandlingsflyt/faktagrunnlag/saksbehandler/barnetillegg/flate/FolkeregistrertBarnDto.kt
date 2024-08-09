package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

data class FolkeregistrertBarnDto(val navn: String, val ident: Ident, val forsorgerPeriode: Periode)
data class ManueltBarnDto(val navn: String, val ident: Ident)