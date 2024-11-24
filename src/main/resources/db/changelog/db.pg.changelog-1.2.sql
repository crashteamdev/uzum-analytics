CREATE UNIQUE INDEX IF NOT EXISTS seller_id_account_id_link_idx ON sellers (seller_id, account_id, link);

DROP INDEX IF EXISTS sellers_link_idx;
