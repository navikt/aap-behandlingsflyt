package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType

class Kvoter(
    val ordinærkvote: Hverdager,
    val sykepengeerstatningkvote: Hverdager
) {
    companion object {
        fun create(ordinærkvote: Int, sykepengeerstatningkvote: Int) = Kvoter(
            ordinærkvote = Hverdager(ordinærkvote),
            sykepengeerstatningkvote = Hverdager(sykepengeerstatningkvote)
        )
    }

    fun hentKvoteForRettighetstype(type: RettighetsType): Hverdager? {
        return when (type) {
            RettighetsType.BISTANDSBEHOV -> this.ordinærkvote
            RettighetsType.SYKEPENGEERSTATNING -> this.sykepengeerstatningkvote
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Kvoter

        if (ordinærkvote != other.ordinærkvote) return false
        if (sykepengeerstatningkvote != other.sykepengeerstatningkvote) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ordinærkvote.hashCode()
        result = 31 * result + sykepengeerstatningkvote.hashCode()
        return result
    }

    override fun toString(): String {
        return "Kvoter(ordinærkvote=$ordinærkvote, sykepengeerstatningkvote=$sykepengeerstatningkvote)"
    }
}
