package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class Effektuer11_7Steg private constructor(
    private val underveisRepository: UnderveisRepository
): BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val underveisGrunnlag = underveisRepository.hent(kontekst.behandlingId)
        if (underveisGrunnlag.perioder.any { it.avslagsårsak == UnderveisÅrsak.BRUDD_PÅ_AKTIVITETSPLIKT }) {
            return Fullført
            //return FantAvklaringsbehov(Definisjon.EFFEKTUER_11_7)
        }
        return Fullført
    }

    companion object: FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return Effektuer11_7Steg(
                //TODO - bruk repository provider?
                UnderveisRepository(connection)
            )
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_11_7
        }
    }
}