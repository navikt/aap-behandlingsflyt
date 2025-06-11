package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EffektuerAvvistPåFormkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.effektueravvistpåformkrav.EffektuerAvvistPåFormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class EffektuerAvvistPåFormkravLøser(
    private val formkravRepository: FormkravRepository,
    private val effektuerAvvistPåFormkravRepository: EffektuerAvvistPåFormkravRepository,
) : AvklaringsbehovsLøser<EffektuerAvvistPåFormkravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        repositoryProvider.provide(),
        repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: EffektuerAvvistPåFormkravLøsning): LøsningsResultat {
        val formkravVurdering = formkravRepository.hentHvisEksisterer(kontekst.behandlingId())
            ?: throw InternfeilException("Formkravvurdering ikke funnet for behandling")

        if (!erKonsistent(formkravVurdering.vurdering, løsning)) {
            throw UgyldigForespørselException(
                "Løsningen er ikke konsistent med formkravvurderingen"
            )
        }
        if (avvisesFørFrist(
                kontekst.kontekst.behandlingId,
                effektuerAvvistPåFormkravRepository,
                løsning
            )
        ) {
                throw UgyldigForespørselException(
                "Kan ikke avvise på formkrav før fristen er utløpt"
            )
        }

        effektuerAvvistPåFormkravRepository.lagreVurdering(
            kontekst.kontekst.behandlingId,
            løsning.effektuerAvvistPåFormkravVurdering.tilVurdering()
        )

        return LøsningsResultat(
            begrunnelse = "Effektuer avvist på formkrav"
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.EFFEKTUER_AVVIST_PÅ_FORMKRAV
    }

    private fun erKonsistent(
        formkravVurdering: FormkravVurdering,
        løsning: EffektuerAvvistPåFormkravLøsning
    ): Boolean {
        return !formkravVurdering.erOppfylt() == løsning.effektuerAvvistPåFormkravVurdering.skalEndeligAvvises
    }

    private fun avvisesFørFrist(
        behandlingId: BehandlingId,
        effektuerAvvistPåFormkravRepository: EffektuerAvvistPåFormkravRepository,
        løsning: EffektuerAvvistPåFormkravLøsning
    ): Boolean {
        val grunnlag = effektuerAvvistPåFormkravRepository.hentHvisEksisterer(behandlingId)
            ?: throw InternfeilException("Fant ikke forhåndsvarsel for behandling")

        return løsning.effektuerAvvistPåFormkravVurdering.skalEndeligAvvises && LocalDate.now() < grunnlag.varsel.frist
    }
}