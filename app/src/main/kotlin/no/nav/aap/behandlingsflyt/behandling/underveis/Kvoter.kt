package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager

class Kvoter(
    val standardkvote: Hverdager,
    val studentkvote: Hverdager
) {
    companion object {
        fun create(standardkvote: Int, studentkvote: Int) = Kvoter(
            standardkvote = Hverdager(standardkvote),
            studentkvote = Hverdager(studentkvote),
        )
    }
}


