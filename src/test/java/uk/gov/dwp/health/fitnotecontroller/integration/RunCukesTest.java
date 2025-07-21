package uk.gov.dwp.health.fitnotecontroller.integration;


import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerApplication;
import uk.gov.dwp.health.fitnotecontroller.application.FitnoteControllerConfiguration;

@RunWith(Cucumber.class)
@SuppressWarnings({"squid:S2187", "squid:S1118", "squid:S4042", "squid:S1192"})
@CucumberOptions(
        plugin = "json:target/cucumber-report.json", features = "src/test/resources/uk/gov/dwp/health/fitnotecontroller/integration/Declaration.feature")
public class RunCukesTest {
  private static final String CONFIG_FILE = "test.yml";

  @ClassRule
  public static final DropwizardAppRule<FitnoteControllerConfiguration> RULE =
          new DropwizardAppRule<>(FitnoteControllerApplication.class, resourceFilePath(CONFIG_FILE));


}
