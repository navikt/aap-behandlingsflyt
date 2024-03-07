package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE
import no.nav.aap.behandlingsflyt.avklaringsbehov.FASTSETT_BEREGNINGSTIDSPUNKT_KODE


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(SattPåVentLøsning::class, name = MANUELT_SATT_PÅ_VENT_KODE),
    JsonSubTypes.Type(AvklarStudentLøsning::class, name = AVKLAR_STUDENT_KODE),
    JsonSubTypes.Type(AvklarSykdomLøsning::class, name = AVKLAR_SYKDOM_KODE),
    JsonSubTypes.Type(ForeslåVedtakLøsning::class, name = FORESLÅ_VEDTAK_KODE),
    JsonSubTypes.Type(FatteVedtakLøsning::class, name = FATTE_VEDTAK_KODE),
    JsonSubTypes.Type(FatteVedtakLøsning::class, name = FASTSETT_BEREGNINGSTIDSPUNKT_KODE)
)
interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first { it.kode == this.javaClass.getDeclaredAnnotation(JsonTypeName::class.java).value }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }
}
