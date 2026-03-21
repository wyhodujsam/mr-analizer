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
            "Analyze this Pull Request for LLM automation potential. " +
            "Rate from -0.5 to +0.5 how suitable it is for automated execution. " +
            "Respond in JSON: {\"scoreAdjustment\": 0.1, \"comment\": \"explanation\"}\n\n" +
            "Title: {{title}}\n" +
            "Description: {{description}}\n" +
            "Files changed: {{filesChanged}}\n" +
            "Additions: {{additions}}\n" +
            "Deletions: {{deletions}}\n" +
            "Has tests: {{hasTests}}\n" +
            "Labels: {{labels}}\n" +
            "Author: {{author}}\n" +
            "Source branch: {{sourceBranch}}\n" +
            "Target branch: {{targetBranch}}";

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
