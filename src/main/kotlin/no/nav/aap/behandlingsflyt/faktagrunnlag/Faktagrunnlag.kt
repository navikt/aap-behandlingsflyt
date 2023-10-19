package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring.Legeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade

enum class GrunnlagstypeEnumTing {
    YRKESSKADE,
    LEGEERKLÆRING
}

class Faktagrunnlag internal constructor(
    private val grunnlag: List<Grunnlag>
) {
    constructor() : this(listOf(Yrkesskade(), Legeerklæring()))

    fun oppdaterFaktagrunnlagForKravliste(kravliste: List<Grunnlagstype<*>>): List<Grunnlagstype<*>> {
        return kravliste.filterNot { grunnlagstype -> grunnlagstype.oppdater(grunnlag) }
    }

    fun <DATATYPE, GRUNNLAGSTYPE : Grunnlagstype<DATATYPE>> hentGrunnlagFor(grunnlagstype: GRUNNLAGSTYPE): DATATYPE? {
        return grunnlagstype.hentGrunnlag(grunnlag)
    }
}
