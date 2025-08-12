package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.meldekort.kontrakt.sak.SakStatus
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class MeldeperiodeTilMeldekortBackendJobbUtfører(
    private val sakService: SakService,
    private val meldekortGateway: MeldekortGateway,
    private val behandlingRepository: BehandlingRepository,
    private val meldeperiodeRepository: MeldeperiodeRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val vedtakRepository: VedtakRepository,
    private val trukketSøknadService: TrukketSøknadService,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        val sak = sakService.hent(sakId)
        val behandling = behandlingRepository.hent(behandlingId)
        val meldeperioder = meldeperiodeRepository.hent(behandlingId)

        val opplysningerTilMeldekortBackend = when {
            trukketSøknadService.søknadErTrukket(behandling.id) ->
                opplysningerVedTrukketSøknad(sak)

            behandling.status().erAvsluttet() ->
                opplysningerVedVedtak(
                    sak = sak,
                    meldeperioder = meldeperioder,
                    vedtak = vedtakRepository.hent(behandling.id),
                    meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id),
                    underveisGrunnlag = underveisRepository.hentHvisEksisterer(behandling.id)
                )

            behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling ->
                opplysningerFørVedtak(sak, meldeperioder)

            else -> null
        }

        if (opplysningerTilMeldekortBackend != null) {
            val antallMeldePerioder = opplysningerTilMeldekortBackend.meldeperioder.size
            val antallOpplysningsbehov = opplysningerTilMeldekortBackend.opplysningsbehov.size
            log.info("Sender $antallMeldePerioder meldeperioder og $antallOpplysningsbehov opplysningsbehov til meldekort-backend for behandling $behandlingId")
            meldekortGateway.oppdaterMeldeperioder(opplysningerTilMeldekortBackend)
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        override val navn = "MeldeperiodeTilMeldekortBackend"
        override val type = "flyt.meldeperiodeTilMeldekortBackend"
        override val beskrivelse = """
                Push informasjon til meldekort-backend slik at vi kan åpne for
                innsending av meldekort før vedtak er fattet.
                """.trimIndent()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return MeldeperiodeTilMeldekortBackendJobbUtfører(
                sakService = SakService(repositoryProvider),
                meldekortGateway = gatewayProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                meldepliktRepository = repositoryProvider.provide(),
                vedtakRepository = repositoryProvider.provide(),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
            )
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldeperiodeTilMeldekortBackendJobbUtfører)
            .apply {
                forBehandling(sakId.toLong(), behandlingId.toLong())
            }

        internal fun opplysningerVedTrukketSøknad(sak: Sak) =
            MeldeperioderV0(
                identer = sak.person.identer().map { it.identifikator },
                saksnummer = sak.saksnummer.toString(),
                sakStatus = SakStatus.AVSLUTTET,
                sakenGjelderFor = sak.rettighetsperiode.somKontraktperiode,
                meldeperioder = listOf(),
                meldeplikt = listOf(),
                opplysningsbehov = listOf(),
            )

        internal fun opplysningerVedVedtak(
            sak: Sak,
            meldeperioder: List<no.nav.aap.komponenter.type.Periode>,
            vedtak: Vedtak?,
            meldepliktGrunnlag: MeldepliktGrunnlag?,
            underveisGrunnlag: UnderveisGrunnlag?
        ): MeldeperioderV0 {
            val underveisperioder = underveisGrunnlag
                ?.perioder
                .orEmpty()
                .map { Segment(it.periode, it) }
                .let(::Tidslinje)

            val fritaksvurderinger: Tidslinje<Fritaksvurdering.FritaksvurderingData> =
                meldepliktGrunnlag
                    ?.tilTidslinje()
                    ?: Tidslinje()

            return MeldeperioderV0(
                sakStatus = when (sak.status()) {
                    Status.OPPRETTET -> null
                    Status.UTREDES -> SakStatus.UTREDES
                    Status.LØPENDE -> SakStatus.LØPENDE
                    Status.AVSLUTTET -> SakStatus.AVSLUTTET
                },
                saksnummer = sak.saksnummer.toString(),
                identer = sak.person.identer().map { it.identifikator },
                sakenGjelderFor = sak.rettighetsperiode.somKontraktperiode,
                meldeperioder = meldeperioder.somKontraktperioder,
                opplysningsbehov = underveisperioder
                    .mapValue { it.rettighetsType != null }
                    .filter { it.verdi }
                    .komprimer()
                    .map { it.periode }
                    .somKontraktperioder,
                meldeplikt = MeldepliktRegel().fastsatteDagerMedMeldeplikt(
                    vedtaksdatoFørstegangsbehandling = vedtak?.virkningstidspunkt,
                    fritak = fritaksvurderinger,
                    meldeperioder = meldeperioder,
                    underveis = underveisperioder
                )
                    .somKontraktperioder
            )
        }

        internal fun opplysningerFørVedtak(
            sak: Sak,
            meldeperioder: List<no.nav.aap.komponenter.type.Periode>
        ): MeldeperioderV0 = MeldeperioderV0(
            saksnummer = sak.saksnummer.toString(),
            identer = sak.person.identer().map { it.identifikator },
            sakenGjelderFor = sak.rettighetsperiode.somKontraktperiode,
            meldeperioder = meldeperioder.somKontraktperioder,
            opplysningsbehov = listOf(sak.rettighetsperiode.somKontraktperiode),
            meldeplikt = emptyList(),
        )

        private val no.nav.aap.komponenter.type.Periode.somKontraktperiode: Periode
            get() = Periode(fom, tom)

        private val List<no.nav.aap.komponenter.type.Periode>.somKontraktperioder: List<Periode>
            get() = map { it.somKontraktperiode }
    }
}