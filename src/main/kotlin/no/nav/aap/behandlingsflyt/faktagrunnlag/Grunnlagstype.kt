package no.nav.aap.behandlingsflyt.faktagrunnlag

abstract class Grunnlagstype<T> {
    internal abstract fun oppdater(grunnlag: List<Grunnlag>): Boolean
    internal abstract fun hentGrunnlag(grunnlag: List<Grunnlag>): T?
}
