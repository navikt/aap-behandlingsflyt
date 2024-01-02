package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.FatteVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.avklaringsbehov.student.AvklarStudentLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_SYKDOM_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FATTE_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.FORESLÅ_VEDTAK_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AVKLAR_STUDENT_KODE
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.MANUELT_SATT_PÅ_VENT_KODE


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(SattPåVentLøsning::class, name = MANUELT_SATT_PÅ_VENT_KODE),
    JsonSubTypes.Type(AvklarStudentLøsning::class, name = AVKLAR_STUDENT_KODE),
    JsonSubTypes.Type(AvklarSykdomLøsning::class, name = AVKLAR_SYKDOM_KODE),
    JsonSubTypes.Type(ForeslåVedtakLøsning::class, name = FORESLÅ_VEDTAK_KODE),
    JsonSubTypes.Type(FatteVedtakLøsning::class, name = FATTE_VEDTAK_KODE)
)
interface AvklaringsbehovLøsning {
    fun definisjon(): Definisjon {
        if (this.javaClass.isAnnotationPresent(JsonTypeName::class.java)) {
            return Definisjon.entries.first { it.kode == this.javaClass.getDeclaredAnnotation(JsonTypeName::class.java).value }
        }
        throw IllegalStateException("Utvikler-feil:" + this.javaClass.getSimpleName() + " er uten JsonTypeName annotation.")
    }
}