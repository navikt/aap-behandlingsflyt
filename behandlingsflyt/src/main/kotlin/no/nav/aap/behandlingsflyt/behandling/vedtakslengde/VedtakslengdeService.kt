package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate

class VedtakslengdeService(
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(datoForUtvidelse)
    }

    fun skalUtvideVedtakslengde(
        behandlingId: BehandlingId,
        datoForUtvidelse: LocalDate = LocalDate.now(clock).plusDays(28)
    ): Boolean {
        val underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandlingId)
        if (underveisGrunnlag != null) {
            val sisteVedtatteUnderveisperiode = underveisGrunnlag.perioder.maxByOrNull { it.periode.tom }

            if (sisteVedtatteUnderveisperiode != null) {
                val forrigeSluttdato = sisteVedtatteUnderveisperiode.periode.tom
                val harFremtidigRettOrdinær = harFremtidigRettOrdinær(forrigeSluttdato, behandlingId)

                log.info("Behandling $behandlingId har harFremtidigRettOrdinær=$harFremtidigRettOrdinær og forrigeSluttdato=$forrigeSluttdato")
                return datoForUtvidelse >= forrigeSluttdato && harFremtidigRettOrdinær
            } else {
                log.info("Behandling $behandlingId har ingen vedtatte underveisperioder")
            }
        } else {
            log.info("Behandling $behandlingId har ikke underveisgrunnlag")
        }
        return false
    }

    /**
     * Neste periode (hele året) er av type ordinær med gjenværende kvote
     */
    private fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        behandlingId: BehandlingId,
    ): Boolean {
        // TODO her må vi vel ta høyde for eventuelle tidligere utvidelser?
        val nySluttdato = vedtattSluttdato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
        val nyUtvidetVedtaksperiode = Periode(vedtattSluttdato.plusDays(1), nySluttdato)
        val nyUtvidetVedtaksperiodeTidslinje = Tidslinje(Periode(vedtattSluttdato.plusDays(1), nySluttdato), true)

        if (unleashGateway.isEnabled(BehandlingsflytFeature.ForenkletKvote)) {
            return vurderRettighetstypeOgKvoter(vilkårsresultatRepository.hent(behandlingId), KvoteService().beregn())
                .begrensetTil(nyUtvidetVedtaksperiode)
                .rightJoin(nyUtvidetVedtaksperiodeTidslinje) { vurdering, _ ->
                    vurdering != null && vurdering is KvoteOk && Kvote.ORDINÆR in vurdering.brukerAvKvoter()
                }
                .segmenter()
                .all { it.verdi }

        } else {
            val rettighetstypeTidslinjeForInneværendeBehandling = vilkårsresultatRepository.hent(behandlingId).rettighetstypeTidslinje()
            return VarighetRegel().simuler(rettighetstypeTidslinjeForInneværendeBehandling)
                .begrensetTil(nyUtvidetVedtaksperiode)
                .rightJoin(nyUtvidetVedtaksperiodeTidslinje) { vurdering, _ ->
                    vurdering != null && vurdering !is Avslag && vurdering.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR }
                }
                .segmenter()
                .all { it.verdi }
        }
    }
}