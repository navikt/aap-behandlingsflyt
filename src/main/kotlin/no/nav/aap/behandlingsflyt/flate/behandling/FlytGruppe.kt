package no.nav.aap.behandlingsflyt.flate.behandling

import no.nav.aap.flyt.StegGruppe

data class FlytGruppe(val stegGruppe: StegGruppe, val steg: List<FlytSteg>) {
}