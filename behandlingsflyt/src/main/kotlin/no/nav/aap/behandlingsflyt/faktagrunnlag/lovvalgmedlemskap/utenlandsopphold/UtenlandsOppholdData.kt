package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.utenlandsopphold

import java.time.LocalDate


data class UtenlandsOppholdData(
    val harBoddINorgeSiste5År: Boolean,
    val harArbeidetINorgeSiste5År: Boolean,
    val arbeidetUtenforNorgeFørSykdom: Boolean,
    val iTilleggArbeidUtenforNorge: Boolean,
    val utenlandsOpphold: List<UtenlandsPeriode>?
)

data class UtenlandsPeriode(
    val land: String?,
    val tilDato: LocalDate?,
    val fraDato: LocalDate?,
    val iArbeid: Boolean,
    val utenlandsId: String?
)