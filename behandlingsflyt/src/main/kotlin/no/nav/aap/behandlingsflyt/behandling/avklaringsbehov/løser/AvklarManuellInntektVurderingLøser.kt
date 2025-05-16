package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Year

class AvklarManuellInntektVurderingLøser(
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
) : AvklaringsbehovsLøser<AvklarManuellInntektVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarManuellInntektVurderingLøsning
    ): LøsningsResultat {
        manuellInntektGrunnlagRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            manuellVurdering = ManuellInntektVurdering(
                begrunnelse = løsning.manuellVurderingForManglendeInntekt.begrunnelse,
                belop = løsning.manuellVurderingForManglendeInntekt.belop.let(::Beløp),
                vurdertAv = løsning.manuellVurderingForManglendeInntekt.vurdertAv,
                år = Year.of(
                    løsning.manuellVurderingForManglendeInntekt.år
                )
            )
        )
        return LøsningsResultat("Vurdert manuell inntekt i inntektsgrunnlag.")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_MANUELL_INNTEKT
    }
}