package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime

@Deprecated(message = "Bruk Innsending i stedet", replaceWith = ReplaceWith("Innsending"))
public data class MottattHendelseDto(
    val saksnummer: Saksnummer,
    val type: InnsendingType,
    val kanal: Kanal,
    val hendelseId: InnsendingReferanse,
    val payload: Any?
)

/**
 * @param saksnummer Hvilken sak innsendingen skal knyttes.
 * @param referanse Referanse til hvordan hendte ut innsendingen igjen. Dette avhenger av [type].
 * @param type Hvilken innsendingtype. [referanse] og [type] går hånd i hånd.
 * @param kanal Om innsendingen kom via digitale kanaler eller via papir. Dette er relevant for statistikkformål.
 * @param mottattTidspunkt Tidspunktet da dokumentet ble mottatt.
 * @param melding Strukturert melding.
 */
public class Innsending(
    public val saksnummer: Saksnummer,
    public val referanse: InnsendingReferanse,
    public val type: InnsendingType,
    public val kanal: Kanal = Kanal.DIGITAL,
    public val mottattTidspunkt: LocalDateTime,
    public val melding: Melding?,
) {
    init {
        when (type) {
            InnsendingType.SØKNAD -> {
                requireNotNull(melding)
                require(melding is Søknad)
                require(referanse.type == InnsendingReferanse.Type.JOURNALPOST)
            }

            InnsendingType.AKTIVITETSKORT -> {
                requireNotNull(melding)
                require(melding is Aktivitetskort)
                require(referanse.type == InnsendingReferanse.Type.BRUDD_AKTIVITETSPLIKT_INNSENDING_ID)
            }

            InnsendingType.PLIKTKORT -> {
                require(InnsendingReferanse.Type.JOURNALPOST == referanse.type)
                requireNotNull(melding)
                require(melding is Pliktkort)
            }

            InnsendingType.LEGEERKLÆRING -> {
                require(melding == null) { "Legeerklæring har ikke payload." }
            }

            InnsendingType.LEGEERKLÆRING_AVVIST -> {
                require(InnsendingType.LEGEERKLÆRING_AVVIST == type)
                require(melding == null) { "Avvist legeerklæring har ikke payload." }
            }

            InnsendingType.DIALOGMELDING -> {
                require(melding == null) { "Dialogmelding har ikke payload. Kun journalpost-ID." }
            }
        }
    }
}

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "meldingType",
    visible = true,
    // For å håndtere at nåværende instanser av Søknad-objektet ikke har meldingType-feltet
    defaultImpl = SøknadV0::class,
)
public sealed interface Melding


/**
 * Eksempel på hvordan håndtere meldingtyper.
 */
@Suppress("unused")
private fun example(innsending: Innsending) {
    when (innsending.melding) {
        is Søknad -> when (innsending.melding) {
            is SøknadV0 -> TODO()
        }

        is Pliktkort -> when (innsending.melding) {
            is PliktkortV0 -> TODO()
        }

        is Aktivitetskort -> TODO()
        null -> TODO()
    }
}
