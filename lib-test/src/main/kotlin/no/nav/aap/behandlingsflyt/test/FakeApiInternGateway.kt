package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdOgTpNr
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.BarnMedBarnetillegg
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
import java.util.*

class FakeApiInternGateway : ApiInternGateway {
    companion object : Factory<ApiInternGateway> {
        // Delt statisk liste - kan aksesseres fra flere motor-tråder samtidig, må derfor være trådsikker
        val sendteSakStatuser: MutableList<Pair<String, SakStatus>> = Collections.synchronizedList(mutableListOf())

        override fun konstruer(): ApiInternGateway {
            return FakeApiInternGateway()
        }
        // No-op
    }

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        // No-op
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        sendteSakStatuser.add(ident to sakStatus)
    }

    override fun varsleNySøknadForPerson(ident: String) {
        // no-op
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
        muligMaksdato: LocalDate?,
        barnMedBarnetillegg: List<BarnMedBarnetillegg>
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

    override fun oppdaterIdenter(
        saksnummer: Saksnummer,
        identer: List<Ident>
    ) {
        // No-op
    }

}