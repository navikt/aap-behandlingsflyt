package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
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
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val vedtakRepository: VedtakRepository,
    private val samIdRepository: SamIdRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
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
        val vilkårsresultatTidslinje = vilkårsresultatRepository.hent(behandling.id).rettighetstypeTidslinje()
        val vedtakId = vedtakRepository.hentId(behandling.id)
        val samId = samIdRepository.hentHvisEksisterer(behandling.id)

        val beregningsgrunnlagGUnit =
            requireNotNull(beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)) { "Fant ikke beregningsgrunnlag for behandling $behandlingId" }.grunnlaget()

        val startPåRettighetsperiode = sak.rettighetsperiode.fom
        val grunnbeløpVedSakensStart = requireNotNull(
            Grunnbeløp.tilTidslinje().begrensetTil(sak.rettighetsperiode).segment(startPåRettighetsperiode)
        ) { "Fant ikke grunnbeløp på tidspunkt $startPåRettighetsperiode" }

        val beregningsgrunnlagIKroner = grunnbeløpVedSakensStart.verdi.multiplisert(beregningsgrunnlagGUnit).verdi

        apiInternGateway.sendBehandling(
            sak,
            behandling,
            vedtakId,
            samId,
            tilkjentYtelse,
            beregningsgrunnlagIKroner,
            underveis?.perioder.orEmpty(),
            vedtaksTidspunkt.toLocalDate(),
            vilkårsresultatTidslinje
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val beskrivelse = "Sender data rundt behandling til api-intern."
        override val navn = "BehandlingDatadelingUtfører"
        override val type = "flyt.DatadelingBehandlingsdata"

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return DatadelingBehandlingJobbUtfører( //TODO Thao: Sjekk om jeg trenger å logge i statistikk for kanseller revurdering
                apiInternGateway = gatewayProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                tilkjentRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                vilkårsresultatRepository = repositoryProvider.provide(),
                vedtakRepository = repositoryProvider.provide(),
                samIdRepository = repositoryProvider.provide(),
                beregningsgrunnlagRepository = repositoryProvider.provide(),
            )
        }
    }
}