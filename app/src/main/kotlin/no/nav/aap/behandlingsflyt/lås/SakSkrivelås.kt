package no.nav.aap.behandlingsflyt.lås

import no.nav.aap.verdityper.sakogbehandling.SakId

data class SakSkrivelås(val id: SakId, val versjon: Long)
