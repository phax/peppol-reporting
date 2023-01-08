# peppol-reporting

Peppol Reporting support library.
Peppol Reporting is the process of collecting, aggregating and transmitting Peppol Reports to OpenPeppol. 

This library is work in progress and will be adopted to potential Reporting specification changes.

It supports the following reports:
* Peppol Transaction Statistics Report 1.0.0 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))
* Peppol End User Statistics Report 1.0.0-RC2 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))

This library does not deal with the transmission of Reports. That needs to be done with [phase4](https://github.com/phax/phase4) or another AS4 solution.

Glossary:
* EUSR - End User Statistics Report
* TSR - Transaction Statistics Report
* Report - Document containing OpenPeppol reporting information
* Reporting - The process of transmitting a **Report** to OpenPeppol
