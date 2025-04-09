package com.helger.peppol.reporting.backend.sql;

import javax.annotation.Nonnull;

import com.helger.config.IConfig;
import com.helger.db.api.flyway.FlywayConfigurationBuilderConfig;

/**
 * The specific Flyway Configuration builder for Peppol Reporting.
 *
 * @author Philip Helger
 * @since 3.0.4
 */
public class ReportingFlywayConfigurationBuilder extends FlywayConfigurationBuilderConfig
{
  public static final String FLYWAY_CONFIG_PREFIX = "peppol.reporting.flyway.";

  public ReportingFlywayConfigurationBuilder (@Nonnull final IConfig aConfig,
                                              @Nonnull final ReportingJdbcConfiguration aJdbcConfig)
  {
    super (aConfig, FLYWAY_CONFIG_PREFIX);

    // Fallback to other configuration values
    if (jdbcUrl () == null)
      jdbcUrl (aJdbcConfig.getJdbcUrl ());
    if (jdbcUser () == null)
      jdbcUser (aJdbcConfig.getJdbcUser ());
    if (jdbcPassword () == null)
      jdbcPassword (aJdbcConfig.getJdbcPassword ());
  }
}
