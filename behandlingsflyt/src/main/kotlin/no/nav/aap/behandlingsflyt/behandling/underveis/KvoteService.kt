package no.nav.aap.behandlingsflyt.behandling.underveis

class KvoteService {
    fun beregn(): Kvoter {
        return Kvoter.create(
            /* Så lenge Arena har 784 må vi ha samme som dem, i stede for ANTALL_ARBEIDSDAGER_I_ÅRET * 3. */
            ordinærkvote = 784,

            /* Fra regelspesifiseringen:
             *
             *  Perioden på inntil 6 måneder er en kvote som består av 131 dager.
             *  Begrunnelsen for at 6 mnd = 131 dager er at tre år etter § 11-12
             *  er (261 dager ganger 3) + 1 = 784 dager. Dersom man deler 784 på 6
             *  for å få 6 måneder tilsvarer dette 130,67 dager, som rundes opp til
             *  131.
             */
            sykepengeerstatningkvote = 131,
        )
    }
}
