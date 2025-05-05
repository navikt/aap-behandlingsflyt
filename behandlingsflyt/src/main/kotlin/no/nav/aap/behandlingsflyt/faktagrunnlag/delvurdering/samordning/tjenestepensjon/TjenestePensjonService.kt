package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.ikkeKjørtSiste
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryRegistry
import org.slf4j.LoggerFactory
import java.time.Duration

class TjenestePensjonService(
    private val tjenestePensjonRepository: TjenestePensjonRepository,
    private val sakService: SakService,
    private val tidligereVurderinger: TidligereVurderinger,
) : Informasjonskrav {
    private val tpGateway = GatewayProvider.provide<TjenestePensjonGateway>()
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.SAMORDNING_TJENESTEPENSJON

        override fun konstruer(connection: DBConnection): Informasjonskrav {
            val repositoryProvider = RepositoryRegistry.provider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val tjenestePensjonRepository = repositoryProvider.provide<TjenestePensjonRepository>()

            return TjenestePensjonService(
                tjenestePensjonRepository = tjenestePensjonRepository,
                sakService = SakService(sakRepository),
                tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(kontekst: FlytKontekstMedPerioder, steg: StegType, oppdatert: InformasjonskravOppdatert?): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                oppdatert.ikkeKjørtSiste(Duration.ofHours(1)) &&
                tidligereVurderinger.harBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        val sak = sakService.hent(kontekst.sakId)
        val personIdent = sak.person.aktivIdent().identifikator

        val tjenestePensjon = hentTjenestePensjon(
            personIdent,
            sak.rettighetsperiode
        )

        log.info("hentet tjeneste pensjon for person i sak ${sak.saksnummer}. Antall: ${tjenestePensjon.size}")

        val eksisterendeData = tjenestePensjonRepository.hentHvisEksisterer(kontekst.behandlingId)

        if (harEndringerITjenestePensjon(eksisterendeData, tjenestePensjon)) {
            log.info("Oppdaterer tjeneste pensjon for behandling ${kontekst.behandlingId}. Tjeneste pensjon funnet: ${tjenestePensjon.size}")
            tjenestePensjonRepository.lagre(kontekst.behandlingId, tjenestePensjon)
            return Informasjonskrav.Endret.ENDRET
        }

        return Informasjonskrav.Endret.IKKE_ENDRET
    }

    private fun hentTjenestePensjon(
        personIdent: String,
        rettigetsperiode: Periode
    ): List<TjenestePensjonForhold> {
        return tpGateway.hentTjenestePensjon(
                personIdent,
                rettigetsperiode
        )
    }

    private fun harEndringerITjenestePensjon(
        eksisterendeData: List<TjenestePensjonForhold>?,
        tjenestePensjon: List<TjenestePensjonForhold>
    ): Boolean {
        return  eksisterendeData.isNullOrEmpty() || eksisterendeData != tjenestePensjon
    }
}