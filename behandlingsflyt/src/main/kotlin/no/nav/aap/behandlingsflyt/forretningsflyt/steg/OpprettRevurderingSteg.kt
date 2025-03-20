package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.LoggerFactory

class OpprettRevurderingSteg(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository,
    private val låsRepository: TaSkriveLåsRepository,
    private val prosesserBehandling: ProsesserBehandlingService,
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                val samordningVurdering =
                    samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
                        ?: return Fullført

                if (!erUsikkerhetTilknyttetMaksSykepengerDato(samordningVurdering)) return Fullført

                logger.info("Oppretter revurdering. SakID: ${kontekst.sakId}")
                val beriketBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                    sakId = kontekst.sakId,
                    årsaker = listOf(
                        Årsak(
                            type = ÅrsakTilBehandling.REVURDER_SAMORDNING,
                        )
                    )
                )

                val behandlingSkrivelås =
                    låsRepository.låsBehandling(beriketBehandling.behandling.id)

                prosesserBehandling.triggProsesserBehandling(
                    beriketBehandling.behandling.sakId,
                    beriketBehandling.behandling.id,
                )
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)

                return Fullført
            }

            VurderingType.REVURDERING, VurderingType.FORLENGELSE, VurderingType.IKKE_RELEVANT -> {
                Fullført
            }
        }
    }

    private fun erUsikkerhetTilknyttetMaksSykepengerDato(samordningVurdering: SamordningVurderingGrunnlag): Boolean {
        return samordningVurdering.maksDatoEndelig != true && samordningVurdering.maksDato != null
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)

            return OpprettRevurderingSteg(
                SakOgBehandlingService(
                    grunnlagKopierer = GrunnlagKopierer(connection),
                    sakRepository = repositoryProvider.provide(),
                    behandlingRepository = repositoryProvider.provide(),
                ),
                samordningYtelseVurderingRepository = repositoryProvider.provide(),
                låsRepository = repositoryProvider.provide(),
                prosesserBehandling = ProsesserBehandlingService(FlytJobbRepository(connection))
            )
        }

        override fun type(): StegType {
            return StegType.OPPRETT_REVURDERING
        }
    }
}