package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
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
        val fritaksvurdering = løsning.fritaksvurdering.toFritaksvurdering()

        val eksisterendeFritaksvurderinger = meldepliktRepository.hentHvisEksisterer(behandling.id)?.vurderinger.orEmpty()

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger =  (eksisterendeFritaksvurderinger plasser fritaksvurdering).komprimer().map { it.verdi }
        )

        return LøsningsResultat(
            begrunnelse = fritaksvurdering.begrunnelse,
            kreverToTrinn = fritaksvurdering.harFritak
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    private infix fun List<Fritaksvurdering>.plasser(nyVurdering: Fritaksvurdering): Tidslinje<Fritaksvurdering> {
        val sortedFritaksvurdering = this.sortedBy { it.opprettetTid } + nyVurdering
        return sortedFritaksvurdering.drop(1).fold(sortedFritaksvurdering.first().tidslinje()) { acc, fritaksvurdering ->
            acc.kombiner(fritaksvurdering.tidslinje(), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
    }

    private fun Fritaksvurdering.tidslinje(): Tidslinje<Fritaksvurdering> {
        return Tidslinje(
            listOf(Segment(Periode(fraDato, Tid.MAKS), this))
        )
    }
}
