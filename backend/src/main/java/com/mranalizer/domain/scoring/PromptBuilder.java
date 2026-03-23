package com.mranalizer.domain.scoring;

import com.mranalizer.domain.model.MergeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure domain class that builds an LLM prompt from a template and a MergeRequest.
 * No Spring dependencies — suitable for unit testing without a container.
 */
public class PromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(PromptBuilder.class);
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    public static final String DEFAULT_TEMPLATE =
            "Przeanalizuj tego Pull Requesta pod katem OPLACALNOSCI automatyzacji przez LLM (np. Claude Code). " +
            "Nie chodzi tylko o to czy LLM MOZE to zrobic, ale czy WARTO to automatyzowac. Odpowiadaj po polsku.\n\n" +
            "KLUCZOWE KRYTERIA OCENY:\n" +
            "1. KOSZT TOKENOW vs KOSZT RECZNY — LLM musi wczytac repozytorium, zrozumiec kontekst, wygenerowac zmiane. " +
            "Jesli zmiana to 1-5 linii w znanym pliku, developer zrobi to szybciej niz napisanie specyfikacji dla LLM.\n" +
            "2. TRUDNOSC NAPISANIA SPECYFIKACJI — jesli opisanie zadania dla LLM jest trudniejsze niz samo wykonanie " +
            "(np. 'zmien tytul na X' vs refaktoring 20 plikow), to automatyzacja nie ma sensu.\n" +
            "3. KONTEKST DEWELOPERSKI — punktowe zmiany (tytul, config, 1 linia) w duzym repozytorium sa latwiejsze " +
            "recznie dla developera ktory zna repo, niz dla LLM ktory musi odkryc kontekst.\n" +
            "4. SKALA ZMIANY — male zmiany (<10 linii, 1-2 pliki) to zwykle strata tokenow. " +
            "Automatyzacja oplaca sie od ~50 linii lub ~3 plikow, gdy jest wzorzec do powielenia.\n" +
            "5. POWTARZALNOSC — jesli zmiana jest jednorazowa (rename, config), LLM nie wnosi wartosci. " +
            "Jesli jest wzorzec (N plikow do zmiany wg tego samego schematu), LLM jest idealny.\n\n" +
            "PRZYKLADY NIOPLACALNYCH ZMIAN (scoreAdjustment ujemny):\n" +
            "- Zmiana tytulu/nazwy w 1 pliku — developer zrobi to w 10 sekund\n" +
            "- Zmiana jednej linii w konfiguracji — banalnie proste recznie\n" +
            "- Kosmetyczne poprawki (whitespace, formatowanie) — IDE zrobi to automatycznie\n" +
            "- Zmiana stalej/wartosci w 1 miejscu — wyszukaj-zamien\n\n" +
            "PRZYKLADY OPLACALNYCH ZMIAN (scoreAdjustment dodatni):\n" +
            "- Refaktoring wzorca w wielu plikach (np. CQRS split, nowa hierarchia wyjatkow)\n" +
            "- Dodanie testow do istniejacego kodu (LLM generuje testy z kontekstu)\n" +
            "- Migracja API/biblioteki w wielu miejscach\n" +
            "- Nowy feature z jasna specyfikacja (endpoint + model + testy)\n\n" +
            "Dane PR:\n" +
            "Tytul: {{title}}\n" +
            "Opis: {{description}}\n" +
            "Zmienione pliki: {{filesChanged}}\n" +
            "Dodane linie: {{additions}}\n" +
            "Usuniete linie: {{deletions}}\n" +
            "Zawiera testy: {{hasTests}}\n" +
            "Etykiety: {{labels}}\n" +
            "Autor: {{author}}\n" +
            "Branch zrodlowy: {{sourceBranch}}\n" +
            "Branch docelowy: {{targetBranch}}\n\n" +
            "Odpowiedz WYLACZNIE poprawnym obiektem JSON (bez markdown, bez dodatkowego tekstu) o dokladnie takiej strukturze:\n" +
            "{\n" +
            "  \"scoreAdjustment\": <liczba od -0.5 do 0.5 — UJEMNA jesli zmiana jest za mala/prosta zeby oplacalo sie ja automatyzowac>,\n" +
            "  \"comment\": \"<podsumowanie w 1-2 zdaniach — WYJASNI czy automatyzacja sie OPLACA, nie tylko czy jest mozliwa, po polsku>\",\n" +
            "  \"overallAutomatability\": <liczba calkowita 0-100 — UWZGLEDNIJ oplacalnosc: 1-liniowa zmiana = niski wynik mimo ze technicznie prosta>,\n" +
            "  \"categories\": [\n" +
            "    {\n" +
            "      \"name\": \"<kategoria zmian>\",\n" +
            "      \"score\": <liczba calkowita 0-100 — UWZGLEDNIJ koszt tokenow vs zysk>,\n" +
            "      \"reasoning\": \"<dlaczego taki wynik — wymien koszt tokenow, trudnosc specyfikacji, alternatywe reczna, po polsku>\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"humanOversightRequired\": [\n" +
            "    {\n" +
            "      \"area\": \"<co wymaga nadzoru czlowieka>\",\n" +
            "      \"reasoning\": \"<dlaczego LLM nie poradzi sobie sam, po polsku>\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"whyLlmFriendly\": [\"<powod 1, po polsku>\", \"<powod 2, po polsku>\"],\n" +
            "  \"summaryTable\": [\n" +
            "    {\n" +
            "      \"aspect\": \"<np. 'Wykonanie kodu', 'Podejmowanie decyzji', 'Pisanie testow', 'Review', 'Oplacalnosc automatyzacji'>\",\n" +
            "      \"score\": <liczba calkowita 0-100 lub null jesli nie da sie ocenic>,\n" +
            "      \"note\": \"<krotka uwaga — w aspekcie 'Oplacalnosc' wyjasnij koszt tokenow vs zysk, po polsku>\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    /**
     * Build a prompt by replacing placeholders in the template with values from the MergeRequest.
     *
     * @param template the prompt template with {{placeholder}} tokens; if null or blank, DEFAULT_TEMPLATE is used
     * @param mr       the merge request providing values
     * @return the fully resolved prompt string
     */
    public String build(String template, MergeRequest mr) {
        String effectiveTemplate = (template == null || template.isBlank()) ? DEFAULT_TEMPLATE : template;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(effectiveTemplate);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = resolve(placeholder, mr);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String resolve(String placeholder, MergeRequest mr) {
        return switch (placeholder) {
            case "title" -> safe(mr.getTitle());
            case "description" -> mr.getDescription() != null ? mr.getDescription() : "none";
            case "filesChanged" -> String.valueOf(mr.getDiffStats() != null ? mr.getDiffStats().changedFilesCount() : 0);
            case "additions" -> String.valueOf(mr.getDiffStats() != null ? mr.getDiffStats().additions() : 0);
            case "deletions" -> String.valueOf(mr.getDiffStats() != null ? mr.getDiffStats().deletions() : 0);
            case "hasTests" -> String.valueOf(mr.hasTests());
            case "labels" -> {
                List<String> labels = mr.getLabels();
                yield (labels != null && !labels.isEmpty()) ? String.join(", ", labels) : "none";
            }
            case "author" -> safe(mr.getAuthor());
            case "sourceBranch" -> safe(mr.getSourceBranch());
            case "targetBranch" -> safe(mr.getTargetBranch());
            default -> {
                log.warn("Unknown placeholder in prompt template: {{}}", placeholder);
                yield "";
            }
        };
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }
}
