package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident

fun ident(): Ident {
    val ident = Ident(FødselsnummerGenerator.Builder().buildAndGenerate())
    return ident
}