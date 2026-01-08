package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.RettighetstypeVurdering
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDate
import kotlin.collections.mapNotNull
import kotlin.collections.orEmpty

class VarsleVedtakJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val samGateway: SamGateway = gatewayProvider.provide()
        val sakRepository: SakRepository = repositoryProvider.provide()
        val behandlingRepository: BehandlingRepository = repositoryProvider.provide()
        val vedtakRepository: VedtakRepository = repositoryProvider.provide()
        val underveisRepository: RettighetstypeVurdering = repositoryProvider.provide()

        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtak = vedtakRepository.hent(behandling.id)
        val vedtakId =
            requireNotNull(vedtakRepository.hentId(behandling.id)) { "Fant ikke vedtak for behandlingId $behandlingId." }
        val forrigeBehandlingId = behandling.forrigeBehandlingId

        val rettighetsPeriodeRepo = repositoryProvider.provide<VurderRettighetsperiodeRepository>()
        val forrigeRettighetsperiodeVurdering =  forrigeBehandlingId?.let{ rettighetsPeriodeRepo.hentVurdering(forrigeBehandlingId) }
        val nåværendeRettighetsperiodeVurdering = rettighetsPeriodeRepo.hentVurdering(behandling.id)

        val tilkjentRepository: TilkjentYtelseRepository = repositoryProvider.provide()
        val forrigeTilkjentYtelse = forrigeBehandlingId?.let { tilkjentRepository.hentHvisEksisterer(it) }
        val nåværendeTilkjentYtelse = tilkjentRepository.hentHvisEksisterer(behandling.id)

        val underveisRepo: UnderveisRepository = repositoryProvider.provide()
        val forrigeUnderveisGrunnlag = forrigeBehandlingId?.let { underveisRepo.hentHvisEksisterer(it) }
        val nåværendeUnderveisGrunnlag = underveisRepo.hentHvisEksisterer(behandling.id)


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
                    requireNotNull(vedtak.vedtakstidspunkt) { "Vedtakstidspunkt kan ikke være null. Men: virkningstidspunt: ${vedtak.virkningstidspunkt}" }
                vedtaksTidspunkt.toLocalDate() > it
            } ?: false,
            utvidetFrist = null,
        )

        val relevantEndring =
            endringIRettighetsPeriode(forrigeRettighetsperiodeVurdering,nåværendeRettighetsperiodeVurdering)
                    || endringITilkjentYtelseTidslinje(forrigeTilkjentYtelse,nåværendeTilkjentYtelse)
                    || endringIRettighetstypeTidslinje(forrigeUnderveisGrunnlag, nåværendeUnderveisGrunnlag!!)

        // For nå: kun varsle ved førstegangsbehandlinger.
        // På sikt skal vi varsle hver gang det skjer en "betydelig" endring i ytelsen. F.eks rettighetstype, stans,
        // etc.
        if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling || relevantEndring) {
            log.info("Varsler SAM for behandling med referanse ${behandling.referanse} og saksnummer ${sak.saksnummer}.")
            samGateway.varsleVedtak(request)
        }

        val flytJobbRepository: FlytJobbRepository = repositoryProvider.provide()
        flytJobbRepository.leggTil(JobbInput(HentSamIdJobbUtfører).medPayload(behandling.id).forSak(sak.id.id))
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return VarsleVedtakJobbUtfører(
                repositoryProvider,
                gatewayProvider,
            )
        }

        override val beskrivelse = "Varsler om endring nytt eller endring i vedtak til SAM"
        override val navn = "VarsleVedtakSam"
        override val type = "flyt.Varsler"
    }

    fun endringIRettighetsPeriode(prev: RettighetsperiodeVurdering?, curr:RettighetsperiodeVurdering?): Boolean{
        // blir dette riktig? vanskelig å sammenligne rettighetsperiode da den ligger på saken, som ikke kan sjekkes historisk med "forrigeBehandlingId"
        return prev==null || prev!=curr
    }

    fun endringITilkjentYtelseTidslinje(
        forrigeTilkjentYtelse: List<TilkjentYtelsePeriode>?,
        nåværendeTilkjentYtelse: List<TilkjentYtelsePeriode>?
    ): Boolean {
        return forrigeTilkjentYtelse!=nåværendeTilkjentYtelse
    }

    fun underveisTilRettighetsTypeTidslinje(underveis: UnderveisGrunnlag?): Tidslinje<RettighetsType> {
        return underveis?.perioder.orEmpty()
            .mapNotNull { if (it.rettighetsType != null) Segment(it.periode, it.rettighetsType) else null }
            .let(::Tidslinje).begrensetTil(Periode(LocalDate.MIN, LocalDate.now()))
    }

    fun endringIRettighetstypeTidslinje(forrigeUnderveisGrunnlag: UnderveisGrunnlag?, nåværendeUnderveisGrunnlag: UnderveisGrunnlag): Boolean {
        return forrigeUnderveisGrunnlag!=null && underveisTilRettighetsTypeTidslinje(forrigeUnderveisGrunnlag)!=underveisTilRettighetsTypeTidslinje(nåværendeUnderveisGrunnlag)
    }
}