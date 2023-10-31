package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.flyt.steg.StegGruppe

data class FlytGruppe(val stegGruppe: StegGruppe, val erFullført: Boolean, val steg: List<FlytSteg>)
