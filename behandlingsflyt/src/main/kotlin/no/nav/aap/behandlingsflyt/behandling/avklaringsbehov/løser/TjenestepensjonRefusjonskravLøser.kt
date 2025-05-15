package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.TjenestepensjonRefusjonskravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class TjenestepensjonRefusjonskravLøser(
    private val sakRepository: SakRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
) : AvklaringsbehovsLøser<TjenestepensjonRefusjonskravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        sakRepository = repositoryProvider.provide(),
        tjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: TjenestepensjonRefusjonskravLøsning
    ): LøsningsResultat {
        val vurdering = validerRefusjonsDato(kontekst, løsning)

        tjenestepensjonRefusjonsKravVurderingRepository.lagre(
            kontekst.kontekst.sakId,
            kontekst.behandlingId(),
            vurdering
        )
        return LøsningsResultat("Vurdert SamordningRefusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_REFUSJONS_KRAV
    }

    private fun validerRefusjonsDato(
        kontekst: AvklaringsbehovKontekst,
        løsning: TjenestepensjonRefusjonskravLøsning
    ): TjenestepensjonRefusjonskravVurdering {
        if (løsning.samordningRefusjonskrav.harKrav) {
            val sak = sakRepository.hent(kontekst.kontekst.sakId)
            val kravDato = sak.rettighetsperiode.fom

            val refusjonFomDato = listOfNotNull(løsning.samordningRefusjonskrav.fom,kravDato).max()
            val refusjonTomDato = løsning.samordningRefusjonskrav.tom

            if (refusjonFomDato.isBefore(kravDato)) {
                throw IllegalArgumentException("Refusjonsdato kan ikke være før kravdato")
            }

            if (refusjonTomDato != null && refusjonTomDato.isBefore(refusjonFomDato)) {
                throw IllegalArgumentException("Refusjonsdato kan ikke være før refusjonsdato eller null")
            }

            return TjenestepensjonRefusjonskravVurdering(
                harKrav = løsning.samordningRefusjonskrav.harKrav,
                fom = refusjonFomDato,
                tom = refusjonTomDato,
                begrunnelse = løsning.samordningRefusjonskrav.begrunnelse
            )
        }
        return løsning.samordningRefusjonskrav

    }
}
