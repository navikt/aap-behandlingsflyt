package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarManuellInntektVurderingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.RepositoryProvider
import java.math.BigDecimal
import java.time.Year

class AvklarManuellInntektVurderingLøser(
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository
) : AvklaringsbehovsLøser<AvklarManuellInntektVurderingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        manuellInntektGrunnlagRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarManuellInntektVurderingLøsning
    ): LøsningsResultat {
        if (løsning.manuellVurderingForManglendeInntekt.vurderinger.any { it.beløp != null && it.beløp < BigDecimal.ZERO }
            || løsning.manuellVurderingForManglendeInntekt.vurderinger.any { it.eøsBeløp != null && it.eøsBeløp < BigDecimal.ZERO }
        ) {
            throw UgyldigForespørselException("Inntekt kan ikke være negativ")
        }

        val vurderinger = løsning.manuellVurderingForManglendeInntekt.vurderinger.map { vurdering ->
            ManuellInntektVurdering(
                begrunnelse = løsning.manuellVurderingForManglendeInntekt.begrunnelse,
                belop = vurdering.beløp?.let { Beløp(it) },
                vurdertAv = kontekst.bruker,
                år = Year.of(vurdering.år),
                eøsBeløp = vurdering.eøsBeløp?.let { Beløp(it) },
                ferdigLignetPGI = vurdering.ferdigLignetPGI?.let { Beløp(it) },
                månedsPeriode = vurdering.periode,
            )
        }.toSet()


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