package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.UførevedtakReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDate
import java.time.LocalDateTime

public sealed interface Uførevedtak : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class UførevedtakKafkaMelding(
    val personId: String,
    val virkningsdato: LocalDate,
    val resultat: UførevedtakResultat,
    val avslag12_5: Boolean,
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
public data class UførevedtakV0(
    val personId: String,
    val virkningsdato: LocalDate,
    val resultat: UførevedtakResultat,
    val avslag12_5: Boolean,
    val kilde: UførevedtakKilde,
) : Uførevedtak {
    public fun beskrivelseVurderingsbehov(): String {
        val standardtekst = "Det er fattet et vedtak om uføre med status: ${resultat.verdi} fra $virkningsdato."
        return when {
            avslag12_5 -> "$standardtekst Vedtaket er fattet med hjemmel i § 12-5."
            else -> standardtekst
        }
    }
}

public enum class UførevedtakKilde {
    PENSJON,
}

public enum class UførevedtakResultat(public val verdi: String) {
    OPPH ("Opphør"),
    INNV ("Innvilgelse"),
    AVSL ("Avslag"),
    ENDR("Endret"), ;

    public fun erOpphørEllerEndring(): Boolean = this == OPPH || this == ENDR
}

