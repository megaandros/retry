package com.serenity.steps;

import net.thucydides.core.annotations.Steps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.serenity.steps.serenity.EndUserSteps;

public class DefinitionSteps {

    @Steps
    EndUserSteps endUser;

    @Given("the user is on the Wikionary home page")
    public void givenTheUserIsOnTheWikionaryHomePage() {
        endUser.is_the_home_page();
    }

    @When("the user looks up the definition of the word $searchDefinition")
    public void whenTheUserLooksUpTheDefinitionOf(String searchDefinition) {
        endUser.looks_for(searchDefinition);
    }

    @Then("they should see the definition $searchResult")
    public void thenTheyShouldSeeADefinitionContainingTheWords(String searchResult) {
        endUser.should_see_definition(searchResult);
    }



}
