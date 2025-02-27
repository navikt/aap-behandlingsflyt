package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.behandling.underveis.UnderveisService
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class UnderveisSteg(private val underveisService: UnderveisService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(UnderveisSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val underveisTidslinje = underveisService.vurder(kontekst.behandlingId)

        log.debug("Underveis tidslinje {}", underveisTidslinje)

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            val repositoryProvider = RepositoryProvider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val vilkårsresultatRepository =
                repositoryProvider.provide<VilkårsresultatRepository>()
            val behandlingService =
                SakOgBehandlingService(
                    GrunnlagKopierer(connection),
                    sakRepository,
                    behandlingRepository
                )
            val aktivitetspliktRepository =
                repositoryProvider.provide<AktivitetspliktRepository>()
            val plikortkortRepository = repositoryProvider.provide<MeldekortRepository>()
            val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
            val barnetilleggRepository = repositoryProvider.provide<BarnetilleggRepository>()

            return UnderveisSteg(
                UnderveisService(
                    behandlingService = behandlingService,
                    vilkårsresultatRepository = vilkårsresultatRepository,
                    meldekortRepository = plikortkortRepository,
                    underveisRepository = underveisRepository,
                    aktivitetspliktRepository = aktivitetspliktRepository,
                    etAnnetStedUtlederService = EtAnnetStedUtlederService(
                        barnetilleggRepository,
                        repositoryProvider.provide(),
                        sakRepository,
                        behandlingRepository
                    ),
                    arbeidsevneRepository = repositoryProvider.provide<ArbeidsevneRepository>(),
                    meldepliktRepository = repositoryProvider.provide(),
                )
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_UTTAK
        }
    }
}