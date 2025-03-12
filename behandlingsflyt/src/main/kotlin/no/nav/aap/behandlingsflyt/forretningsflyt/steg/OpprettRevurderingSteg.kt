package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

class OpprettRevurderingSteg(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val samordningYtelseVurderingRepository: SamordningVurderingRepository
) : BehandlingSteg {
    private val logger = LoggerFactory.getLogger(javaClass)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurdering.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING -> {
                val samordningVurdering =
                    samordningYtelseVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
                        ?: return Fullført

                val maksDato =
                    if (samordningVurdering.maksDatoEndelig && samordningVurdering.maksDato != null) samordningVurdering.maksDato else null


                if (maksDato == null) return Fullført

                logger.info("Oppretter revurdering. SakID: ${kontekst.sakId}")
                sakOgBehandlingService.finnEllerOpprettBehandling(
                    sakId = kontekst.sakId,
                    årsaker = listOf(
                        Årsak(
                            type = ÅrsakTilBehandling.REVURDER_SAMORDNING,
                            periode = kontekst.vurdering.rettighetsperiode.flytt(
                                ChronoUnit.DAYS.between(
                                    kontekst.vurdering.rettighetsperiode.tom,
                                    samordningVurdering.maksDato
                                )
                            )
                        )
                    )
                )

                return Fullført
            }

            VurderingType.REVURDERING, VurderingType.FORLENGELSE, VurderingType.IKKE_RELEVANT -> {
                Fullført
            }
        }
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
                samordningYtelseVurderingRepository = repositoryProvider.provide()
            )
        }

        override fun type(): StegType {
            return StegType.OPPRETT_REVURDERING
        }
    }
}