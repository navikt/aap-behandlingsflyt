package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopiererImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import java.time.LocalDate
import java.time.LocalDateTime

class OpprettBehandlingFastsattPeriodePassertJobbUtfører(
    private val sakService: SakService,
    private val behandlingRepository: BehandlingRepository,
    private val underveisRepository: UnderveisRepository,
    private val sakOgBehandlingService: SakOgBehandlingService,
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val sak = sakService.hent(SakId(input.sakId()))

        if (!sak.rettighetsperiode.inneholder(LocalDate.now())) {
            return
        }

        val behandling = behandlingRepository.finnSisteBehandlingFor(
            sak.id, listOf(
                TypeBehandling.Førstegangsbehandling,
                TypeBehandling.Revurdering
            )
        ) ?: return

        if (behandling.status().erÅpen() && ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT in behandling.årsaker().map { it.type }) {
            return
        }

        val underveisperioder = underveisRepository.hentHvisEksisterer(behandling.id)
            ?.perioder
            ?: return

        val førsteAntatteMeldeperiode = underveisperioder
            .filter { it.meldepliktStatus == MeldepliktStatus.FREMTIDIG_OPPFYLT }
            .minByOrNull { it.meldePeriode }
            ?: return

        val tidspunktForKjøringVedManglendeMeldekort =
            førsteAntatteMeldeperiode.meldePeriode.fom.plusDays(8).atStartOfDay().plusHours(2)

        if (LocalDateTime.now() < tidspunktForKjøringVedManglendeMeldekort) {
            return
        }

        sakOgBehandlingService.finnEllerOpprettBehandling(
            sak.id,
            listOf(
                Årsak(
                    type = ÅrsakTilBehandling.FASTSATT_PERIODE_PASSERT,
                )
            )
        )
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryRegistry.provider(connection)
            return OpprettBehandlingFastsattPeriodePassertJobbUtfører(
                sakService = SakService(
                    sakRepository = repositoryProvider.provide(),
                ),
                behandlingRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                sakOgBehandlingService = SakOgBehandlingService(
                    grunnlagKopierer = GrunnlagKopiererImpl(repositoryProvider),
                    sakRepository = repositoryProvider.provide(),
                    behandlingRepository = repositoryProvider.provide(),
                )
            )
        }

        override fun type(): String {
            return "batch.OpprettBehandlingFastsattPeriodePassert"
        }

        override fun navn(): String {
            return "Opprett behandling fordi fastsatt dag er passert"
        }

        override fun beskrivelse(): String {
            return """
                Starter ny behandling hvis siste behandlig har antatt at meldeplikten er oppfylt, men
                fastsatt dag er passert, og meldekort nå mangler.
            """.trimIndent()
        }
    }
}
