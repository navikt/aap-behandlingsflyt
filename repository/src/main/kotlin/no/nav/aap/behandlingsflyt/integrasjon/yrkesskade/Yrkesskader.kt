package no.nav.aap.behandlingsflyt.integrasjon.yrkesskade

import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeModell

class Yrkesskader(
    //FIXME: Kan denne være null?? Når da? Ser ut som at yrkesskade-saker alltid returnerer en liste med mindre det er en feil i responsen
    val saker: List<YrkesskadeModell>?
)