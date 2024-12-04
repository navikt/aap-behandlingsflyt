package no.nav.aap.behandlingsflyt.sakogbehandling.lås

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId


data class SakSkrivelås(val id: SakId, val versjon: Long)
