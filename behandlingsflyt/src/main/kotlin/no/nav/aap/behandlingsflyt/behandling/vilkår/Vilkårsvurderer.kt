package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag

@Deprecated(
    """Dette interfacet er basert på mutering og er nesten bare et marker-interface – det
        brukes ikke til noe egentlig.
        Det nye interfacet legger opp til en funksjonell approach, og eksponerer også vilkårstype,
        slik at VilkårService kan håndtere skriving av vilkåret automatisk.""",
    replaceWith = ReplaceWith("no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer")
)
interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T)
}
