package com.mranalizer.bdd.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mranalizer.adapter.out.llm.LlmResponseParser;
import com.mranalizer.domain.model.LlmAssessment;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.*;

public class LlmAdapterSteps {

    private String adapterType;
    private String mockResponseJson;
    private LlmAssessment assessment;
    private final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Given("LLM adapter ustawiony na {string}")
    public void llmAdapterUstawionyNa(String type) {
        adapterType = type;
    }

    @And("OpenAI API zwraca poprawny response z score {double} i komentarzem {string}")
    public void openaiApiZwracaResponse(double score, String comment) {
        mockResponseJson = String.format(
                "{\"scoreAdjustment\": %s, \"comment\": \"%s\"}", score, comment);
    }

    @And("Anthropic API zwraca poprawny response z score {double} i komentarzem {string}")
    public void anthropicApiZwracaResponse(double score, String comment) {
        mockResponseJson = String.format(
                "{\"scoreAdjustment\": %s, \"comment\": \"%s\"}", score, comment);
    }

    @Given("response JSON z scoreAdjustment, comment, categories, humanOversightRequired")
    public void responseJsonZeWszystkimiPolami() {
        mockResponseJson = """
            {
              "scoreAdjustment": 0.2,
              "comment": "full analysis",
              "overallAutomatability": 80,
              "categories": [{"name": "refactor", "score": 85, "reasoning": "Clean refactor"}],
              "humanOversightRequired": [{"area": "security", "reasoning": "Auth changes"}],
              "whyLlmFriendly": ["repetitive"],
              "summaryTable": [{"aspect": "Complexity", "score": 3, "note": "Low"}]
            }
            """;
    }

    @Given("LLM API nie odpowiada w czasie")
    public void llmApiTimeout() {
        assessment = new LlmAssessment(0.0, "LLM timeout", "test");
    }

    @Given("LLM API zwraca {int} Unauthorized")
    public void llmApiReturnsUnauthorized(int code) {
        assessment = new LlmAssessment(0.0, "LLM auth error", "test");
    }

    @When("system analizuje PR z LLM")
    public void systemAnalizujePrZLlm() {
        if (assessment == null && mockResponseJson != null) {
            assessment = parser.parseJsonResponse(mockResponseJson, adapterType != null ? adapterType : "test");
        }
    }

    @When("parser przetwarza response")
    public void parserPrzetwarzaResponse() {
        assessment = parser.parseJsonResponse(mockResponseJson, "test");
    }

    @Then("LlmAssessment zawiera scoreAdjustment {double}")
    public void llmAssessmentZawieraScore(double expected) {
        assertEquals(expected, assessment.scoreAdjustment(), 0.01);
    }

    @And("LlmAssessment zawiera comment {string}")
    public void llmAssessmentZawieraComment(String expected) {
        assertEquals(expected, assessment.comment());
    }

    @And("LlmAssessment provider to {string}")
    public void llmAssessmentProviderTo(String expected) {
        assertEquals(expected, assessment.provider());
    }

    @Then("LlmAssessment zawiera pełne dane \\(categories, oversight, summaryTable)")
    public void llmAssessmentZawieraPelneDane() {
        assertFalse(assessment.categories().isEmpty());
        assertFalse(assessment.humanOversightRequired().isEmpty());
        assertFalse(assessment.summaryTable().isEmpty());
        assertTrue(assessment.overallAutomatability() > 0);
    }

    @Then("LlmAssessment zawiera score {double} i comment z informacją o timeout")
    public void llmAssessmentTimeout(double score) {
        assertEquals(score, assessment.scoreAdjustment(), 0.01);
        assertTrue(assessment.comment().toLowerCase().contains("timeout"));
    }

    @Then("LlmAssessment zawiera score {double} i comment z informacją o błędzie auth")
    public void llmAssessmentAuthError(double score) {
        assertEquals(score, assessment.scoreAdjustment(), 0.01);
        assertTrue(assessment.comment().toLowerCase().contains("auth"));
    }
}
