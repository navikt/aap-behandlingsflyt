package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.verdityper.sakogbehandling.Ident

class RettTilBarnetillegg(barn: Set<Ident> = emptySet()) {
    private val barnMedFolkeregisterRelasjonTil = barn.toMutableSet()
    private val uavklarteBarn = mutableSetOf<Ident>()
    private val godkjenteUavklarteBarn = mutableSetOf<Ident>()
    private val underkjenteUavklarteBarn = mutableSetOf<Ident>()

    fun leggTilFolkeregisterBarn(ident: Set<Ident>): RettTilBarnetillegg {
        barnMedFolkeregisterRelasjonTil.addAll(ident)
        return this
    }

    fun leggTilOppgitteBarn(ident: Set<Ident>): RettTilBarnetillegg {
        uavklarteBarn.addAll(ident.filter { ident -> !barnMedFolkeregisterRelasjonTil.any { it.er(ident) } })
        return this
    }

    fun godkjenteBarn(ident: Set<Ident>): RettTilBarnetillegg {
        uavklarteBarn.removeAll(ident)
        godkjenteUavklarteBarn.addAll(ident)
        return this
    }

    fun underkjenteBarn(ident: Set<Ident>): RettTilBarnetillegg {
        uavklarteBarn.removeAll(ident)
        underkjenteUavklarteBarn.addAll(ident)
        return this
    }

    fun barnMedRettTil(): Set<Ident> {
        return barnMedFolkeregisterRelasjonTil.toSet() + godkjenteUavklarteBarn.toSet()
    }

    fun harBarnTilAvklaring(): Boolean {
        return uavklarteBarn.filterNot { ident -> godkjenteUavklarteBarn.any { it.er(ident) } }
            .filterNot { ident -> underkjenteUavklarteBarn.any { it.er(ident) } }.isNotEmpty()
    }

    fun barnTilAvklaring(): Set<Ident> {
        return uavklarteBarn.filterNot { ident -> godkjenteUavklarteBarn.any { it.er(ident) } }
            .filterNot { ident -> underkjenteUavklarteBarn.any { it.er(ident) } }.toSet()
    }

    fun registerBarn(): Set<Ident> {
        return barnMedFolkeregisterRelasjonTil
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RettTilBarnetillegg

        if (barnMedFolkeregisterRelasjonTil != other.barnMedFolkeregisterRelasjonTil) return false
        if (uavklarteBarn != other.uavklarteBarn) return false
        if (godkjenteUavklarteBarn != other.godkjenteUavklarteBarn) return false
        if (underkjenteUavklarteBarn != other.underkjenteUavklarteBarn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = barnMedFolkeregisterRelasjonTil.hashCode()
        result = 31 * result + uavklarteBarn.hashCode()
        result = 31 * result + godkjenteUavklarteBarn.hashCode()
        result = 31 * result + underkjenteUavklarteBarn.hashCode()
        return result
    }

    override fun toString(): String {
        return "RettTilBarnetillegg(antallBarn=${barnMedRettTil()}, harBarnTilAvklaring=${harBarnTilAvklaring()})"
    }

}
