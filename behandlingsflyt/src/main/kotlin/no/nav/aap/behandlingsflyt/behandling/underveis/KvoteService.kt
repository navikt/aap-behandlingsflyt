package no.nav.aap.behandlingsflyt.behandling.underveis

class KvoteService {

    private val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

    fun beregn(): Kvoter {
        // TODO ta hensyn til når du har rett på hvilken kvote (Kvoter-objektet burde ha en tidslinje et sted)
        // Dette burde skje ved å hente en tidslinje av rettighetstyper
        return Kvoter.create(
            /* Så lenge Arena har 784 må vi ha samme som dem, i stede for ANTALL_ARBEIDSDAGER_I_ÅRET * 3. */
            ordinærkvote = 784,
            sykepengeerstatningkvote = ANTALL_ARBEIDSDAGER_I_ÅRET / 2
        )
    }
}