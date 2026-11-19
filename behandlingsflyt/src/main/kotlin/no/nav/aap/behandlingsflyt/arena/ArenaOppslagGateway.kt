package no.nav.aap.behandlingsflyt.arena

import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.MaksdatoMedVedtakResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaBeregningstidspunktResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaForutgaaendeMedlemskapResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaOppholdskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVedtaksperiodeResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArenaMeldeperiodesyklusInformasjonskrav
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Gateway

interface ArenaOppslagGateway : Gateway {
    fun hentHarHistorikk(ident: Ident): HarHistorikkResponse
    fun hentSakerForPerson(ident: Ident): SakerResponse
    fun hentMaksdato(ident: Ident): MaksdatoMedVedtakResponse
    fun hentKravDataForSak(arenasaksnummer: String): KravResponse
    fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse
    fun hentOppholdskrav(saksnummerArena: String): ArenaOppholdskravResponse
    fun hentForutgaaendeMedlemskap(saksnummerArena: String): ArenaForutgaaendeMedlemskapResponse
    fun hentRefusjonskrav(saksnummerArena: String): ArenaRefusjonskravResponse
    fun hentVedtaksperiode(saksnummerArena: String): ArenaVedtaksperiodeResponse
    fun hentBeregningstidspunkt(saksnummerArena: String): ArenaBeregningstidspunktResponse
    fun hentArenaMeldekortsyklus(ident: Ident): ArenaMeldeperiodesyklusInformasjonskrav.Registerdata
}
