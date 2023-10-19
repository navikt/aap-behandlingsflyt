package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagstype

class Yrkesskade(
    private val datalager: Yrkesskadedatalager = Yrkesskadedatalager(),
    private val service: YrkesskadeService = YrkesskadeService()
) : Grunnlag {

    override fun oppdaterYrkesskade(): Boolean {
        //Hent siste tidspunkt for oppdatering
        //Hvis oppdatert, return
        if (datalager.erOppdatert()) {
            return true
        }
        //Hvis ikke, hent nye data og kontroller endringer
        val nyeData = service.hentYrkesskade()
        val gamleData = datalager.hentYrkesskade()
        if (nyeData == gamleData) {
            return true
        }
        //Ved endringer, lagre og returner hint om at data er oppdatert
        datalager.lagre(nyeData)
        return false
    }

    companion object : Grunnlagstype {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterYrkesskade)
        }
    }
}
