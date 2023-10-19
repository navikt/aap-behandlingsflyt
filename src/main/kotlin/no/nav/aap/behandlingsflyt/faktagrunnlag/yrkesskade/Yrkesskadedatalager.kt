package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import java.time.LocalDate
import java.time.LocalDateTime

internal class Yrkesskadedatalager {
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
