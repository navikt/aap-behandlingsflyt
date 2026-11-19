package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

object SamordningUføreVurderingValidator {
    /**
     * Virkningstidspunkt for uførevedtak som mangler vurdering, og som er gyldig i en periode
     * der bruker potensielt har rett på AAP. [behandlingsutfall] er allerede avgrenset til rettighetsperioden.
     */
    fun manglendeVurderinger(
        behandlingsutfall: Tidslinje<TidligereVurderinger.Behandlingsutfall>,
        uførevedtak: Collection<Uføre>,
        vurderinger: Collection<SamordningUføreVurderingPeriode>,
    ): Set<LocalDate> {
        val uføretidslinje = uførevedtak.tilTidslinje()
        val vurderteVirkningstidspunkt = vurderinger.map { it.virkningstidspunkt }.toSet()

        val rettOgUføre = Tidslinje.map2(behandlingsutfall, uføretidslinje) { utfall, uføregrad ->
            utfall is TidligereVurderinger.PotensieltOppfylt && uføregrad != null
        }

        val sorterteVirkningstidspunkt = uførevedtak.map { it.virkningstidspunkt }.distinct().sorted()

        return sorterteVirkningstidspunkt
            .filterNot { it in vurderteVirkningstidspunkt }
            .filter { virkningstidspunkt ->
                val segment = uføretidslinje.segmenter().firstOrNull { it.periode.inneholder(virkningstidspunkt) }
                    ?: return@filter false
                val nesteVirkningstidspunkt = sorterteVirkningstidspunkt.firstOrNull { it > virkningstidspunkt }
                val tom = minOf(segment.periode.tom, nesteVirkningstidspunkt?.minusDays(1) ?: Tid.MAKS)

                rettOgUføre.begrensetTil(Periode(virkningstidspunkt, tom)).segmenter().any { it.verdi }
            }
            .toSet()
    }
}
