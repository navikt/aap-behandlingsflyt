package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.orEmpty

private const val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

class KvoteService(private val underveisService: UnderveisRepository) {

    fun beregn(behandlingId: BehandlingId): Kvoter {

        val tidslinje = underveisService.hentHvisEksisterer(behandlingId)?.somTidslinje().orEmpty()

        val x = tidslinje.map { it.brukerAvKvoter }
            .filter { it.verdi.contains(Kvote.SYKEPENGEERSTATNING) }
            .segmenter()
            .map { it.periode.antallHverdager() }
            .sumOf { it.asInt }

        println("XXXX")
        println(x)
        println("XXXX")

        // TODO ta hensyn til når du har rett på hvilken kvote (Kvoter-objektet burde ha en tidslinje et sted)
        // Dette burde skje ved å hente en tidslinje av rettighetstyper
        return Kvoter.create(
            /* Så lenge Arena har 784 må vi ha samme som dem, i stede for ANTALL_ARBEIDSDAGER_I_ÅRET * 3. */
            ordinærkvote = 784,
            studentkvote = ANTALL_ARBEIDSDAGER_I_ÅRET / 2,
            sykepengeerstatningkvote = ANTALL_ARBEIDSDAGER_I_ÅRET / 2
        )
    }
}