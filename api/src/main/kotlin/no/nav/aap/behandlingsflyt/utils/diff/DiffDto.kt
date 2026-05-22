package no.nav.aap.behandlingsflyt.utils.diff

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret as DomeneEndret
import no.nav.aap.behandlingsflyt.utils.Fjernet as DomeneFjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil as DomeneLagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret as DomeneUendret

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "diff",
)
sealed interface DiffDto<T>

data class LagtTil<T>(val lagtTil: T) : DiffDto<T>

data class Fjernet<T>(val fjernet: T) : DiffDto<T>

data class Uendret<T>(val uendret: T) : DiffDto<T>

data class Endret<T>(val fra: T, val til: T) : DiffDto<T>

fun <T> Diff<T>.somDto(): DiffDto<T> = when (this) {
    is DomeneEndret<T> -> Endret(fra = fra, til = til)
    is DomeneFjernet<T> -> Fjernet(fjernet)
    is DomeneLagtTil<T> -> LagtTil(lagtTil)
    is DomeneUendret<T> -> Uendret(uendret)
}