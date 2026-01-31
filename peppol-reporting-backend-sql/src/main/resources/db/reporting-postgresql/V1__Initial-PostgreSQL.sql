--
-- Copyright (C) 2023-2026 Philip Helger
-- philip[at]helger[dot]com
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--         http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE peppol_reporting_item (
  exchangedt timestamp(3) NOT NULL,
  sending    boolean      NOT NULL,
  c2id       varchar(64)  NOT NULL,
  c3id       varchar(64)  NOT NULL,
  dtscheme   varchar(64)  NOT NULL,
  dtvalue    varchar(500) NOT NULL,
  procscheme varchar(64)  NOT NULL,
  procvalue  varchar(200) NOT NULL,
  tp         varchar(64)  NOT NULL,
  c1cc       varchar(2)   NOT NULL,
  c4cc       varchar(2)   DEFAULT NULL,
  enduserid  varchar(256) NOT NULL
);

CREATE INDEX peppol_reporting_item_idx ON peppol_reporting_item (exchangedt);
