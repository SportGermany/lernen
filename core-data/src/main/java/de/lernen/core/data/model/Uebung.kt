package de.lernen.core.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Eine Übungsaufgabe. Der Typ steht im Feld "typ". Unbekannte Typen werden als [Unbekannt] gelesen. */
@Serializable(with = UebungSerializer::class)
sealed interface Uebung {
    /** Optionale feste id aus dem Paket; sonst wird "{stufe}-{index}" verwendet. */
    val id: String?

    data class Auswahl(
        override val id: String?,
        val frage: String,
        val optionen: List<String>,
        val richtig: Int,
        val warum: String,
    ) : Uebung

    data class Tippen(
        override val id: String?,
        val frage: String,
        val tokens: List<Token>,
        val warum: String,
    ) : Uebung

    data class Umschreiben(
        override val id: String?,
        val frage: String,
        val satz: String,
        val tipp: String?,
        val loesung: String,
    ) : Uebung

    data class Text(
        override val id: String?,
        val titel: String,
        val vlabel: String?,
        val auftrag: String,
        val vorlage: String?,
        val loesung: String,
    ) : Uebung

    /** Rechenaufgabe (Mathe/Physik): Aufgabe, Musterlösung, optional Ergebnis und Tipp. */
    data class Rechnen(
        override val id: String?,
        val aufgabe: String,
        val loesung: String,
        val ergebnis: String?,
        val tipp: String?,
    ) : Uebung

    /** Typ, den diese App-Version noch nicht kennt (z. B. ein neuer Typ aus einem neueren Paket). */
    data class Unbekannt(override val id: String?, val typ: String, val roh: JsonObject) : Uebung
}

/** Ein antippbarer Baustein: ein Wort oder ein ganzer Satz. */
data class Token(val text: String, val richtig: Boolean)

object UebungSerializer : KSerializer<Uebung> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Uebung")

    override fun deserialize(decoder: Decoder): Uebung {
        val input = decoder as? JsonDecoder ?: error("Nur JSON wird unterstützt")
        val o = input.decodeJsonElement() as? JsonObject ?: throw IllegalArgumentException("Übung ist kein Objekt")
        val id = o.str("id")
        return when (val typ = o.str("typ")) {
            "auswahl" -> Uebung.Auswahl(
                id, o.req("frage"), o.strList("optionen"),
                o["richtig"]?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("auswahl: 'richtig' fehlt"),
                o.str("warum") ?: "",
            )
            "tippen" -> Uebung.Tippen(id, o.req("frage"), o.tokens(), o.str("warum") ?: "")
            "umschreiben" -> Uebung.Umschreiben(id, o.req("frage"), o.req("satz"), o.str("tipp"), o.req("loesung"))
            "text" -> Uebung.Text(id, o.req("titel"), o.str("vlabel"), o.req("auftrag"), o.str("vorlage"), o.req("loesung"))
            "rechenaufgabe" -> Uebung.Rechnen(id, o.req("aufgabe"), o.req("loesung"), o.str("ergebnis"), o.str("tipp"))
            else -> Uebung.Unbekannt(id, typ ?: "", o)
        }
    }

    override fun serialize(encoder: Encoder, value: Uebung) {
        val out = encoder as? JsonEncoder ?: error("Nur JSON wird unterstützt")
        out.encodeJsonElement(zuJson(value))
    }

    private fun zuJson(u: Uebung): JsonElement {
        val m = linkedMapOf<String, JsonElement>()
        fun put(k: String, v: String?) { if (v != null) m[k] = JsonPrimitive(v) }
        when (u) {
            is Uebung.Auswahl -> {
                put("typ", "auswahl"); put("id", u.id); put("frage", u.frage)
                m["optionen"] = JsonArray(u.optionen.map(::JsonPrimitive))
                m["richtig"] = JsonPrimitive(u.richtig); put("warum", u.warum)
            }
            is Uebung.Tippen -> {
                put("typ", "tippen"); put("id", u.id); put("frage", u.frage)
                m["tokens"] = JsonArray(u.tokens.map { JsonArray(listOf(JsonPrimitive(it.text), JsonPrimitive(if (it.richtig) 1 else 0))) })
                put("warum", u.warum)
            }
            is Uebung.Umschreiben -> {
                put("typ", "umschreiben"); put("id", u.id); put("frage", u.frage); put("satz", u.satz)
                put("tipp", u.tipp); put("loesung", u.loesung)
            }
            is Uebung.Text -> {
                put("typ", "text"); put("id", u.id); put("titel", u.titel); put("vlabel", u.vlabel)
                put("auftrag", u.auftrag); put("vorlage", u.vorlage); put("loesung", u.loesung)
            }
            is Uebung.Rechnen -> {
                put("typ", "rechenaufgabe"); put("id", u.id); put("aufgabe", u.aufgabe); put("loesung", u.loesung)
                put("ergebnis", u.ergebnis); put("tipp", u.tipp)
            }
            is Uebung.Unbekannt -> return u.roh
        }
        return JsonObject(m)
    }

    private fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.contentOrNull
    private fun JsonObject.req(k: String): String = str(k) ?: throw IllegalArgumentException("Übung: Feld '$k' fehlt")
    private fun JsonObject.strList(k: String): List<String> =
        (this[k] as? JsonArray)?.map { it.jsonPrimitive.content } ?: throw IllegalArgumentException("Übung: Liste '$k' fehlt")

    private fun JsonObject.tokens(): List<Token> =
        (this["tokens"] as? JsonArray ?: throw IllegalArgumentException("tippen: 'tokens' fehlt")).map { el ->
            val a = el.jsonArray
            val p = a[1].jsonPrimitive
            Token(a[0].jsonPrimitive.content, p.intOrNull?.let { it != 0 } ?: p.booleanOrNull ?: false)
        }
}
