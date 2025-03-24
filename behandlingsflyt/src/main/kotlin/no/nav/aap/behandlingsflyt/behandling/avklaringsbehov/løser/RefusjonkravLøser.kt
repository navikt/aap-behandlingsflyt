package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.RefusjonkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class RefusjonkravLøser(val connection: DBConnection) : AvklaringsbehovsLøser<RefusjonkravLøsning> {
    private val repositoryProvider = RepositoryProvider(connection)
    private val refusjonkravRepository = repositoryProvider.provide<RefusjonkravRepository>()
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning): LøsningsResultat {
        validerRefusjonDato(kontekst, løsning)

        refusjonkravRepository.lagre(kontekst.kontekst.sakId, kontekst.behandlingId(), løsning.refusjonkravVurdering)
        return LøsningsResultat("Vurdert refusjonskrav")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.REFUSJON_KRAV
    }

    private fun validerRefusjonDato (kontekst: AvklaringsbehovKontekst, løsning: RefusjonkravLøsning) {
        if (løsning.refusjonkravVurdering.harKrav) {
            val refusjonFomDato = løsning.refusjonkravVurdering.fom
            val refusjonTomDato = løsning.refusjonkravVurdering.tom
            requireNotNull(refusjonFomDato) {"Krever fra-dato når refusjonskrav er satt"}

            val behandling = behandlingRepository.hent(kontekst.behandlingId())
            val kravDato = behandling.opprettetTidspunkt.toLocalDate()

            if (refusjonFomDato.isBefore(kravDato)) {
                throw IllegalArgumentException("Refusjonsdato kan ikke være før kravdato")
            }

            if (refusjonTomDato != null && refusjonFomDato.isAfter(refusjonTomDato)) {
                throw IllegalArgumentException("Tom (${refusjonTomDato}) er før fom(${refusjonFomDato})")
            }
        }
    }
}
