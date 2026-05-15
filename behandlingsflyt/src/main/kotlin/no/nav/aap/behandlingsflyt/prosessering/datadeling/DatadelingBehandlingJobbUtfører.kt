package no.nav.aap.behandlingsflyt.prosessering.datadeling

import no.nav.aap.behandlingsflyt.behandling.StansOpphørService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class DatadelingBehandlingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vedtakRepository: VedtakRepository,
    private val samIdRepository: SamIdRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val stansOpphørService: StansOpphørService,
    private val rettighetstypeService: RettighetstypeService,
    private val utledArenaVedtakstype: UtledArenaVedtakstype,
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val (behandlingId, vedtaksTidspunkt) = input.payload<Pair<BehandlingId, LocalDateTime>>()
        val behandling = behandlingRepository.hent(behandlingId)

        if (behandling.typeBehandling() !in listOf(
                TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering
            )
        ) {
            log.info("Fant behandling av type ${behandling.typeBehandling()} - oversender ikke til api-intern")
            return
        }

        val sak = sakRepository.hent(behandling.sakId)
        val tilkjentYtelse = tilkjentRepository.hentHvisEksisterer(behandling.id)

        if (tilkjentYtelse == null) {
            log.warn("Fant ikke tilkjent ytelse for behandling $behandlingId. Sender ikke data til API Intern.")
            return
        }

        val underveis = underveisRepository.hentHvisEksisterer(behandling.id)

        val vilkårsresultatTidslinje = underveis?.somTidslinje().orEmpty()
            .mapNotNull { it.rettighetsType }

        val vedtakId = vedtakRepository.hentId(behandling.id)
        val samId = samIdRepository.hentHvisEksisterer(behandling.id)

        val beregningsgrunnlagGUnit =
            beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)?.grunnlaget()

        val startPåRettighetsperiode = sak.rettighetsperiode.fom
        val grunnbeløpVedSakensStart = Grunnbeløp.finnGrunnbeløp(startPåRettighetsperiode)

        val beregningsgrunnlagIKroner = beregningsgrunnlagGUnit?.multiplisert(grunnbeløpVedSakensStart)?.verdi

        val stansOpphør = stansOpphørService.vedtattStansOpphør(behandling.id).toSet()

        val maksdato = rettighetstypeService.sisteDagMedRett(sak.saksnummer)

        apiInternGateway.sendBehandling(
            sak = sak,
            behandling = behandling,
            vedtakId = vedtakId,
            samId = samId,
            tilkjent = tilkjentYtelse,
            beregningsgrunnlag = beregningsgrunnlagIKroner,
            vedtaksDato = vedtaksTidspunkt.toLocalDate(),
            rettighetsTypeTidslinje = vilkårsresultatTidslinje,
            muligMaksdato = maksdato,
            stansOpphørGrunnlag = stansOpphør,
            arenavedtak = utledArenaVedtakstype.utledVedtak(sak),
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val beskrivelse = "Sender data rundt behandling til api-intern."
        override val navn = "BehandlingDatadelingUtfører"
        override val type = "flyt.DatadelingBehandlingsdata"

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return DatadelingBehandlingJobbUtfører(
                apiInternGateway = gatewayProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                tilkjentRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                vedtakRepository = repositoryProvider.provide(),
                samIdRepository = repositoryProvider.provide(),
                beregningsgrunnlagRepository = repositoryProvider.provide(),
                stansOpphørService = StansOpphørService(
                    repositoryProvider.provide(),
                    repositoryProvider.provide(),
                    repositoryProvider.provide(),
                ),
                rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider),
                utledArenaVedtakstype = UtledArenaVedtakstype(repositoryProvider, gatewayProvider),
            )
        }
    }
}