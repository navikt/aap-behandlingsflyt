package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.sak.SakId

data class FlytKontekst(val sakId: SakId,
                        val behandlingId: Long)
