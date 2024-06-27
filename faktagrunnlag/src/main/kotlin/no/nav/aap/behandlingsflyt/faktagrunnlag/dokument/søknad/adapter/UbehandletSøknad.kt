package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

class UbehandletSøknad(
    val journalpostId: JournalpostId,
    val periode: Periode,
    val erStudent: ErStudentStatus,
    val skalGjenopptaStudie: SkalGjenopptaStudieStatus?,
    val harYrkesskade: Boolean
) {
}