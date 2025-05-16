package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarManuellInntektVurderingLøser(
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val beregningService: BeregningService
) : AvklaringsbehovsLøser<AvklarManuellInntektVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        beregningService = BeregningService(repositoryProvider)
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarManuellInntektVurderingLøsning
    ): LøsningsResultat {
        val relevanteÅr = beregningService.utledRelevanteBeregningsÅr(kontekst.behandlingId())
        val sisteRelevanteÅr = relevanteÅr.max()

        manuellInntektGrunnlagRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            manuellVurdering = ManuellInntektVurdering(
                begrunnelse = løsning.manuellVurderingForManglendeInntekt.begrunnelse,
                belop = løsning.manuellVurderingForManglendeInntekt.belop.let(::Beløp),
                vurdertAv = kontekst.bruker.ident,
                år = sisteRelevanteÅr
            )
        )
        return LøsningsResultat("Vurdert manuell inntekt i inntektsgrunnlag.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_MANUELL_INNTEKT
    }
}