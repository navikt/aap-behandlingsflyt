package no.nav.aap.behandlingsflyt.behandling.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator

class RettTilBarnetillegg(barn: Set<BarnIdentifikator> = emptySet()) {
    private val barnMedFolkeregisterRelasjonTil = barn.toMutableSet()
    private val uavklarteBarn = mutableSetOf<BarnIdentifikator>()
    private val godkjenteUavklarteBarn = mutableSetOf<BarnIdentifikator>()
    private val underkjenteUavklarteBarn = mutableSetOf<BarnIdentifikator>()

    fun leggTilFolkeregisterBarn(barn: Set<BarnIdentifikator>): RettTilBarnetillegg {
        barnMedFolkeregisterRelasjonTil.addAll(barn)
        return this
    }

    fun leggTilUavklartBarn(barn: Set<BarnIdentifikator>): RettTilBarnetillegg {
        val identer = barn.toSet()
        uavklarteBarn.addAll(identer.filter { ident -> !barnMedFolkeregisterRelasjonTil.any { it.er(ident) } })
        return this
    }

    fun godkjenteBarn(ident: Set<BarnIdentifikator>): RettTilBarnetillegg {
        uavklarteBarn.removeAll(ident)
        godkjenteUavklarteBarn.addAll(ident)
        return this
    }

    fun underkjenteBarn(ident: Set<BarnIdentifikator>): RettTilBarnetillegg {
        uavklarteBarn.removeAll(ident)
        underkjenteUavklarteBarn.addAll(ident)
        return this
    }

    fun barnMedRettTil(): Set<BarnIdentifikator> {
        return barnMedFolkeregisterRelasjonTil.toSet() + godkjenteUavklarteBarn.toSet()
    }

    fun harBarnTilAvklaring(): Boolean {
        return uavklarteBarnIdenter().isNotEmpty()
    }

    fun barnTilAvklaring(): Set<BarnIdentifikator> {
        return uavklarteBarnIdenter().toSet()
    }

    private fun uavklarteBarnIdenter(): List<BarnIdentifikator> {
        return uavklarteBarn
            .filterNot { ident -> godkjenteUavklarteBarn.any { it.er(ident) } }
            .filterNot { ident -> underkjenteUavklarteBarn.any { it.er(ident) } }
    }

    fun registerBarn(): Set<BarnIdentifikator> {
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
        return "RettTilBarnetillegg(barnMedFolkeregisterRelasjonTil=$barnMedFolkeregisterRelasjonTil, uavklarteBarn=$uavklarteBarn, godkjenteUavklarteBarn=$godkjenteUavklarteBarn, underkjenteUavklarteBarn=$underkjenteUavklarteBarn)"
    }

    fun copy(): RettTilBarnetillegg {
        val kopi = RettTilBarnetillegg()
        kopi.barnMedFolkeregisterRelasjonTil.addAll(this.barnMedFolkeregisterRelasjonTil)
        kopi.uavklarteBarn.addAll(uavklarteBarn)
        kopi.godkjenteUavklarteBarn.addAll(godkjenteUavklarteBarn)
        kopi.underkjenteUavklarteBarn.addAll(underkjenteUavklarteBarn)
        return kopi
    }
}
