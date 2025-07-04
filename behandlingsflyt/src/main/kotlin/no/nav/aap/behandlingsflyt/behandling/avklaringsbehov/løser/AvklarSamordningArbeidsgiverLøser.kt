package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningAndreStatligeYtelserLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningArbeidsgiverLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverVurdering
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
                 vurdering = løsning.samordningArbeidsgiverVurdering.vurdering,
                 fom = løsning.samordningArbeidsgiverVurdering.fom,
                 tom = løsning.samordningArbeidsgiverVurdering.tom,
                 opprettetTid = TODO(),
                 vurdertAv = kontekst.bruker.ident,
             )
        )
        return LøsningsResultat("Vurdert samordning andre statlige ytelser")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ARBEIDSGIVER
    }
}