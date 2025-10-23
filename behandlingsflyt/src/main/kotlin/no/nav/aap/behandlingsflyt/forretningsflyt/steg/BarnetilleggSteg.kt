package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class BarnetilleggSteg(
    private val barnetilleggService: BarnetilleggService,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val barnRepository: BarnRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val avklaringsbehovService: AvklaringsbehovService
) : BehandlingSteg {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        barnetilleggService = BarnetilleggService(repositoryProvider, gatewayProvider),
        barnetilleggRepository = repositoryProvider.provide(),
        barnRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider),
        avklaringsbehovService = AvklaringsbehovService(repositoryProvider)
    )

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        avklaringsbehovService.oppdaterAvklaringsbehov(
            avklaringsbehovene = avklaringsbehovene,
            definisjon = Definisjon.AVKLAR_BARNETILLEGG,
            vedtakBehøverVurdering = { vedtakBehøverVurdering(kontekst) },
            erTilstrekkeligVurdert = { !harPerioderMedBarnTilAvklaring(kontekst) },
            tilbakestillGrunnlag = { tilbakestillBarnetillegg(kontekst)},
            kontekst
        )
        return Fullført
    }

    fun vedtakBehøverVurdering(kontekst: FlytKontekstMedPerioder): Boolean {
        val barneGrunnlag = barnRepository.hentHvisEksisterer(kontekst.behandlingId)
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING ->
                tidligereVurderinger.muligMedRettTilAAP(kontekst, type())
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty()
                        && harOppgittBarnEllerFolkeregistrerteBarn(barneGrunnlag)

            VurderingType.REVURDERING ->
                (!tidligereVurderinger.girIngenBehandlingsgrunnlag(kontekst, type())
                        || (Vurderingsbehov.DØDSFALL_BARN in kontekst.vurderingsbehovRelevanteForSteg)
                        && kontekst.vurderingsbehovRelevanteForSteg.isNotEmpty())
                        && harOppgittBarnEllerFolkeregistrerteBarn(barneGrunnlag)

            VurderingType.MELDEKORT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT,
            VurderingType.EFFEKTUER_AKTIVITETSPLIKT_11_9,
            VurderingType.IKKE_RELEVANT ->
                false
        }
    }

    private fun tilbakestillBarnetillegg(kontekst: FlytKontekstMedPerioder) {
        val vedtatteBarnetillegg = kontekst.forrigeBehandlingId
            ?.let { barnetilleggRepository.hentHvisEksisterer(it) }
            ?.perioder
            ?: emptyList()
        barnetilleggRepository.lagre(kontekst.behandlingId, vedtatteBarnetillegg)

        val forrigeBarnGrunnlag = kontekst.forrigeBehandlingId
            ?.let { barnRepository.hentHvisEksisterer(it) }

        val vurderteBarn = forrigeBarnGrunnlag?.vurderteBarn?.barn.orEmpty()

        barnRepository.lagreVurderinger(
            behandlingId = kontekst.behandlingId,
            vurdertAv = forrigeBarnGrunnlag?.vurderteBarn?.vurdertAv ?: SYSTEMBRUKER.ident,
            vurderteBarn = vurderteBarn
        )
    }

    private fun harOppgittBarnEllerFolkeregistrerteBarn(grunnlag: BarnGrunnlag?) =
        grunnlag?.oppgitteBarn != null || grunnlag?.registerbarn?.barn?.isNotEmpty() == true

    private fun harPerioderMedBarnTilAvklaring(kontekst: FlytKontekstMedPerioder): Boolean {
        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        barnetilleggRepository.lagre(
            kontekst.behandlingId,
            barnetillegg.segmenter()
                .map {
                    BarnetilleggPeriode(
                        it.periode,
                        it.verdi.barnMedRettTil()
                    )
                }
        )

        val finnesBarnTilAvklaring = barnetillegg.segmenter().any { it.verdi.harBarnTilAvklaring() }
        if (finnesBarnTilAvklaring) {
            val perioderTilAvklaring = barnetillegg.segmenter().filter { it.verdi.harBarnTilAvklaring() }
            log.info("Det finnes perioder med barn som ikke har blitt avklart. Antall: ${perioderTilAvklaring.size}")
        }

        return finnesBarnTilAvklaring
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return BarnetilleggSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}