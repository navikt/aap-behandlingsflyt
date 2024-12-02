package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktFeilregistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRegistrering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.AktivitetspliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Brudd
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddType
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Grunn
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.type.Periode

class OppdaterAktivitetspliktDTOV2(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
    val grunn: GrunnDTO,
    val begrunnelse: String,
) {
    fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd.nyttBrudd(
            sak = sak,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        if (grunn == GrunnDTO.FEILREGISTRERING) {
            return listOf(
                AktivitetspliktRepository.FeilregistreringInput(
                    brudd = brudd,
                    innsender = innsender,
                    begrunnelse = begrunnelse,
                )
            )
        }
        return listOf(AktivitetspliktRepository.RegistreringInput(
            brudd = brudd,
            innsender = innsender,
            begrunnelse = begrunnelse,
            grunn = grunn.tilDomene(),
        ))
    }
}


interface AktivitetspliktDTO {
    fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput>
}

data class OpprettAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf?,
    val begrunnelse: String,
    val grunn: GrunnDTO?,
    val perioder: List<Periode>,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        require(grunn != GrunnDTO.FEILREGISTRERING)
        return perioder.map { periode ->
            val brudd = Brudd.nyttBrudd(
                sak = sak,
                periode = periode,
                bruddType = brudd,
                paragraf = brudd.paragraf(paragraf),
            )
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = begrunnelse,
                grunn = grunn?.tilDomene() ?: Grunn.INGEN_GYLDIG_GRUNN,
            )
        }
    }
}

data class OppdaterAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
    val grunn: GrunnDTO,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd.nyttBrudd(
            sak = sak,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )

        if (grunn == GrunnDTO.FEILREGISTRERING) {
            return listOf(
                AktivitetspliktRepository.FeilregistreringInput(
                    brudd = brudd,
                    innsender = innsender,
                    begrunnelse = "",
                )
            )
        }

        return listOf(
            AktivitetspliktRepository.RegistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = "",
                grunn = grunn.tilDomene(),
            )
        )
    }
}

data class FeilregistrerAktivitetspliktDTO(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val periode: Periode,
) : AktivitetspliktDTO {
    override fun tilDomene(sak: Sak, innsender: Bruker): List<AktivitetspliktRepository.DokumentInput> {
        val brudd = Brudd.nyttBrudd(
            sak = sak,
            periode = periode,
            bruddType = brudd,
            paragraf = brudd.paragraf(paragraf),
        )
        return listOf(
            AktivitetspliktRepository.FeilregistreringInput(
                brudd = brudd,
                innsender = innsender,
                begrunnelse = "",
            )
        )
    }
}

data class BruddAktivitetspliktResponse(
    val hendelser: List<BruddAktivitetspliktHendelseDto>
)

data class SaksnummerParameter(@PathParam("saksnummer") val saksnummer: String)

data class BruddAktivitetspliktHendelseDto(
    val brudd: BruddType,
    val paragraf: Brudd.Paragraf,
    val grunn: GrunnDTO,
    val periode: Periode,
    val begrunnelse: String,
)

fun List<AktivitetspliktDokument>.utledBruddTilstand(): List<BruddAktivitetspliktHendelseDto> {
    return this
        .groupBy { it.brudd }
        .mapNotNull { (_, brudd) ->
            requireNotNull(brudd.maxByOrNull { it.metadata.opprettetTid }) {
                "bug: skal ikke få null fra liste vi fikk fra groupBy "
            }
        }
        .sortedBy { it.brudd.periode }
        .map {
            BruddAktivitetspliktHendelseDto(
                brudd = it.brudd.bruddType,
                paragraf = it.brudd.paragraf,
                grunn = when (it) {
                    is AktivitetspliktFeilregistrering -> GrunnDTO.FEILREGISTRERING
                    is AktivitetspliktRegistrering -> GrunnDTO.fraDomene(it.grunn)
                },
                periode = it.brudd.periode,
                begrunnelse = when (it) {
                    is AktivitetspliktFeilregistrering -> it.begrunnelse
                    is AktivitetspliktRegistrering -> it.begrunnelse
                }
            )
        }
}

enum class GrunnDTO(private val tilDomene: Grunn?) {
    SYKDOM_ELLER_SKADE(Grunn.SYKDOM_ELLER_SKADE),
    STERKE_VELFERDSGRUNNER(Grunn.STERKE_VELFERDSGRUNNER),
    RIMELIG_GRUNN(Grunn.RIMELIG_GRUNN),
    INGEN_GYLDIG_GRUNN(Grunn.INGEN_GYLDIG_GRUNN),
    FEILREGISTRERING(null);

    fun tilDomene(): Grunn {
        return requireNotNull(tilDomene) {
            "$this kan ikke representeres som en grunn i domenet"
        }
    }

    companion object {
        fun fraDomene(grunn: Grunn): GrunnDTO {
            return requireNotNull(entries.find { it.tilDomene == grunn }) {
                "$grunn kan ikke representeres som GrunnDTO"
            }
        }
    }
}
