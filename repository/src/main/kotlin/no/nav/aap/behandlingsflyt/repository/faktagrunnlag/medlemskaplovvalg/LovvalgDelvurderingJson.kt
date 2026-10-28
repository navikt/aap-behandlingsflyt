package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.medlemskaplovvalg

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.KanBehandlesAutomatiskVurdering
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.TilhørighetVurdering
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode

internal object LovvalgDelvurderingJson {

    const val versjon = 1

    fun serialiser(vurdering: KanBehandlesAutomatiskVurdering): String =
        DefaultJsonMapper.toJson(VurderingV1(vurdering.kanBehandlesAutomatisk, vurdering.tilhørighetVurdering))

    fun deserialiser(versjon: Int, json: String): KanBehandlesAutomatiskVurdering {
        require(versjon == 1) { "Ukjent serialiseringsversjon for lovvalgtilhørighet: $versjon" }
        val vurdering = DefaultJsonMapper.fromJson<VurderingV1>(json)
        return KanBehandlesAutomatiskVurdering(vurdering.kanBehandlesAutomatisk, vurdering.tilhørighetVurdering)
    }

    // Endringer i TilhørighetVurdering må være bakoverkompatible med V1, eller få en ny serialiseringsversjon.
    private data class VurderingV1(
        val kanBehandlesAutomatisk: Boolean,
        val tilhørighetVurdering: List<TilhørighetVurdering>,
    )
}
