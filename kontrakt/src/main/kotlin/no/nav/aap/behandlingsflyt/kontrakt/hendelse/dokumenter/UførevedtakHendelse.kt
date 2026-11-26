package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.UførevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

public sealed interface Uførevedtak : Melding {
    public val personId: String
    public val virkningsdato: LocalDate
    public val resultat: UførevedtakResultat
    public val avslag12_5: Boolean
    public val kilde: UførevedtakKilde

    public fun beskrivelseVurderingsbehov(): String
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class UførevedtakKafkaMelding(
    val personId: String,
    val virkningsdato: LocalDate,
    val resultat: UførevedtakResultat,
    @Suppress("PropertyName") val avslag12_5: Boolean,
) {
    public fun tilUføreVedtakV0(): Uførevedtak =
        UførevedtakV0(
            personId = personId,
            virkningsdato = virkningsdato,
            resultat = resultat,
            avslag12_5 = avslag12_5,
            kilde = UførevedtakKilde.PENSJON
        )

    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        return Innsending(
            saksnummer = saksnummer,
            referanse = InnsendingReferanse(UførevedtakReferanse.ny("${meldingKey}_${virkningsdato}")),
            type = InnsendingType.UFØRE_VEDTAK_HENDELSE,
            kanal = Kanal.DIGITAL,
            mottattTidspunkt = LocalDateTime.now(),
            melding = this.tilUføreVedtakV0()
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class UførevedtakKafkaMeldingV1(
    val personId: String,
    val virkningsdato: LocalDate,
    val resultat: UførevedtakResultat,
    @Suppress("PropertyName") val avslag12_5: Boolean,
    val vedtaksdato: LocalDate? = null,
) {
    public fun tilInnsending(meldingKey: String, saksnummer: Saksnummer): Innsending {
        val opprinnelig = UførevedtakKafkaMelding(personId, virkningsdato, resultat, avslag12_5)
            .tilInnsending(meldingKey, saksnummer)
        val melding = vedtaksdato?.let {
            UførevedtakV1(personId, virkningsdato, resultat, avslag12_5, UførevedtakKilde.PENSJON, it)
        } ?: opprinnelig.melding
        return Innsending(
            saksnummer = opprinnelig.saksnummer,
            referanse = opprinnelig.referanse,
            type = opprinnelig.type,
            kanal = opprinnelig.kanal,
            mottattTidspunkt = opprinnelig.mottattTidspunkt,
            melding = melding,
            digitalisertAvPostmottak = opprinnelig.digitalisertAvPostmottak
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class UførevedtakV0(
    override val personId: String,
    override val virkningsdato: LocalDate,
    override val resultat: UførevedtakResultat,
    @Suppress("PropertyName") override val avslag12_5: Boolean,
    override val kilde: UførevedtakKilde,
) : Uførevedtak {
    override fun beskrivelseVurderingsbehov(): String {
        val standardtekst = "Det er fattet et vedtak om uføre med status: ${resultat.verdi} fra $virkningsdato."
        return when {
            avslag12_5 -> "$standardtekst Vedtaket er fattet med hjemmel i § 12-5."
            else -> standardtekst
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class UførevedtakV1(
    override val personId: String,
    override val virkningsdato: LocalDate,
    override val resultat: UførevedtakResultat,
    @Suppress("PropertyName") override val avslag12_5: Boolean,
    override val kilde: UførevedtakKilde,
    val vedtaksdato: LocalDate,
) : Uførevedtak {
    override fun beskrivelseVurderingsbehov(): String =
        UførevedtakV0(personId, virkningsdato, resultat, avslag12_5, kilde).beskrivelseVurderingsbehov()
}

public enum class UførevedtakKilde {
    PENSJON,
}

public enum class UførevedtakResultat(public val verdi: String) {
    OPPH("Opphør"),
    INNV("Innvilgelse"),
    AVSL("Avslag"),
    ENDR("Endret"), ;

    public fun erOpphørEllerEndring(): Boolean = this == OPPH || this == ENDR
}

