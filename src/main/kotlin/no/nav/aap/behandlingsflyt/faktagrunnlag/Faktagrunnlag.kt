package no.nav.aap.behandlingsflyt.faktagrunnlag

import java.time.LocalDate
import java.time.LocalDateTime

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

abstract class Grunnlag {
    open fun oppdaterYrkesskade(): Boolean {
        return true
    }

    open fun oppdaterLegeerklæring(): Boolean {
        return true
    }
}

interface Grunnlagstype {
    fun oppdater(grunnlag: List<Grunnlag>): Boolean
}

class Yrkesskade(
    private val datalager: Yrkesskadedatalager = Yrkesskadedatalager(),
    private val service: YrkesskadeService = YrkesskadeService()
) : Grunnlag() {
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

    class Yrkesskadedatalager {
        private var sisteLagretTidspunkt: LocalDateTime? = null //TODO: Må hentes fra DB
        private lateinit var data: Yrkesskadedata               //TODO: Må hentes fra DB

        fun erOppdatert(): Boolean {
            val sisteLagretTidspunkt = sisteLagretTidspunkt
            if (sisteLagretTidspunkt == null) {
                return false
            }
            return sisteLagretTidspunkt.toLocalDate() >= LocalDate.now()
        }

        fun hentYrkesskade(): Yrkesskadedata? {
            if (sisteLagretTidspunkt == null) {
                return null
            }
            return data
        }

        fun lagre(data: Yrkesskadedata, sisteLagretTidspunkt: LocalDateTime = LocalDateTime.now()) {
            this.sisteLagretTidspunkt = sisteLagretTidspunkt
            this.data = data
        }
    }

    class YrkesskadeService {
        fun hentYrkesskade(): Yrkesskadedata {
            return Yrkesskadedata()
        }
    }

    class Yrkesskadedata {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            return true
        }

        override fun hashCode(): Int {
            return javaClass.hashCode()
        }
    }

    companion object : Grunnlagstype {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterYrkesskade)
        }
    }
}

class Legeerklæring : Grunnlag() {
    override fun oppdaterLegeerklæring(): Boolean {
        //TODO("Not yet implemented")
        return true
    }

    companion object : Grunnlagstype {
        override fun oppdater(grunnlag: List<Grunnlag>): Boolean {
            return grunnlag.all(Grunnlag::oppdaterLegeerklæring)
        }
    }
}
