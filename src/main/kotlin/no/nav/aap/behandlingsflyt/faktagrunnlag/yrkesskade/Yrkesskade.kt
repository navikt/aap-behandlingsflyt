package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagstype

class Yrkesskade internal constructor(
    private val datalager: Yrkesskadedatalager,
    private val service: YrkesskadeService
) : Grunnlag {
    constructor() : this(
        datalager = Yrkesskadedatalager(),
        service = YrkesskadeService()
    )

    override fun oppdaterYrkesskade(): Boolean {
        if (datalager.erOppdatert()) {
            return true
        }

        val nyeData = service.hentYrkesskade()
        val gamleData = datalager.hentYrkesskade()
        if (nyeData == gamleData) {
            return true
        }

        datalager.lagre(nyeData)
        return false
    }

    override fun hentYrkesskade(): Yrkesskadedata? {
        return datalager.hentYrkesskade()
    }

    companion object : Grunnlagstype<Yrkesskadedata>() {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterYrkesskade)
        }

        override fun hentGrunnlag(grunnlag: List<Grunnlag>): Yrkesskadedata? {
            return grunnlag.firstNotNullOfOrNull(Grunnlag::hentYrkesskade)
        }
    }
}
