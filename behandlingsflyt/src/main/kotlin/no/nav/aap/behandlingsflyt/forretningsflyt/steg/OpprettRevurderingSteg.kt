package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.*
import no.nav.aap.behandlingsflyt.hendelse.mottak.HåndterMottattDokumentService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
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

                val revurderingsDato =
                    if (samordningVurdering.maksDatoEndelig && samordningVurdering.maksDato != null) samordningVurdering.maksDato else null


                if (revurderingsDato == null) return Fullført

                logger.info("Oppretter revurdering. SakID: ${kontekst.sakId}")
                val beriketBehandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                    sakId = kontekst.sakId,
                    årsaker = listOf(
                        Årsak(
                            type = ÅrsakTilBehandling.REVURDER_SAMORDNING,
                        )
                    )
                )

                val behandlingSkrivelås = låsRepository.låsBehandling(beriketBehandling.behandling.id)

//                sakOgBehandlingService.oppdaterRettighetsperioden(beriketBehandling.behandling.sakId, brevkategori, mottattDato)

                prosesserBehandling.triggProsesserBehandling(
                    beriketBehandling.behandling.sakId,
                    beriketBehandling.behandling.id,
//                    listOf("trigger" to elementer.map { it.type.name }.toString())
                )
                låsRepository.verifiserSkrivelås(behandlingSkrivelås)
                return Fullført
                return FantVentebehov(
                    Ventebehov(
                        definisjon = Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT,
                        grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                        frist = revurderingsDato
                    )
                )
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