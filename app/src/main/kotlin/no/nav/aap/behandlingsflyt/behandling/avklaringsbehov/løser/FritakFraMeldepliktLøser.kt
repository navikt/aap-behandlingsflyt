package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksvurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Tid

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val fritaksvurderinger = løsning.fritaksvurderinger.map(FritaksvurderingDto::toFritaksvurdering)

        val eksisterendeFritaksvurderinger = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger =  (eksisterendeFritaksvurderinger plasser fritaksvurderinger).komprimer().map { it.verdi.copy(fraDato = it.periode.fom) }
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert fritak meldeplikt",
            kreverToTrinn = fritaksvurderinger.minstEttFritak()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    //eller this.sorted.tidslinje.kombiner(nye.sorted.tidslinje)
    private infix fun List<Fritaksvurdering>.plasser(nyeVurderinger: List<Fritaksvurdering>): Tidslinje<Fritaksvurdering> {
        return (this.sortedBy { it.fraDato } + nyeVurderinger.sortedBy { it.fraDato }).tidslinje()
    }

    private fun List<Fritaksvurdering>.tidslinje(): Tidslinje<Fritaksvurdering> {
        return this.drop(1).fold(this.first().tidslinje()) { acc, fritaksvurdering ->
            acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    }

    private fun Fritaksvurdering.tidslinje(): Tidslinje<Fritaksvurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }

    private fun List<Fritaksvurdering>.minstEttFritak(): Boolean {
        return this.any { it.harFritak }
    }
}
