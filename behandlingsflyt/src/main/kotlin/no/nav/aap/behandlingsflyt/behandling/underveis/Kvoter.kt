package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager

class Kvoter(
    val ordinærkvote: Hverdager,
    val studentkvote: Hverdager,
    val sykepengeerstatningkvote: Hverdager
) {
    companion object {
        fun create(ordinærkvote: Int, studentkvote: Int, sykepengeerstatningkvote: Int) = Kvoter(
            ordinærkvote = Hverdager(ordinærkvote),
            studentkvote = Hverdager(studentkvote),
            sykepengeerstatningkvote = Hverdager(sykepengeerstatningkvote)
        )
    }
}


