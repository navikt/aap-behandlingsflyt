package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

interface ApiInternGateway : Gateway {
    fun sendPerioder(ident: String, perioder: List<Periode>)
    fun sendSakStatus(ident: String, sakStatus: SakStatus)
    fun sendBehandling(
        sak: Sak,
        behandling: Behandling,
        vedtakId: Long?,
        samId: String?,
        tilkjent: List<TilkjentYtelsePeriode>?,
        underveis: List<Underveisperiode>,
        vedtaksDato: LocalDate,
        rettighetsTypeTidslinje: Tidslinje<RettighetsType>
    )
}

