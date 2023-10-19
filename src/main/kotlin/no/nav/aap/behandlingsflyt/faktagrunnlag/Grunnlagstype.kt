package no.nav.aap.behandlingsflyt.faktagrunnlag

interface Grunnlagstype {
    fun oppdater(grunnlag: List<Grunnlag>): Boolean
}
