package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArenaMeldeperiodesyklusInformasjonskrav
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Gateway

interface ArenaOppslagGateway : Gateway {
    fun hentHarHistorikk(ident: Ident): HarHistorikkResponse
    fun hentSakerForPerson(ident: Ident): SakerResponse
    fun hentKravDataForSak(arenasaksnummer: String): KravResponse
    fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse
    fun hentArenaMeldekortsyklus(ident: Ident): ArenaMeldeperiodesyklusInformasjonskrav.Registerdata
}
