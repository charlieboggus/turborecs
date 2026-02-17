package com.github.charlieboggus.turborecs.common.enums

enum class Dimension(val displayName: String, val low: String, val high: String) {
    EMOTIONAL_INTENSITY("Emotional Intensity", "Detached, cerebral", "Devastating, gut-punch"),
    NARRATIVE_COMPLEXITY("Narrative Complexity", "Linear, straightforward", "Non-linear, multi-threaded"),
    MORAL_AMBIGUITY("Moral Ambiguity", "Clear heroes/villains", "Ethical gray zones"),
    TONE_DARKNESS("Tone Darkness", "Light, optimistic", "Bleak, nihilistic"),
    PACING("Pacing", "Slow-burn, meditative", "Relentless, propulsive"),
    HUMOR("Humor", "Dead serious", "Pervasively funny"),
    VIOLENCE_INTENSITY("Violence Intensity", "Non-violent, gentle", "Graphic, visceral"),
    INTELLECTUAL_DEPTH("Intellectual Depth", "Entertainment-first", "Ideas-driven, demanding"),
    STYLISTIC_BOLDNESS("Stylistic Boldness", "Conventional craft", "Highly stylized, experimental"),
    INTIMACY_SCALE("Intimacy vs Scale", "Epic scope, grand sweep", "Claustrophobic, interior"),
    REALISM("Realism", "Fantastical, surreal", "Grounded, naturalistic"),
    CULTURAL_SPECIFICITY("Cultural Specificity", "Universal/generic", "Deep in specific time/place");

    companion object {
        /** Ordered list of SNAKE_CASE names for prompt generation */
        val NAMES: List<String> = entries.map { it.name }
    }
}