Lookup a definition
Narrative:
In order to talk better
As an English student
I want to look up word definitions


Scenario: Looking up the definition of a few words
Given the user is on the Wikionary home page
When the user looks up the definition of the word searchDefinition
Then they should see the definition <searchResult>

Examples:
| searchDefinition | searchResult                                                                                   |
| pear             | An edible fruit produced by the pear tree, similar to an apple but elongated towards the stem. |

