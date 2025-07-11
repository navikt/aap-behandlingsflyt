package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningAndreStatligeYtelserLøser(
    private val samordningAndreStatligeYtelserRepository: SamordningAndreStatligeYtelserRepository,
) : AvklaringsbehovsLøser<AvklarSamordningAndreStatligeYtelserLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningAndreStatligeYtelserRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningAndreStatligeYtelserLøsning
    ): LøsningsResultat {
        samordningAndreStatligeYtelserRepository.lagre(
            kontekst.behandlingId(),
            SamordningAndreStatligeYtelserVurdering(
                begrunnelse = løsning.samordningAndreStatligeYtelserVurdering.begrunnelse,
                vurdertAv = kontekst.bruker.ident,
                vurderingPerioder = løsning.samordningAndreStatligeYtelserVurdering.vurderingPerioder.map {
                    SamordningAndreStatligeYtelserVurderingPeriode(
                        ytelse = it.ytelse,
                        periode = it.periode,
                    )
                }
            )
        )
        return LøsningsResultat("Vurdert samordning andre statlige ytelser")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ANDRE_STATLIGE_YTELSER
    }
}