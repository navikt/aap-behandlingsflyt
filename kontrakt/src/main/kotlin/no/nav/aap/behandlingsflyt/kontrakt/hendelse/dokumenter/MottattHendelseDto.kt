package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime

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
    public val melding: Melding? = null,
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

            InnsendingType.MELDEKORT -> {
                require(InnsendingReferanse.Type.JOURNALPOST == referanse.type)
                requireNotNull(melding)
                require(melding is Meldekort)
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

            InnsendingType.ANNET_RELEVANT_DOKUMENT -> {
                require(referanse.type == InnsendingReferanse.Type.JOURNALPOST)
                requireNotNull(melding)
                require(melding is AnnetRelevantDokument)
            }

            InnsendingType.MANUELL_REVURDERING -> {
                require(referanse.type == InnsendingReferanse.Type.REVURDERING_ID)
                requireNotNull(melding)
                require(melding is ManuellRevurdering)
            }
            InnsendingType.OMGJØRING_KLAGE_REVURDERING -> {
                require(referanse.type == InnsendingReferanse.Type.REVURDERING_ID)
                requireNotNull(melding)
                require(melding is OmgjøringKlageRevurdering)
            }

            InnsendingType.KLAGE -> {
                require(referanse.type == InnsendingReferanse.Type.JOURNALPOST || referanse.type == InnsendingReferanse.Type.MANUELL_OPPRETTELSE)
                require(melding is Klage)
            }

            InnsendingType.NY_ÅRSAK_TIL_BEHANDLING -> {
                require(referanse.type == InnsendingReferanse.Type.SAKSBEHANDLER_KELVIN_REFERANSE)
                require(melding is NyÅrsakTilBehandling)
            }

            InnsendingType.KABAL_HENDELSE -> {
                require(referanse.type == InnsendingReferanse.Type.KABAL_HENDELSE_ID)
                requireNotNull(melding) {"Melding fra Kabal kan ikke være null"}
                require(melding is KabalHendelse)
            }

            InnsendingType.OPPFØLGINGSOPPGAVE -> {
                requireNotNull(melding)
            }

            InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER, InnsendingType.PDL_HENDELSE_DODSFALL_BARN -> {
                require(referanse.type == InnsendingReferanse.Type.PDL_HENDELSE_ID)
                requireNotNull(melding) {"Melding for dødsfall kan ikke være null"}
                require(melding is PdlHendelse)
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

