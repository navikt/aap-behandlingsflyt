package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sak.SakId

data class SakSkrivelås(val id: SakId, val versjon: Long)
