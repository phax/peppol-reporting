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

  public ReportingFlywayConfigurationBuilder (@Nonnull final IConfig aConfig)
  {
    super (aConfig, FLYWAY_CONFIG_PREFIX);

    // Fallback to other configuration values
    if (jdbcUrl () == null)
      jdbcUrl (ReportingJdbcConfiguration.getJdbcUrl (aConfig));
    if (jdbcUser () == null)
      jdbcUser (ReportingJdbcConfiguration.getJdbcUser (aConfig));
    if (jdbcPassword () == null)
      jdbcUser (ReportingJdbcConfiguration.getJdbcPassword (aConfig));
  }
}
