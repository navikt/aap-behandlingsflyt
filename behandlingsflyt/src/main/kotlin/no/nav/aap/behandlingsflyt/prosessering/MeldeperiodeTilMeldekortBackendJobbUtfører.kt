package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.meldekort.kontrakt.Periode
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.meldekort.kontrakt.sak.SakStatus
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

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

    override fun utfør(input: JobbInput) {
        val sakId = SakId(input.sakId())
        val behandlingId = BehandlingId(input.behandlingId())
        val sak = sakService.hent(sakId)
        val behandling = behandlingRepository.hent(behandlingId)

        val identer = sak.person.identer().map { it.identifikator }
        val meldeperioder = meldeperiodeRepository.hent(behandlingId)
        val sakenGjelderFor = Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)

        if (trukketSøknadService.søknadErTrukket(behandlingId)) {
            meldekortGateway.oppdaterMeldeperioder(
                MeldeperioderV0(
                    identer = identer,
                    saksnummer = sak.saksnummer.toString(),
                    sakStatus = SakStatus.AVSLUTTET,
                    sakenGjelderFor = sakenGjelderFor,
                    meldeperioder = listOf(),
                    meldeplikt = listOf(),
                    opplysningsbehov = listOf(),
                )
            )
        } else if (behandling.status().erAvsluttet()) {
            val underveisperioder = underveisRepository.hentHvisEksisterer(behandlingId)
                ?.perioder
                .orEmpty()
                .map { Segment(it.periode, it) }
                .let(::Tidslinje)

            val fritaksvurderinger: Tidslinje<Fritaksvurdering.FritaksvurderingData> =
                meldepliktRepository.hentHvisEksisterer(behandlingId)
                    ?.tilTidslinje()
                    ?: Tidslinje()

            meldekortGateway.oppdaterMeldeperioder(
                MeldeperioderV0(
                    sakStatus = mapStatusTilMeldekortSakStatus(sak.status()),
                    saksnummer = sak.saksnummer.toString(),
                    identer = identer,
                    sakenGjelderFor = sakenGjelderFor,
                    meldeperioder = meldeperioder
                        .map { Periode(it.fom, it.tom) },
                    opplysningsbehov =
                        underveisperioder
                            .mapNotNull { if (it.verdi.utfall == Utfall.OPPFYLT) it.periode else null }
                            .map { Periode(it.fom, it.tom) },
                    meldeplikt = MeldepliktRegel()
                        .fastsatteDagerMedMeldeplikt(
                            vedtaksdatoFørstegangsbehandling = vedtakRepository.hent(behandlingId)?.virkningstidspunkt,
                            fritak = fritaksvurderinger,
                            meldeperioder = meldeperioder,
                            underveis = underveisperioder
                        )
                        .map { Periode(it.fom, it.tom) }
                )
            )
        } else if (behandling.typeBehandling() == TypeBehandling.Førstegangsbehandling) {
            meldekortGateway.oppdaterMeldeperioder(
                MeldeperioderV0(
                    saksnummer = sak.saksnummer.toString(),
                    identer = identer,
                    sakenGjelderFor = sakenGjelderFor,
                    meldeperioder = meldeperioder
                        .map { Periode(it.fom, it.tom) },
                    opplysningsbehov = listOf(Periode(sak.rettighetsperiode.fom, sak.rettighetsperiode.tom)),
                    meldeplikt = emptyList(),
                )
            )
        }
    }

    private fun mapStatusTilMeldekortSakStatus(status: Status): SakStatus? {
        return when (status) {
            Status.OPPRETTET -> null
            Status.UTREDES -> SakStatus.UTREDES
            Status.LØPENDE -> SakStatus.LØPENDE
            Status.AVSLUTTET -> SakStatus.AVSLUTTET
        }
    }

    companion object : Jobb {
        override fun beskrivelse(): String {
            return """
                Push informasjon til meldekort-backend slik at vi kan åpne for
                innsending av meldekort før vedtak er fattet.
                """.trimIndent()
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryRegistry.provider(connection)

            return MeldeperiodeTilMeldekortBackendJobbUtfører(
                sakService = SakService(repositoryProvider),
                meldekortGateway = GatewayProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                meldeperiodeRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                meldepliktRepository = repositoryProvider.provide(),
                vedtakRepository = repositoryProvider.provide(),
                trukketSøknadService = TrukketSøknadService(repositoryProvider),
            )
        }

        override fun navn(): String {
            return "MeldeperiodeTilMeldekortBackend"
        }

        override fun type(): String {
            return "flyt.meldeperiodeTilMeldekortBackend"
        }

        fun nyJobb(
            sakId: SakId,
            behandlingId: BehandlingId,
        ) = JobbInput(MeldeperiodeTilMeldekortBackendJobbUtfører)
            .apply {
                forBehandling(sakId.toLong(), behandlingId.toLong())
            }
    }
}