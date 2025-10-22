package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.math.BigDecimal

class AvklarManuellInntektVurderingLøser(
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val beregningService: BeregningService,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<AvklarManuellInntektVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        beregningService = BeregningService(repositoryProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarManuellInntektVurderingLøsning
    ): LøsningsResultat {
        val relevantePeriode = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId())
        val sisteRelevanteÅr = relevantePeriode.max()

        if (løsning.manuellVurderingForManglendeInntekt.vurderinger?.any { it.belop < BigDecimal.ZERO } == true) {
            throw UgyldigForespørselException("Inntekt kan ikke være negativ")
        }

        val vurderinger = if (unleashGateway.isEnabled(BehandlingsflytFeature.EOSBeregning)) {
            val begrunnelse = løsning.manuellVurderingForManglendeInntekt.begrunnelse
            løsning.manuellVurderingForManglendeInntekt.vurderinger?.map { vurdering ->
                ManuellInntektVurdering(
                    begrunnelse = begrunnelse,
                    belop = vurdering.belop.let(::Beløp),
                    vurdertAv = kontekst.bruker.ident,
                    år = vurdering.år
                )
            }?.toSet()!!
        } else {
            setOf(
                ManuellInntektVurdering(
                    begrunnelse = løsning.manuellVurderingForManglendeInntekt.begrunnelse,
                    belop = løsning.manuellVurderingForManglendeInntekt.belop.let(::Beløp),
                    vurdertAv = kontekst.bruker.ident,
                    år = sisteRelevanteÅr
                )
            )
        }

        manuellInntektGrunnlagRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            manuellVurderinger = vurderinger
        )
        return LøsningsResultat("Vurdert manuell inntekt i inntektsgrunnlag.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_MANUELL_INNTEKT
    }
}