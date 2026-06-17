package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.datadeling.UtledArenaVedtakstype
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface ApiInternGateway : Gateway {
    fun sendPerioder(ident: String, perioder: List<Periode>)
    fun sendSakStatus(ident: String, sakStatus: SakStatus)

    /**
     * @param vedtakId ID til raden i vedtak-tabellen som referer til behandlingen.
     * @param beregningsgrunnlag Beregningsgrunnlag i kroner.
     */
    fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal?,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>,
        stansOpphørGrunnlag: Set<GjeldendeStansEllerOpphør>?,
        arenavedtak: Tidslinje<UtledArenaVedtakstype.ArenaVedtak>,
        muligMaksdato: LocalDate?,
    )

    fun sendDetaljertMeldekortListe(
        detaljertMeldekortListe: List<DetaljertMeldekortDTO>,
        sakId: SakId,
        behandlingId: BehandlingId
    )

    /**
    Returnerer med overlegg `Result` slik at kalleren av metoden er nødt til å ta hensyn til at kallet kan feile.
    Grunnen er at vi må passe på at Behandlingsflyt ikke feiler når Arena er utilgjengelig.
     */
    fun hentArenaStatus(personidentifikatorer: Set<String>): Result<ArenaStatusResponse>

    fun oppdaterIdenter(saksnummer: Saksnummer, identer: List<Ident>)
}

