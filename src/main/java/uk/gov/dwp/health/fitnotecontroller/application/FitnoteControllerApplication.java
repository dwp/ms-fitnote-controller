package uk.gov.dwp.health.fitnotecontroller.application;

import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;

import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import uk.gov.dwp.health.crypto.CryptoDataManager;
import uk.gov.dwp.health.crypto.MessageEncoder;
import uk.gov.dwp.health.crypto.exception.CryptoException;
import uk.gov.dwp.health.fitnotecontroller.FitnoteAddressResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteConfirmationResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteDeclarationResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteQueryResource;
import uk.gov.dwp.health.fitnotecontroller.FitnoteSubmitResource;
import uk.gov.dwp.health.fitnotecontroller.ImageStorage;
import uk.gov.dwp.health.fitnotecontroller.handlers.MsDataMatrixCreatorHandler;
import uk.gov.dwp.health.messageq.amazon.sns.MessagePublisher;
import uk.gov.dwp.health.version.HealthCheckResource;
import uk.gov.dwp.health.version.ServiceInfoResource;
import uk.gov.dwp.health.version.info.PropertyFileInfoProvider;
import uk.gov.dwp.tls.TLSConnectionBuilder;

public class FitnoteControllerApplication extends Application<FitnoteControllerConfiguration> {

  @Override
  protected void bootstrapLogging() {
    // to prevent dropwizard using its own standard logger
  }

  @Override
  public void run(
          FitnoteControllerConfiguration fitnoteControllerConfiguration, Environment environment)
          throws Exception {

    CryptoDataManager mqKmsCrypto = null;
    if (fitnoteControllerConfiguration.isSnsEncryptMessages()
            && null == fitnoteControllerConfiguration.getSnsKmsCryptoConfiguration()) {
      throw new CryptoException(
              "SnsEncryptMessages is TRUE.  "
              + "Cannot encrypt without a valid 'snsKmsCryptoConfiguration' configuration item");

    } else if (fitnoteControllerConfiguration.isSnsEncryptMessages()) {
      mqKmsCrypto =
              new CryptoDataManager(fitnoteControllerConfiguration.getSnsKmsCryptoConfiguration());
    }

    CryptoDataManager redisMqKmsCrypto = null;
    if (fitnoteControllerConfiguration.isRedisEncryptMessages()
            && null == fitnoteControllerConfiguration.getRedisKmsCryptoConfiguration()) {
      throw new CryptoException(
              "RedisEncryptMessages is TRUE.  "
              + "Cannot encrypt without a valid 'redisKmsCryptoConfiguration' configuration item");

    } else if (fitnoteControllerConfiguration.isRedisEncryptMessages()) {
      redisMqKmsCrypto =
              new CryptoDataManager(
                      fitnoteControllerConfiguration.getRedisKmsCryptoConfiguration());
    }

    RedisClusterClient redisClient = null;
    if (fitnoteControllerConfiguration.isRedisEncryptionTransit()) {
      RedisURI redisUri = RedisURI
              .create("rediss://" + fitnoteControllerConfiguration.getRedisStoreURI());
      redisClient = RedisClusterClient.create(redisUri);
    } else {
      redisClient = RedisClusterClient
              .create("redis://" + fitnoteControllerConfiguration.getRedisStoreURI());
    }

    final ImageStorage imageStorage =
            new ImageStorage(fitnoteControllerConfiguration, redisClient, redisMqKmsCrypto);

    final MessageEncoder<MessageAttributeValue> messageEncoder =
            new MessageEncoder<>(mqKmsCrypto, MessageAttributeValue.class);
    final MessagePublisher snsPublisher =
            new MessagePublisher(messageEncoder,
                    fitnoteControllerConfiguration.getSnsConfiguration());

    TLSConnectionBuilder connectionBuilder =
            new TLSConnectionBuilder(
                    fitnoteControllerConfiguration.getDataMatrixCreatorTruststoreFile(),
                    fitnoteControllerConfiguration.getDataMatrixCreatorTruststorePass(),
                    fitnoteControllerConfiguration.getDataMatrixCreatorKeystoreFile(),
                    fitnoteControllerConfiguration.getMsDataMatrixCreatorKeystorePass());

    final MsDataMatrixCreatorHandler msDataMatrixCreatorHandler =
            new MsDataMatrixCreatorHandler(connectionBuilder, fitnoteControllerConfiguration);

    final FitnoteSubmitResource resource =
            new FitnoteSubmitResource(fitnoteControllerConfiguration, imageStorage,
                    msDataMatrixCreatorHandler);
    final FitnoteConfirmationResource confirmationResource =
            new FitnoteConfirmationResource(imageStorage);
    final FitnoteAddressResource addressResource = new FitnoteAddressResource(imageStorage);
    final FitnoteQueryResource queryResource = new FitnoteQueryResource(imageStorage);
    final FitnoteDeclarationResource declarationResource =
            new FitnoteDeclarationResource(
                    imageStorage, snsPublisher, fitnoteControllerConfiguration);

    environment.jersey().register(resource);
    environment.jersey().register(confirmationResource);
    environment.jersey().register(declarationResource);
    environment.jersey().register(addressResource);
    environment.jersey().register(queryResource);

    environment.jersey().register(new HealthCheckResource());

    if (fitnoteControllerConfiguration.isApplicationInfoEnabled()) {
      environment.jersey()
              .register(
                      new ServiceInfoResource(
                              new PropertyFileInfoProvider("application.yml")
                      ));
    }
  }

  @Override
  public void initialize(Bootstrap<FitnoteControllerConfiguration> bootstrap) {
    bootstrap.setConfigurationSourceProvider(
            new SubstitutingSourceProvider(
                    bootstrap.getConfigurationSourceProvider(),
                    new EnvironmentVariableSubstitutor(false)));
  }

  public static void main(String[] args) throws Exception {
    new FitnoteControllerApplication().run(args);
  }
}
