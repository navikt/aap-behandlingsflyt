package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningArbeidsgiverLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurderingDTO
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurderingerDTO
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.type.Periode
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

        if ( sjekkOmPerioderOverlapper(løsning.samordningArbeidsgiverVurdering.perioder) ) {
            throw UgyldigForespørselException("Perioder for samordning arbeidsgiver vurdering overlapper hverandre.")
        }

        samordningArbeidsgiverRepository.lagre(
                kontekst.kontekst.sakId,
                kontekst.behandlingId(),
                SamordningArbeidsgiverVurdering(
                    begrunnelse = løsning.samordningArbeidsgiverVurdering.begrunnelse,
                    perioder =  løsning.samordningArbeidsgiverVurdering.perioder,
                    vurdertAv = kontekst.bruker.ident,
                )
            )

        return LøsningsResultat("Vurdert samordning arbeidsgiver")

    }

    private fun sjekkOmPerioderOverlapper(perioder: List<Periode>): Boolean {
        if (perioder.size < 2) return false
        val sortert = perioder.sortedBy { it.fom }
        for (i in 1 until sortert.size) {
            if (sortert[i].overlapper(sortert[i - 1])) {
                return true
            }
        }
        return false
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_ARBEIDSGIVER
    }
}