package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
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

    // Det finnes en fremtidig periode med ordinær rett og gjenværende kvote
    private fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        behandlingId: BehandlingId,
    ): Boolean {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.ForenkletKvote)) {
            return vurderRettighetstypeOgKvoter(vilkårsresultatRepository.hent(behandlingId), KvoteService().beregn())
                .begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
                .segmenter()
                .any { Kvote.ORDINÆR in it.verdi.brukerAvKvoter() }
        } else {
            val rettighetstypeTidslinjeForInneværendeBehandling = vilkårsresultatRepository.hent(behandlingId).rettighetstypeTidslinje()
            val varighetstidslinje = VarighetRegel().simuler(rettighetstypeTidslinjeForInneværendeBehandling)
            return varighetstidslinje.begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
                .segmenter()
                .any { varighetSegment ->
                    varighetSegment.verdi.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR }
                            && varighetSegment.verdi !is Avslag
                }
        }
    }
}