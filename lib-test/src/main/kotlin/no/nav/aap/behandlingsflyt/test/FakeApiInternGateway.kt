package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.datadeling.DetaljertMeldekortDTO
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

    }

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
    }

    override fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>,
        beregningsgrunnlag: BigDecimal,
        underveis: List<Underveisperiode>,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>
    ) {
    }

    override fun sendDetaljertMeldekortListe(
        detaljertMeldekortListe: List<DetaljertMeldekortDTO>,
        sakId: SakId,
        behandlingId: BehandlingId
    ){
    }
}