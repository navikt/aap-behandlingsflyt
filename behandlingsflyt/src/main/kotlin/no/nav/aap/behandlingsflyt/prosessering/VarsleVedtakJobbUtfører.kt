package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class VarsleVedtakJobbUtfører(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val underveisRepository: UnderveisRepository,
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val behandlingService: BehandlingService,
    private val samGateway: SamGateway
) : JobbUtfører {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        vedtakRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        tjenestePensjonRepository = repositoryProvider.provide(),
        flytJobbRepository = repositoryProvider.provide(),
        behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
        samGateway = gatewayProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtak = vedtakRepository.hent(behandling.id)
        val vedtakId =
            requireNotNull(vedtakRepository.hentId(behandling.id)) { "Fant ikke vedtak for behandlingId $behandlingId." }
        val forrigeBehandlingId = behandling.forrigeBehandlingId

        val forrigeUnderveisGrunnlag = forrigeBehandlingId?.let { underveisRepository.hentHvisEksisterer(it) }
        val nåværendeUnderveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)


        requireNotNull(vedtak) { "Forventer at vedtak-objekter er lagret når denne jobben kjøres." }

        // Hvis avslag, bruk vedtakstidspunkt som fallback
        val virkFom = vedtak.virkningstidspunkt ?: vedtak.vedtakstidspunkt.toLocalDate()

        val request = SamordneVedtakRequest(
            pid = sak.person.aktivIdent().identifikator,
            vedtakId = vedtakId.toString(),
            sakId = sak.id.id,
            virkFom = virkFom,
            virkTom = sak.rettighetsperiode.tom,
            fagomrade = "AAP",
            ytelseType = "AAP",
            etterbetaling = vedtak.virkningstidspunkt?.let {
                val vedtaksTidspunkt =
                    requireNotNull(vedtak.vedtakstidspunkt) { "Vedtakstidspunkt kan ikke være null. Virkningstidspunkt: ${vedtak.virkningstidspunkt}" }
                vedtaksTidspunkt.toLocalDate() > it
            } ?: false,
            utvidetFrist = null,
        )

        val behandlingType = behandlingService.utledFaktiskBehandlingstype(behandling)

        val førstegangsbehandling = behandlingType == TypeBehandling.Førstegangsbehandling
        val endringIRettighetsTypeTidslinje = endringIRettighetstypeTidslinje(
            forrigeUnderveisGrunnlag,
            nåværendeUnderveisGrunnlag!!
        )

        val tpYtelser =
            tjenestePensjonRepository.hentHvisEksisterer(behandling.id).orEmpty()
                .flatMap { it.ytelser }

        val relevantEndring =
            listOf(
                førstegangsbehandling,
                endringIRettighetsTypeTidslinje
            )


        if (relevantEndring.contains(true) && tpYtelser.isNotEmpty()) {
            log.info("Varsler SAM for behandling med referanse ${behandling.referanse} og saksnummer ${sak.saksnummer}. Årsak: førstegangsbehandling=${førstegangsbehandling}, endringIRettighetstype=${endringIRettighetsTypeTidslinje}")
            samGateway.varsleVedtak(request)
        } else {
            log.info("Varsler ikke SAM for behandling med referanse ${behandling.referanse} og saksnummer ${sak.saksnummer}. Årsak: førstegangsbehandling=${førstegangsbehandling}, endringIRettighetstype=${endringIRettighetsTypeTidslinje}, tpYtelser=${tpYtelser.size}")
        }

        flytJobbRepository.leggTil(JobbInput(HentSamIdJobbUtfører).medPayload(behandling.id).forSak(sak.id.id))
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return VarsleVedtakJobbUtfører(repositoryProvider, gatewayProvider,)
        }

        fun endringITilkjentYtelseTidslinje(
            forrigeTilkjentYtelse: Tidslinje<Tilkjent>?,
            nåværendeTilkjentYtelse: Tidslinje<Tilkjent>?
        ): Boolean {
            if (forrigeTilkjentYtelse == null && nåværendeTilkjentYtelse == null) return false
            else if (forrigeTilkjentYtelse == null) return true

            requireNotNull(nåværendeTilkjentYtelse) { "Hvis forrigeTilkjentYtelse ikke er null, så kan ikke nåværendeTilkjentYtelse være det." }

            return forrigeTilkjentYtelse.komprimer()
                .outerJoin(nåværendeTilkjentYtelse.komprimer()) { left: Tilkjent?, right: Tilkjent? ->
                    when {
                        left == null && right == null -> false
                        left == null -> right?.gradering != Prosent.`0_PROSENT`
                        right == null -> left.gradering != Prosent.`0_PROSENT`
                        else -> {
                            val leftErNull = left.gradering == Prosent.`0_PROSENT`
                            val rightErNull = right.gradering == Prosent.`0_PROSENT`
                            val positivEndringDagsats = (left.dagsats.verdi) < (right.dagsats.verdi)
                            (leftErNull != rightErNull) || positivEndringDagsats
                        }
                    }
                }.filter { it.verdi }.isNotEmpty()

        }

        override val beskrivelse = "Varsler om endring nytt eller endring i vedtak til SAM"
        override val navn = "VarsleVedtakSam"
        override val type = "flyt.Varsler"
    }

    fun underveisTilRettighetsTypeTidslinje(
        underveis: UnderveisGrunnlag?
    ): Tidslinje<RettighetsType> {
        return underveis?.somTidslinje().orEmpty()
            .mapNotNull { it.rettighetsType }
            .komprimer()
    }

    fun endringIRettighetstypeTidslinje(
        forrigeUnderveisGrunnlag: UnderveisGrunnlag?,
        nåværendeUnderveisGrunnlag: UnderveisGrunnlag
    ): Boolean {
        return forrigeUnderveisGrunnlag != null && underveisTilRettighetsTypeTidslinje(
            forrigeUnderveisGrunnlag
        ) != underveisTilRettighetsTypeTidslinje(nåværendeUnderveisGrunnlag)
    }
}