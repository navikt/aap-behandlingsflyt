package no.nav.aap.behandlingsflyt.behandling.utbetaling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDetaljerDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelseDto
import no.nav.aap.utbetal.tilkjentytelse.TilkjentYtelsePeriodeDto
import java.time.LocalDate
import java.time.LocalDateTime

class UtbetalingService(
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val vedtakRepository: VedtakRepository,
    private val refusjonskravRepository: RefusjonkravRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val samordningAndreStatligeYtelserRepository: SamordningAndreStatligeYtelserRepository,
    private val samordningArbeidsgiverRepository: SamordningArbeidsgiverRepository,
    private val underveisRepository: UnderveisRepository,
    private val unleashGateway: UnleashGateway,
) {
    constructor(
        repositoryProvider: RepositoryProvider,
        gatewayProvider: GatewayProvider,
    ) : this(
        sakRepository = repositoryProvider.provide<SakRepository>(),
        behandlingRepository = repositoryProvider.provide<BehandlingRepository>(),
        tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>(),
        avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>(),
        vedtakRepository = repositoryProvider.provide<VedtakRepository>(),
        refusjonskravRepository = repositoryProvider.provide<RefusjonkravRepository>(),
        tjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide<TjenestepensjonRefusjonsKravVurderingRepository>(),
        samordningAndreStatligeYtelserRepository = repositoryProvider.provide<SamordningAndreStatligeYtelserRepository>(),
        samordningArbeidsgiverRepository = repositoryProvider.provide<SamordningArbeidsgiverRepository>(),
        underveisRepository = repositoryProvider.provide<UnderveisRepository>(),
        unleashGateway = gatewayProvider.provide<UnleashGateway>()
    )


    fun lagTilkjentYtelseForUtbetaling(sakId: SakId, behandlingId: BehandlingId, simulering: Boolean = false): TilkjentYtelseDto? {
        val sak = sakRepository.hent(sakId)
        val behandling = behandlingRepository.hent(behandlingId)

        if (simulering && behandling.status() == Status.AVSLUTTET) {
            //Utbetaling skal ikke simuleres dersom behandling er avsluttet
            return null
        }

        val forrigeBehandling = behandling.forrigeBehandlingId?.let { behandlingRepository.hent(it) }
        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        return if (tilkjentYtelse != null) {
            val saksnummer = sak.saksnummer.toString()
            val behandlingsreferanse = behandling.referanse.referanse
            val forrigeBehandlingRef = forrigeBehandling?.referanse?.referanse
            val personIdent = sak.person.aktivIdent().identifikator
            val avklaringsbehovForeslåVedtak = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FORESLÅ_VEDTAK)
            val avklaringsbehovFatteVedtak = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FATTE_VEDTAK)
            val saksbehandlerIdent = avklaringsbehovForeslåVedtak?.endretAv() ?: "Kelvin"
            val beslutterIdent = avklaringsbehovFatteVedtak?.endretAv() ?: "Kelvin"
            val vedtakstidspunkt = if (simulering) {
                LocalDateTime.now()
            } else {
                vedtakRepository.hent(behandlingId)?.vedtakstidspunkt ?: error("Fant ikke vedtak")
            }
            val avventUtbetaling = if (tilkjentYtelse.isNotEmpty()) {
                val førsteVedtaksdato = finnFørsteVedtaksdato(sakId) ?: LocalDate.now()
                val avventUtbetalingService = AvventUtbetalingService(
                    refusjonskravRepository = refusjonskravRepository,
                    tjenestepensjonRefusjonsKravVurderingRepository = tjenestepensjonRefusjonsKravVurderingRepository,
                    samordningAndreStatligeYtelserRepository = samordningAndreStatligeYtelserRepository,
                    samordningArbeidsgiverYtelserRepository = samordningArbeidsgiverRepository,
                    unleashGateway = unleashGateway,
                )
                avventUtbetalingService.finnEventuellAvventUtbetaling(behandlingId, førsteVedtaksdato, tilkjentYtelse.finnHelePerioden())
            } else {
                null
            }
            TilkjentYtelseDto(
                saksnummer = saksnummer,
                behandlingsreferanse = behandlingsreferanse,
                forrigeBehandlingsreferanse  = forrigeBehandlingRef,
                personIdent = personIdent,
                vedtakstidspunkt = vedtakstidspunkt,
                beslutterId = beslutterIdent,
                saksbehandlerId = saksbehandlerIdent,
                perioder = tilkjentYtelse.tilTilkjentYtelsePeriodeDtoer(),
                avvent = avventUtbetaling,
            )

        } else {
            null
        }
    }

    private fun finnFørsteVedtaksdato(sakId: SakId): LocalDate? {
        val behandlinger = behandlingRepository.hentAlleFor(sakId)
            .sortedBy { it.opprettetTidspunkt }

        val avsluttedeBehandlinger = behandlinger.filter {it.status().erAvsluttet()}

        for (avsluttedeBehandling in avsluttedeBehandlinger) {
            val harOppfyltPeriode = underveisRepository.hentHvisEksisterer(avsluttedeBehandling.id)
                ?.perioder
                .orEmpty()
                .any { it.utfall == Utfall.OPPFYLT }

            if (harOppfyltPeriode) {
                val vedtak = vedtakRepository.hent(avsluttedeBehandling.id)
                if (vedtak != null) {
                    return vedtak.vedtakstidspunkt.toLocalDate()
                }
            }
        }
        return null
    }

    private fun List<TilkjentYtelsePeriode>.finnHelePerioden() =
        Periode(
            fom = this.minOf { it.periode.fom },
            tom = this.maxOf { it.periode.tom }
        )

    private fun List<TilkjentYtelsePeriode>.tilTilkjentYtelsePeriodeDtoer() =
        map { segment ->
            val periode = segment.periode
            val detaljer = segment.tilkjent
            TilkjentYtelsePeriodeDto(
                fom = periode.fom,
                tom = periode.tom,
                detaljer = TilkjentYtelseDetaljerDto(
                    redusertDagsats = detaljer.redusertDagsats().verdi(),
                    gradering = detaljer.gradering.endeligGradering.prosentverdi(),
                    dagsats = detaljer.dagsats.verdi(),
                    grunnlag = detaljer.dagsats.verdi(),
                    grunnlagsfaktor = detaljer.grunnlagsfaktor.verdi(),
                    grunnbeløp = detaljer.grunnbeløp.verdi(),
                    antallBarn = detaljer.antallBarn,
                    barnetilleggsats = detaljer.barnetilleggsats.verdi(),
                    barnetillegg = detaljer.barnetillegg.verdi(),
                    utbetalingsdato = detaljer.utbetalingsdato
                )
            )
        }
}