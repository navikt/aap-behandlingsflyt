package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.Tid

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val eksisterendeFritaksvurderinger = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger =  eksisterendeFritaksvurderinger plasser løsning.fritaksvurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.fritaksvurdering.begrunnelse,
            kreverToTrinn = løsning.fritaksvurdering.harFritak
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    //spiller rekkefølge noe rolle?
    private infix fun List<Fritaksvurdering>.plasser(nyVurdering: Fritaksvurdering): List<Fritaksvurdering> {
        return this.map { eksisterendeVurdering -> eksisterendeVurdering lagPlassFor nyVurdering } + nyVurdering
    }

    //scope functions fordi smart cast er litt wonky med klasser fra en annen modul
    private infix fun Fritaksvurdering.lagPlassFor(nyVurdering: Fritaksvurdering): Fritaksvurdering {
        validateVurderingIsNew(nyVurdering)
        return nyVurdering.periode?.let {
            this.copy(periode = this.periode except it)
        } ?: throw IllegalStateException("Ny fritaksvurdering må ha en periode")
    }

    private fun validateVurderingIsNew(nyVurdering: Fritaksvurdering) {
        require(nyVurdering.periode?.tom == Tid.MAKS) {
            "ny vurdering uten periode/maksverdi for periode er endret/metode blir ikke brukt i riktig kontekst"
        }
    }

    private infix fun Periode?.except(åpenPeriode: Periode): Periode? {
        //redundant men for klarhets skyld
        require(åpenPeriode.tom == Tid.MAKS) {
            "åpenPeriode parameter er ikke åpen/maksverdi for periode er endret"
        }
        return this?.minus(åpenPeriode)?.ifEmpty { null }?.single()
    }
}
