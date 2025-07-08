package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningArbeidsgiverLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningArbeidsgiverLøser(
    private val samordningArbeidsgiverRepository: SamordningArbeidsgiverRepository,
) : AvklaringsbehovsLøser<AvklarSamordningArbeidsgiverLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        samordningArbeidsgiverRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningArbeidsgiverLøsning
    ): LøsningsResultat {
         samordningArbeidsgiverRepository.lagre(

             kontekst.kontekst.sakId,
             kontekst.behandlingId(),
             SamordningArbeidsgiverVurdering(
                 begrunnelse = løsning.samordningArbeidsgiverVurdering.vurdering,
                 fom = løsning.samordningArbeidsgiverVurdering.fom,
                 tom = løsning.samordningArbeidsgiverVurdering.tom,
                 vurdertAv = kontekst.bruker.ident,
             )
        )
        return LøsningsResultat("Vurdert samordning arbeidsgiver")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ARBEIDSGIVER
    }
}