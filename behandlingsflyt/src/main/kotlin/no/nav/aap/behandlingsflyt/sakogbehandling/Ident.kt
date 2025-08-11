package no.nav.aap.behandlingsflyt.sakogbehandling

import kotlin.math.min

data class Ident(val identifikator: String, val aktivIdent: Boolean = true) {

    fun er(ident: Ident): Boolean {
        return identifikator == ident.identifikator
    }

    override fun toString(): String {
        return "Ident(identifikator='${identifikator.substring(0, min(identifikator.length, 6))}*****')"
    }

    fun getMasked(): String =
        "${identifikator.substring(0, min(identifikator.length, 6))}*****"

}
