package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering

data class BistandVurderingDto(
    val begrunnelse: String,
    val erBehovForBistand: Boolean,
    val grunnerTilBehovForBistand: List<BistandGrunn>?,
) {
    companion object {
       fun fraBistandVurdering(bistandVurdering: BistandVurdering?): BistandVurderingDto? {
           if(bistandVurdering == null) return null
           return BistandVurderingDto(
              begrunnelse = bistandVurdering.begrunnelse,
               erBehovForBistand = bistandVurdering.erBehovForBistand,
               grunnerTilBehovForBistand = listOfNotNull(
                   behovBooleanTilBistandGrunn(bistandVurdering.erBehovForAktivBehandling, BistandGrunn.AKTIV_BEHANDLING),
                   behovBooleanTilBistandGrunn(bistandVurdering.erBehovForArbeidsrettetTiltak, BistandGrunn.ARBEIDSRETTET_TILTAK),
                   behovBooleanTilBistandGrunn(bistandVurdering.erBehovForAnnenOppfølging, BistandGrunn.ANNEN_OPPFØLGING),
               )
           )
       }
        private fun behovBooleanTilBistandGrunn(erBehovBoolean: Boolean?, grunn: BistandGrunn): BistandGrunn? {
            return if (erBehovBoolean == true) grunn else null
        }
    }
    fun tilBistandVurdering(): BistandVurdering {
       return BistandVurdering(
           begrunnelse = begrunnelse,
           erBehovForBistand = erBehovForBistand,
           erBehovForAktivBehandling = grunnerTilBehovForBistand?.contains(BistandGrunn.AKTIV_BEHANDLING),
           erBehovForArbeidsrettetTiltak = grunnerTilBehovForBistand?.contains(BistandGrunn.ARBEIDSRETTET_TILTAK),
           erBehovForAnnenOppfølging = grunnerTilBehovForBistand?.contains(BistandGrunn.ANNEN_OPPFØLGING),
       )
    }
}
enum class BistandGrunn {
    ARBEIDSRETTET_TILTAK,
    AKTIV_BEHANDLING,
    ANNEN_OPPFØLGING
}
