package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakOppsummering
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakerResponse
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaStatusResponse
import no.nav.aap.behandlingsflyt.hendelse.datadeling.UnderveisperiodeDatadeling
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.datadeling.UtledArenaVedtakstype
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.time.LocalDate

class FakeApiInternGateway : ApiInternGateway {
    companion object : Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
            return FakeApiInternGateway()
        }
        // No-op
    }

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        // No-op
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        // No-op
    }

    override fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: List<SamIdOgTpNr>,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal?,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>,
        stansOpphørGrunnlag: Set<GjeldendeStansEllerOpphør>?,
        perioderMedFritakMeldeplikt: List<Periode>,
        underveisperioder: List<UnderveisperiodeDatadeling>,
        arenavedtak: Tidslinje<UtledArenaVedtakstype.ArenaVedtak>,
        muligMaksdato: LocalDate?
    ) {
        // No-op
    }

    override fun sendDetaljertMeldekortListe(
        detaljertMeldekortListe: List<DetaljertMeldekortDTO>,
        sakId: SakId,
        behandlingId: BehandlingId
    ) {
        // No-op
    }

    override fun hentArenaStatus(personidentifikatorer: Set<String>): Result<ArenaStatusResponse> {
        return Result.success(ArenaStatusResponse(false))
    }

    override fun oppdaterIdenter(
        saksnummer: Saksnummer,
        identer: List<Ident>
    ) {
        // No-op
    }

    override fun hentSakerForPerson(personidentifikator: String): ArenaSakerResponse {
        return ArenaSakerResponse(
            saker = listOf(
                ArenaSakOppsummering(
                    sakId = "2016-123456",
                    lopenummer = 123456,
                    aar = 2016,
                    antallVedtak = 1,
                    statuskode = "AKTIV",
                    statusnavn = "Aktiv",
                    sakstype = null,
                    regDato = LocalDate.of(2016, 1, 1),
                    avsluttetDato = null,
                )
            )
        )
    }

}