package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Avslag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetRegel
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDate.now

class OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
    private val prosesserBehandlingService: ProsesserBehandlingService,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val datoHvorSakerSjekkesForUtvidelse = now(clock).plusDays(28)

        val sakId = SakId(input.sakId())
        val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
        if (sisteGjeldendeBehandling != null) {
            log.info("Gjeldende behandling for sak $sakId er ${sisteGjeldendeBehandling.id}")
            val underveisGrunnlag = underveisRepository.hentHvisEksisterer(sisteGjeldendeBehandling.id)
            if (underveisGrunnlag != null && harBehovForUtvidetVedtakslengde(sisteGjeldendeBehandling.id, sakId, underveisGrunnlag, datoHvorSakerSjekkesForUtvidelse)) {
                log.info("Oppretter behandling for utvidelse av vedtakslengde sak $sakId")
                val utvidVedtakslengdeBehandling = opprettNyBehandling(sakId)
                prosesserBehandlingService.triggProsesserBehandling(utvidVedtakslengdeBehandling)
            } else {
                log.info("Sak med id $sakId trenger ikke utvidelse av vedtakslengde, hopper over")
            }
        } else {
            log.info("Sak med id $sakId har ingen gjeldende behandlinger, hopper over")
        }
    }

    private fun harBehovForUtvidetVedtakslengde(
        behandlingId: BehandlingId,
        sakId: SakId,
        underveisGrunnlag: UnderveisGrunnlag,
        datoForUtvidelse: LocalDate
    ): Boolean {
        val sisteVedtatteUnderveisperiode = underveisGrunnlag.perioder.maxByOrNull { it.periode.tom }
        val rettighetstypeTidslinje = vilkårsresultatRepository.hent(behandlingId).rettighetstypeTidslinje()

        if (sisteVedtatteUnderveisperiode != null) {
            val harFremtidigRettBistandsbehov = skalUtvide(
                forrigeSluttdato = sisteVedtatteUnderveisperiode.periode.tom,
                rettighetstypeTidslinjeForInneværendeBehandling = rettighetstypeTidslinje
            )

            val gjeldendeSluttdato = sisteVedtatteUnderveisperiode.periode.tom

            log.info("Sak $sakId har harFremtidigRettBistandsbehov=$harFremtidigRettBistandsbehov og gjeldendeSluttdato=$gjeldendeSluttdato")
            return gjeldendeSluttdato.isBefore(datoForUtvidelse) && harFremtidigRettBistandsbehov
        }
        log.info("Sak $sakId har ingen vedtatte underveisperioder")
        return false
    }

    fun skalUtvide(
        forrigeSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        return harFremtidigRettOrdinær(forrigeSluttdato, rettighetstypeTidslinjeForInneværendeBehandling)
                && now(clock).plusDays(28) >= forrigeSluttdato
    }

    // Det finnes en fremtidig periode med ordinær rett og gjenværende kvote
    fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        rettighetstypeTidslinjeForInneværendeBehandling: Tidslinje<RettighetsType>
    ): Boolean {
        val varighetstidslinje = VarighetRegel().simluer(rettighetstypeTidslinjeForInneværendeBehandling)
        return varighetstidslinje.begrensetTil(Periode(vedtattSluttdato.plusDays(1), Tid.MAKS))
            .segmenter()
            .any { varighetSegment ->
                varighetSegment.verdi.brukerAvKvoter.any { kvote -> kvote == Kvote.ORDINÆR }
                        && varighetSegment.verdi !is Avslag
            }

    }

    private fun opprettNyBehandling(sakId: SakId): SakOgBehandlingService.OpprettetBehandling =
        sakOgBehandlingService.finnEllerOpprettBehandling(
            sakId = sakId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                årsak = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = Vurderingsbehov.UTVID_VEDTAKSLENGDE))
            ),
        )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettBehandlingUtvidVedtakslengdeJobbUtfører(
                prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
                vilkårsresultatRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.UtvidVedtakslengdeJobbUtfører"
        override val navn = "Utvid vedtakslengde for saker"
        override val beskrivelse = "Skal trigge behandling som utvider vedtakslengde for saker som er i ferd med å nå sluttdato"
    }
}