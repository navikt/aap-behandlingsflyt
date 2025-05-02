package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class RefusjonkravLøser(val connection: DBConnection) : AvklaringsbehovsLøser<RefusjonkravLøsning> {
    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {
        val vurdering = validerRefusjonDato(kontekst, løsning)

        refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), vurdering)
        return LøsningsResultat("Vurdert refusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }

    private fun validerRefusjonDato(
        kontekst: AvklaringsbehovKontekst,
        løsning: RefusjonkravLøsning
    ): RefusjonkravVurdering {
        if (løsning.refusjonkravVurdering.harKrav) {
            val sak = sakRepository.hent(kontekst.kontekst.sakId)
            val kravDato = sak.rettighetsperiode.fom

            val refusjonFomDato = løsning.refusjonkravVurdering.fom ?: kravDato
            val refusjonTomDato = løsning.refusjonkravVurdering.tom

            if (refusjonFomDato.isBefore(kravDato)) {
                throw IllegalArgumentException("Refusjonsdato kan ikke være før kravdato. Refusjonsdato: $refusjonFomDato, kravdato: $kravDato")
            }

            if (refusjonTomDato != null && refusjonFomDato.isAfter(refusjonTomDato)) {
                throw IllegalArgumentException("Tom (${refusjonTomDato}) er før fom(${refusjonFomDato})")
            }
            return RefusjonkravVurdering(løsning.refusjonkravVurdering.harKrav, refusjonFomDato, refusjonTomDato)
        }
        return løsning.refusjonkravVurdering
    }
}
