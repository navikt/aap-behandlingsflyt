package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Gateway

interface ArenaOppslagGateway : Gateway {
    fun hentHarHistorikk(ident: Ident): HarArenaHistorikkResponse
    fun hentSakerForPerson(ident: Ident): ArenaSakerResponse
    fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse
}
