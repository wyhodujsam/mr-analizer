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
            "Przeanalizuj tego Pull Requesta pod katem potencjalu automatyzacji przez LLM (np. Claude Code). " +
            "Podaj szczegolowa, strukturyzowana ocene. Odpowiadaj po polsku.\n\n" +
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
            "  \"scoreAdjustment\": <liczba od -0.5 do 0.5>,\n" +
            "  \"comment\": \"<podsumowanie w 1-2 zdaniach, po polsku>\",\n" +
            "  \"overallAutomatability\": <liczba calkowita 0-100, procent szansy ze LLM dobrze to wykona>,\n" +
            "  \"categories\": [\n" +
            "    {\n" +
            "      \"name\": \"<kategoria zmian, np. 'Podzial klasy CQRS', 'Hierarchia wyjatkow'>\",\n" +
            "      \"score\": <liczba calkowita 0-100, na ile ta kategoria jest automatyzowalna>,\n" +
            "      \"reasoning\": \"<dlaczego taki wynik — wymien konkretne wzorce kodu, technologie lub architekture, po polsku>\"\n" +
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
            "      \"aspect\": \"<np. 'Wykonanie kodu', 'Podejmowanie decyzji', 'Pisanie testow', 'Review'>\",\n" +
            "      \"score\": <liczba calkowita 0-100 lub null jesli nie da sie ocenic>,\n" +
            "      \"note\": \"<krotka uwaga, po polsku>\"\n" +
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
            case "hasTests" -> String.valueOf(mr.isHasTests());
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
