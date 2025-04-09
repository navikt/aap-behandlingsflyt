package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import java.time.Duration

class TjenestePensjonService(
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val sakService: SakService
) : Informasjonskrav {
    private val tpGateway = GatewayProvider.provide<TjenestePensjonGateway>()
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_TJENESTEPENSJON

        override fun erRelevant(kontekst: FlytKontekstMedPerioder, oppdatert: InformasjonskravOppdatert?): Boolean {
            return kontekst.erFørstegangsbehandlingRevurderingEllerForlengelse() && oppdatert.ikkeKjørtSiste(
                Duration.ofHours(
                    1
                )
            )
        }

        override fun konstruer(connection: DBConnection): Informasjonskrav {
            val repositoryProvider = RepositoryProvider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()

            return TjenestePensjonService(
                tjenestePensjonRepository = tjenestePensjonRepository,
                sakService = SakService(sakRepository)
            )

        }

    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val tjenestePensjon = hentTjenestePensjon(
            personIdent,
            sak.rettighetsperiode
        )

        log.info("hentet tjeneste pensjon for person i sak ${sak.saksnummer}. Antall: ${tjenestePensjon.tp.size}")

        val eksisterendeData = tjenestePensjonRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (harEndringerITjenestePensjon(eksisterendeData, tjenestePensjon)) {
            log.info("Oppdaterer tjeneste pensjon for behandling ${kontekst.behandlingId}. Tjeneste pensjon funnet: ${tjenestePensjon.tp}")
            tjenestePensjonRepository.lagre(kontekst.behandlingId, tjenestePensjon)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentTjenestePensjon(
        personIdent: String,
        rettigetsperiode: Periode
    ): TjenestePensjon {
        return tpGateway.hentTjenestePensjon(
                personIdent,
                rettigetsperiode

        )
    }

    private fun harEndringerITjenestePensjon(
        eksisterendeData: TjenestePensjon?,
        tjenestePensjon: TjenestePensjon
    ): Boolean {
        return eksisterendeData == null || eksisterendeData.tp != tjenestePensjon.tp
    }
}