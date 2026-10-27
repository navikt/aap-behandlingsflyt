package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderingerImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingValidator
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningUføreSteg
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.periodisering.FlytKontekstMedPeriodeService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningUføreLøser(
    private val samordningUføreRepository: SamordningUføreRepository,
    private val uføreRepository: UføreRepository,
    private val tidligereVurderinger: TidligereVurderinger,
    private val flytKontekstMedPeriodeService: FlytKontekstMedPeriodeService,
) : AvklaringsbehovsLøser<AvklarSamordningUføreLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        samordningUføreRepository = repositoryProvider.provide(),
        uføreRepository = repositoryProvider.provide(),
        tidligereVurderinger = TidligereVurderingerImpl(repositoryProvider, gatewayProvider),
        flytKontekstMedPeriodeService = FlytKontekstMedPeriodeService(repositoryProvider, gatewayProvider),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningUføreLøsning
    ): LøsningsResultat {
        val vurderingPerioder = løsning.samordningUføreVurdering.vurderingPerioder.map {
            SamordningUføreVurderingPeriode(
                virkningstidspunkt = it.virkningstidspunkt,
                uføregradTilSamordning = Prosent(it.uføregradTilSamordning)
            )
        }
        validerAllePerioderErVurdert(kontekst, vurderingPerioder)
        validerIkkeDupliserteVurderinger(løsning)
        samordningUføreRepository.lagre(
            kontekst.behandlingId(),
            SamordningUføreVurdering(
                begrunnelse = løsning.samordningUføreVurdering.begrunnelse,
                vurderingPerioder = vurderingPerioder,
                vurdertAv = kontekst.bruker
            )
        )
        return LøsningsResultat("Vurdert samordning uføre")
    }

    private fun validerIkkeDupliserteVurderinger(løsning: AvklarSamordningUføreLøsning) {
        val duplikatLøsningPåSammeVirkningstidspunkt = løsning.samordningUføreVurdering.vurderingPerioder
            .groupingBy { it.virkningstidspunkt }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplikatLøsningPåSammeVirkningstidspunkt.isNotEmpty()) {
            throw UgyldigForespørselException("Det finnes duplikate vurderinger på samme virkningstidspunkt ${duplikatLøsningPåSammeVirkningstidspunkt.first()}. Dette er ikke tillatt - vennligst fjern en av radene.")
        }
    }

    private fun validerAllePerioderErVurdert(
        kontekst: AvklaringsbehovKontekst,
        vurderinger: List<SamordningUføreVurderingPeriode>
    ) {
        val stegType = SamordningUføreSteg.type()
        val flytKontekst = flytKontekstMedPeriodeService.utled(kontekst.kontekst, stegType)
        val manglendeVurderinger = SamordningUføreVurderingValidator.manglendeVurderinger(
            behandlingsutfall = tidligereVurderinger.behandlingsutfall(flytKontekst, stegType),
            uførevedtak = uføreRepository.hentHvisEksisterer(kontekst.behandlingId())?.vedtak.orEmpty(),
            vurderinger = vurderinger,
        )
        if (manglendeVurderinger.isNotEmpty()) {
            throw UgyldigForespørselException(message = "Har ikke vurdert alle perioder for samordning med delvis uføre.")
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_UFØRE
    }
}