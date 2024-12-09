package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

// mål få denne
data class UbehandletSøknad(
    val journalpostId: JournalpostId,
    val periode: Periode,
    val studentData: StudentData,
    val harYrkesskade: Boolean,
    val oppgitteBarn: OppgitteBarn?
)

data class StudentData(val erStudent: ErStudentStatus, val skalGjenopptaStudie: SkalGjenopptaStudieStatus)