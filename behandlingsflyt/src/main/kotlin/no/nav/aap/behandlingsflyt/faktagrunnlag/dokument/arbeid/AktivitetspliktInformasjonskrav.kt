package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav.Endret.IKKE_ENDRET
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottaDokumentService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class AktivitetspliktInformasjonskrav(
    private val mottaDokumentService: MottaDokumentService,
    private val aktivitetspliktRepository: AktivitetspliktRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val prosessert11_7VurderingRepository: Prosessert11_7VurderingRepository,
    private val behandlingRepository: BehandlingRepository,
) : Informasjonskrav {
    private val log = LoggerFactory.getLogger(AktivitetspliktInformasjonskrav::class.java)

    companion object : Informasjonskravkonstruktør {
        override val navn = InformasjonskravNavn.AKTIVITETSPLIKT

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): AktivitetspliktInformasjonskrav {
            val mottattDokumentRepository =
                repositoryProvider.provide<MottattDokumentRepository>()

            return AktivitetspliktInformasjonskrav(
                MottaDokumentService(mottattDokumentRepository),
                repositoryProvider.provide(),
                TidligereVurderingerImpl(repositoryProvider),
                repositoryProvider.provide(),
                repositoryProvider.provide()
            )
        }
    }

    override val navn = Companion.navn

    override fun erRelevant(
        kontekst: FlytKontekstMedPerioder,
        steg: StegType,
        oppdatert: InformasjonskravOppdatert?
    ): Boolean {
        return kontekst.erFørstegangsbehandlingEllerRevurdering() &&
                !tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, steg)
    }

    override fun oppdater(kontekst: FlytKontekstMedPerioder): Informasjonskrav.Endret {
        if (kontekst.forrigeBehandlingId == null) {
            // Skal ikke skje da det ikke er aktvitetsplikt før vedtatt førstegangsbehandling?
            return IKKE_ENDRET
        }

        if (kontekst.vurderingType == VurderingType.AKTIVITETSPLIKT) {
            // Her skal i ikke ha tilbakeføring, men lagre ned iverksatte behandlinger som finnes i nåværende transaksjon
            //hent iverksatte aktivitetsbehandlinger
            val nyesteIverksatteAktivitetspliktBehandling =
                behandlingRepository
                    .hentAlleFor(kontekst.sakId, listOf(TypeBehandling.Aktivitetsplikt))
                    .filter { it.status().erAvsluttet() }
                    .maxByOrNull { it.opprettetTidspunkt }
            requireNotNull(nyesteIverksatteAktivitetspliktBehandling) {
                "Fant ingen iverksatte aktivitetspliktbehandlinger for sak ${kontekst.sakId}, men vurderingstype er ${VurderingType.AKTIVITETSPLIKT}"
            }

            prosessert11_7VurderingRepository.lagre(kontekst.behandlingId, nyesteIverksatteAktivitetspliktBehandling.id)
            return IKKE_ENDRET
        }

        // Kan ha et grunnlag som peker på alle vurderinger i aktivitetsbehandlingen, 
        // og kopiere dette over ved "fletting". Fordi aktivitetspliktgrunnlaget alltid går via fasttrack, bør dette fungere.
        // Må da sjekke er "nåværende grunnlag" ulikt "forrige grunnlag" og oppdatere med forrige da det i praksis er nyere


        val aktivitetspliktBehandlingProsessertIForrige =
            prosessert11_7VurderingRepository.nyesteProsesserteAktivitetspliktBehandling(kontekst.forrigeBehandlingId)
        val aktivitetspliktBehandlingProsessertIDenne =
            prosessert11_7VurderingRepository.nyesteProsesserteAktivitetspliktBehandling(kontekst.behandlingId)
        if (aktivitetspliktBehandlingProsessertIForrige != null && aktivitetspliktBehandlingProsessertIForrige != aktivitetspliktBehandlingProsessertIDenne) {
            prosessert11_7VurderingRepository.lagre(kontekst.behandlingId, aktivitetspliktBehandlingProsessertIForrige)
            return ENDRET
        } else {
            return IKKE_ENDRET
        }

//
//        val aktivitetskortSomIkkeErBehandlet = mottaDokumentService.aktivitetskortSomIkkeErBehandlet(kontekst.sakId)
//        if (aktivitetskortSomIkkeErBehandlet.isEmpty()) {
//            return IKKE_ENDRET
//        }
//
//        val eksisterendeBrudd = aktivitetspliktRepository.hentGrunnlagHvisEksisterer(kontekst.behandlingId)
//            ?.bruddene
//            .orEmpty()
//
//        val alleBrudd = HashSet<AktivitetspliktDokument>(eksisterendeBrudd)
//
//        for (ubehandletInnsendingId in aktivitetskortSomIkkeErBehandlet) {
//            val nyeBrudd = aktivitetspliktRepository.hentBruddForInnsending(ubehandletInnsendingId)
//            alleBrudd.addAll(nyeBrudd)
//            mottaDokumentService.markerSomBehandlet(
//                sakId = kontekst.sakId,
//                behandlingId = kontekst.behandlingId,
//                referanse = InnsendingReferanse(ubehandletInnsendingId),
//            )
//        }
//
//        aktivitetspliktRepository.nyttGrunnlag(behandlingId = kontekst.behandlingId, brudd = alleBrudd)
//        return ENDRET
    }
}