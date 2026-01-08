package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningSykestipendLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningSykestipendLøser(
    private val sykestipendRepository: SykestipendRepository
) : AvklaringsbehovsLøser<AvklarSamordningSykestipendLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this(
        sykestipendRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningSykestipendLøsning
    ): LøsningsResultat {
        return when (val validertLøsning = løsning.valider()) {
            is Validation.Invalid -> throw UgyldigForespørselException(validertLøsning.errorMessage)
            is Validation.Valid -> {
                sykestipendRepository.lagre(
                    kontekst.behandlingId(),
                    løsning.sykestipendVurdering.tilVurdering(
                        bruker = kontekst.bruker,
                        vurdertIBehandling = kontekst.behandlingId()
                    )
                )
                LøsningsResultat(
                    begrunnelse = løsning.sykestipendVurdering.begrunnelse,
                )
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAMORDNING_SYKESTIPEND
    }

    private fun AvklarSamordningSykestipendLøsning.valider(): Validation<AvklarSamordningSykestipendLøsning> {
        return if (Periode.overlapper(sykestipendVurdering.perioder)) {
            Validation.Invalid(this, "Fant overlappende perioder")

        } else {
            Validation.Valid(this)
        }
    }
}