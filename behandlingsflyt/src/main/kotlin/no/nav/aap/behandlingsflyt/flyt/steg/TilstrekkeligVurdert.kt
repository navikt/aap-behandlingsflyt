package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

sealed class TilstrekkeligVurdertResultat {
    data object Godkjent : TilstrekkeligVurdertResultat()
    data class IkkeTilstrekkelig(val melding: String) : TilstrekkeligVurdertResultat()

    fun erTilstrekkelig(): Boolean = this is Godkjent

    fun valider() {
        if (this is IkkeTilstrekkelig) throw UgyldigForespørselException(melding)
    }
}

interface TilstrekkeligVurdert<in Input> {
    fun erTilstrekkeligVurdert(input: Input): TilstrekkeligVurdertResultat
}

