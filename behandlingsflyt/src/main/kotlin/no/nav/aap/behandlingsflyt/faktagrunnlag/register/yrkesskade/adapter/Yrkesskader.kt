package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter

class Yrkesskader(
    //FIXME: Kan denne være null?? Når da? Ser ut som at yrkesskade-saker alltid returnerer en liste med mindre det er en feil i responsen
    val skader: List<YrkesskadeModell>?
)