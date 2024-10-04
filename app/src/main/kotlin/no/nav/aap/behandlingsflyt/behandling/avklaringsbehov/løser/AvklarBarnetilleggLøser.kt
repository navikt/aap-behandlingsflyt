package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvar
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarn.ForeldreansvarVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.Ident

class AvklarBarnetilleggLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBarnetilleggLøsning> {

    private val barnRepository = BarnRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBarnetilleggLøsning): LøsningsResultat {

        val vurderteBarn = barnRepository.hentHvisEksisterer(kontekst.kontekst.behandlingId)?.vurderteBarn?.barn
            ?: emptyList<VurdertBarn>()
        val oppdatertTilstand = oppdaterTilstandBasertPåNyeVurderinger(vurderteBarn, løsning)

        barnRepository.lagreVurderinger(kontekst.kontekst.behandlingId, oppdatertTilstand)

        return LøsningsResultat(begrunnelse = "Vurdert barnetillegg")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BARNETILLEGG
    }
}

internal fun oppdaterTilstandBasertPåNyeVurderinger(
    vurderteBarn: List<VurdertBarn>,
    løsning: AvklarBarnetilleggLøsning
): List<VurdertBarn> {
    val tidslinjePerBarn = HashMap<String, Tidslinje<ForeldreansvarVurdering>>()
    vurderteBarn.forEach { barn -> tidslinjePerBarn[barn.ident.identifikator] = barn.tilTidslinje() }

    val nyeVurderinger = løsning.vurderingerForBarnetillegg.vurderteBarn

    nyeVurderinger.forEach { nyVurdering ->
        val oppdatertTidslinje = tidslinjePerBarn[nyVurdering.ident.identifikator]?.kombiner(
            nyVurdering.tilTidslinje(),
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ) ?: Tidslinje()

        tidslinjePerBarn[nyVurdering.ident.identifikator] = oppdatertTidslinje.komprimer()
    }

    val oppdatertTilstand = tidslinjePerBarn.map {
        VurdertBarn(
            ident = Ident(it.key),
            vurderinger = it.value.segmenter().map {
                VurderingAvForeldreAnsvar(
                    it.periode.fom,
                    it.verdi.harForeldreAnsvar,
                    it.verdi.begrunnelse
                )
            })
    }
    return oppdatertTilstand
}
