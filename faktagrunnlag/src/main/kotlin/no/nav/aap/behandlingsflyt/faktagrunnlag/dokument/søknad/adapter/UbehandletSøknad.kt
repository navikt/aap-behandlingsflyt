package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgittBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

class UbehandletSøknad(
    val journalpostId: JournalpostId,
    val periode: Periode,
    val erStudent: ErStudentStatus, // TODO: Trekke student ut i eget objekt
    val skalGjenopptaStudie: SkalGjenopptaStudieStatus?,
    val harYrkesskade: Boolean,
    val oppgittBarn: List<OppgittBarn>,
)