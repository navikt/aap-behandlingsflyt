package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.legeerklæring.Legeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade.Yrkesskade

enum class GrunnlagstypeEnumTing {
    YRKESSKADE,
    LEGEERKLÆRING
}

class Faktagrunnlag(
    private val grunnlag: List<Grunnlag> = listOf(Yrkesskade(), Legeerklæring())
) {
    fun oppdaterFaktagrunnlagForKravliste(kravliste: List<Grunnlagstype>): List<Grunnlagstype> {
        return kravliste.filterNot { grunnlagstype -> grunnlagstype.oppdater(grunnlag) }
    }
}
