package no.nav.aap.behandlingsflyt.behandling.vedtakslengde

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.behandling.underveis.KvoteService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class VedtakslengdeService(
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) {
    companion object {
        const val ANTALL_DAGER_FØR_UTVIDELSE = 28L
    }
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeRepository =  repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
    )

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        return underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(datoForUtvidelse)
    }

    fun skalUtvideSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        datoForUtvidelse: LocalDate = LocalDate.now(clock).plusDays(ANTALL_DAGER_FØR_UTVIDELSE)
    ): Boolean {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtattSluttdato != null) {
            val nesteUtvidelse = hentNesteUtvidelse(vedtakslengdeGrunnlag?.vurdering)
            val utvidetSluttdato = vedtattSluttdato.plussEtÅrMedHverdager(nesteUtvidelse)

            val harFremtidigRettOrdinær = harFremtidigRettOrdinær(vedtattSluttdato, utvidetSluttdato, behandlingId)
            log.info("Behandling $behandlingId har harFremtidigRettOrdinær=$harFremtidigRettOrdinær og forrigeSluttdato=${vedtattSluttdato}")

            return datoForUtvidelse >= vedtattSluttdato && harFremtidigRettOrdinær
        } else {
            log.info("Behandling $behandlingId har ingen vedtatt sluttdato, ingen utvidelse nødvendig")
        }
        return false
    }

    fun utvidSluttdato(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
    ) {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtattSluttdato != null) {
            val forrigeUtvidelse = vedtakslengdeGrunnlag?.vurdering
            val utvidelse = hentNesteUtvidelse(forrigeUtvidelse)

            val nySluttdato = vedtattSluttdato.plussEtÅrMedHverdager(utvidelse)
            vedtakslengdeRepository.lagre(
                behandlingId, VedtakslengdeVurdering(
                    sluttdato = nySluttdato,
                    utvidetMed = utvidelse,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock)
                )
            )
        } else {
            log.info("Behandling $behandlingId har ingen vedtatt sluttdato, ingen utvidelse nødvendig")
        }
    }

    fun lagreGjeldendeSluttdatoHvisIkkeEksisterer(
        behandlingId: BehandlingId,
        forrigeBehandlingId: BehandlingId?,
        rettighetsperiode: Periode,
    ) {
        val vedtakslengdeGrunnlag = forrigeBehandlingId?.let { vedtakslengdeRepository.hentHvisEksisterer(forrigeBehandlingId) }
        val vedtattSluttdato = hentVedtattSluttdato(forrigeBehandlingId, vedtakslengdeGrunnlag)

        if (vedtakslengdeGrunnlag == null) {
            val sluttdato = vedtattSluttdato ?: utledInitiellSluttdato(behandlingId, rettighetsperiode).tom

            // Skal lagre ned vedtakslengde for eksisterende behandlinger som mangler dette
            vedtakslengdeRepository.lagre(
                behandlingId, VedtakslengdeVurdering(
                    sluttdato = sluttdato,
                    utvidetMed = ÅrMedHverdager.FØRSTE_ÅR,
                    vurdertAv = SYSTEMBRUKER,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.now(clock)
                )
            )
        }
    }

    /**
     * Henter siste vedtatte sluttdato.
     * - Foretrekker siste vedtatte vedtakslengdevurdering dersom den finnes.
     * - Hvis ikke velges siste vedtatte underveisperiode (målet burde være å bli kvitt dette når alle saker har
     *   vedtakslengdeVurdering.
     */
    private fun hentVedtattSluttdato(forrigeBehandlingId: BehandlingId?, vedtakslengdeGrunnlag: VedtakslengdeGrunnlag?): LocalDate? {
        val vedtattUnderveis = forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val sluttdatoSisteVedtatteUnderveis = vedtattUnderveis?.perioder?.maxByOrNull { it.periode.tom }?.periode?.tom
        val sluttdatoSisteVedtatteVedtakslengdeVurdering = vedtakslengdeGrunnlag?.vurdering?.sluttdato

        return sluttdatoSisteVedtatteVedtakslengdeVurdering ?: sluttdatoSisteVedtatteUnderveis
    }

    private fun hentNesteUtvidelse(forrigeUtvidelse: VedtakslengdeVurdering?): ÅrMedHverdager =
        when (forrigeUtvidelse?.utvidetMed) {
            null, ÅrMedHverdager.FØRSTE_ÅR -> ÅrMedHverdager.ANDRE_ÅR // Antar at man skal utvide med andre år dersom grunnlag ikke finnes
            ÅrMedHverdager.ANDRE_ÅR -> ÅrMedHverdager.TREDJE_ÅR
            ÅrMedHverdager.TREDJE_ÅR, ÅrMedHverdager.ANNET -> ÅrMedHverdager.ANNET
        }

    private fun utledInitiellSluttdato(
        behandlingId: BehandlingId,
        rettighetsperiode: Periode
    ): Periode {
        val startdatoForBehandlingen =
            VirkningstidspunktUtleder(vilkårsresultatRepository).utledVirkningsTidspunkt(behandlingId)
                ?: rettighetsperiode.fom

        /**
         * Det første år inkluderes startdatoen, og en dag på slutten må trekkes ifra for at det skal bli 261 dager
         */
        val sluttdatoForBehandlingen = startdatoForBehandlingen
            .plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)

        /**
         * For behandlinger som har passert alle vilkår og vurderinger med kortere rettighetsperiode
         * enn "sluttdatoForBehandlingen" så vil det bli feil å vurdere underveis lenger enn faktisk rettighetsperiode.
         */
        val sluttdatoForBakoverkompabilitet = minOf(rettighetsperiode.tom, sluttdatoForBehandlingen)

        return Periode(rettighetsperiode.fom, sluttdatoForBakoverkompabilitet)
    }

    /**
     * Neste periode (hele året) er av type ordinær med gjenværende kvote
     */
    private fun harFremtidigRettOrdinær(
        vedtattSluttdato: LocalDate,
        utvidetSluttdato: LocalDate,
        behandlingId: BehandlingId,
    ): Boolean {
        val nyUtvidetVedtaksperiode = Periode(vedtattSluttdato.plusDays(1), utvidetSluttdato)
        val nyUtvidetVedtaksperiodeTidslinje = Tidslinje(nyUtvidetVedtaksperiode, true)

        return vurderRettighetstypeOgKvoter(vilkårsresultatRepository.hent(behandlingId), KvoteService().beregn())
            .rightJoin(nyUtvidetVedtaksperiodeTidslinje) { vurdering, _ ->
                vurdering != null && vurdering is KvoteOk && Kvote.ORDINÆR in vurdering.brukerAvKvoter()
            }
            .segmenter()
            .all { it.verdi }
    }
}