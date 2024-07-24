package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDate
import java.util.*

data class VilkårsResultatDTO(
    val typeBehandling: TypeBehandling,
    val vilkår: List<VilkårDTO>,
) {
    companion object {
        fun fraDomeneObjekt(
            typeBehandling: TypeBehandling,
            vilkårsresultat: Vilkårsresultat,
        ): VilkårsResultatDTO {
            return VilkårsResultatDTO(
                typeBehandling,
                vilkårsresultat.alle().map { VilkårDTO.fraDomeneObjekt(it) }
            )
        }
    }
}

data class VilkårDTO(val vilkårType: Vilkårtype, val perioder: List<VilkårsPeriodeDTO>) {
    companion object {
        fun fraDomeneObjekt(vilkår: Vilkår): VilkårDTO {
            return VilkårDTO(
                vilkårType = vilkår.type,
                perioder = vilkår.vilkårsperioder().map { VilkårsPeriodeDTO.fraDomeneObjekt(it) })
        }
    }
}

data class VilkårsPeriodeDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val innvilgelsesårsak: Innvilgelsesårsak?,
    val avslagsårsak: Avslagsårsak?
) {
    companion object {
        fun fraDomeneObjekt(vilkårsperiode: Vilkårsperiode): VilkårsPeriodeDTO {
            return VilkårsPeriodeDTO(
                fraDato = vilkårsperiode.periode.fom,
                tilDato = vilkårsperiode.periode.tom,
                utfall = vilkårsperiode.utfall,
                manuellVurdering = vilkårsperiode.manuellVurdering,
                innvilgelsesårsak = vilkårsperiode.innvilgelsesårsak,
                avslagsårsak = vilkårsperiode.avslagsårsak
            )
        }
    }
}

data class TilkjentYtelseDTO(val perioder: List<TilkjentYtelsePeriodeDTO>)

data class TilkjentYtelsePeriodeDTO(
    val dagsats: Number,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val gradering: Number
)

data class AvsluttetBehandlingDTO(
    val behandlingsReferanse: UUID,
    val saksnummer: Saksnummer,
    val vilkårsResultat: VilkårsResultatDTO,
    val tilkjentYtelse: TilkjentYtelseDTO
)