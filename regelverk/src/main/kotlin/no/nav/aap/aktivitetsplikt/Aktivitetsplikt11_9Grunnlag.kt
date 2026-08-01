package no.nav.aap.aktivitetsplikt

data class Aktivitetsplikt11_9Grunnlag(
    val vurderinger: Set<Aktivitetsplikt11_9Vurdering>) {

    fun gjeldendeVurderinger(): Set<Aktivitetsplikt11_9Vurdering> =
         vurderinger.groupBy { it.dato }
            .mapValues { it.value.maxBy { v -> v.opprettet } }
            .values
            .toSet()

}