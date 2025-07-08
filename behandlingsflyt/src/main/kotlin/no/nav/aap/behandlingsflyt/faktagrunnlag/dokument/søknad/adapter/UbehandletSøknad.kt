package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.søknad.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.OppgitteBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.ErStudentStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.SkalGjenopptaStudieStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsOppholdData
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold.UtenlandsPeriode
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Søknad
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate
import kotlin.text.uppercase

data class UbehandletSøknad(
    val journalpostId: JournalpostId,
    val periode: Periode,
    val studentData: StudentData?,
    val harYrkesskade: Boolean,
    val oppgitteBarn: OppgitteBarn?,
    val utenlandsOppholdData: UtenlandsOppholdData?
) {
    companion object {
        fun fraKontrakt(søknad: Søknad, mottattDato: LocalDate, journalPostId: JournalpostId): UbehandletSøknad {
            return when (søknad) {
                is SøknadV0 -> UbehandletSøknad(
                    journalpostId = journalPostId,
                    periode = Periode(mottattDato, mottattDato),
                    studentData = if (søknad.student == null) null else søknad.student?.let {
                        val erStudent = erStudent(it.erStudent) ?: return@let null
                        StudentData(
                            erStudent = erStudent,
                            skalGjenopptaStudie = skalGjennopptaStudie(it.kommeTilbake)
                        )
                    },
                    harYrkesskade = søknad.yrkesskade.uppercase() == "JA",
                    oppgitteBarn = søknad.oppgitteBarn.let {
                        it?.identer?.map {
                            Ident(
                                identifikator = it.identifikator,
                                aktivIdent = true
                            )
                        }?.let { id ->
                            OppgitteBarn(
                                identer = id
                            )
                        }
                    },
                    utenlandsOppholdData = if (søknad.medlemskap == null) null else søknad.medlemskap?.let {
                        val utenlandsOpphold = it.utenlandsOpphold?.map { opphold ->
                            UtenlandsPeriode(
                                land = opphold.land,
                                tilDato = opphold.tilDatoLocalDate,
                                fraDato = opphold.fraDatoLocalDate,
                                iArbeid = opphold.iArbeid?.uppercase() == "JA",
                                utenlandsId = opphold.utenlandsId,
                            )
                        }

                        UtenlandsOppholdData(
                            harBoddINorgeSiste5År = it.harBoddINorgeSiste5År?.uppercase() == "JA",
                            harArbeidetINorgeSiste5År = it.harArbeidetINorgeSiste5År?.uppercase() == "JA",
                            arbeidetUtenforNorgeFørSykdom = it.arbeidetUtenforNorgeFørSykdom?.uppercase() == "JA",
                            iTilleggArbeidUtenforNorge = it.iTilleggArbeidUtenforNorge?.uppercase() == "JA",
                            utenlandsOpphold = utenlandsOpphold
                        )
                    }
                )
            }
        }

        private fun erStudent(stringVerdi: String): ErStudentStatus? {
            return if (stringVerdi.uppercase() == "JA") {
                ErStudentStatus.JA
            } else if (stringVerdi.uppercase() == "AVBRUTT") {
                ErStudentStatus.AVBRUTT
            } else if (stringVerdi.uppercase() == "NEI") {
                ErStudentStatus.NEI
            } else {
                null
            }
        }


        private fun skalGjennopptaStudie(stringVerdi: String?): SkalGjenopptaStudieStatus {
            return if (stringVerdi?.uppercase() == "JA") {
                SkalGjenopptaStudieStatus.JA
            } else if (stringVerdi?.uppercase() == "NEI") {
                SkalGjenopptaStudieStatus.NEI
            } else if (stringVerdi?.uppercase() == "VET IKKE") {
                SkalGjenopptaStudieStatus.VET_IKKE
            } else {
                SkalGjenopptaStudieStatus.IKKE_OPPGITT
            }
        }
    }
}

data class StudentData(val erStudent: ErStudentStatus, val skalGjenopptaStudie: SkalGjenopptaStudieStatus)