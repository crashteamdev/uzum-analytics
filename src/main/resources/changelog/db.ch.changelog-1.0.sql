--liquibase formatted sql
--changeset vitaxa:create-uzum-db
CREATE DATABASE IF NOT EXISTS uzum;

--changeset vitaxa:ke-tables
DROP TABLE IF EXISTS uzum.product;
CREATE TABLE uzum.product
(
    timestamp              DateTime,
    productId              String,
    skuId                  String,
    title                  String,
    rating                 Decimal32(2),
    categoryPath           Array(UInt64),
    reviewsAmount          UInt32,
    totalOrdersAmount      UInt64,
    totalAvailableAmount   UInt64,
    availableAmount        UInt64,
    charityCommission      UInt32,
    attributes             Array(String),
    tags                   Array(String),
    photoKey               String,
    characteristics        Map(String, String),
    sellerId               UInt64,
    sellerAccountId        UInt64,
    sellerTitle            String,
    sellerLink             String,
    sellerRegistrationDate DateTime,
    sellerRating           Decimal32(2),
    sellerReviewsCount     UInt64,
    sellerOrders           UInt64,
    sellerOfficial         UInt8,
    sellerContacts         Map(String, String),
    isEco                  UInt8,
    isPerishable           UInt8,
    hasVerticalPhoto       UInt8,
    showKitty              UInt8,
    bonusProduct           UInt8,
    adultCategory          UInt8,
    colorPhotoPreview      UInt8,
    fullPrice              Nullable(UInt64),
    purchasePrice          UInt64,
    charityProfit          UInt64,
    barcode                String,
    vatType                String,
    vatAmount              UInt64,
    vatPrice               UInt64
) ENGINE = MergeTree()
      PARTITION BY toYYYYMM(timestamp)
      PRIMARY KEY (productId, skuId)
      ORDER BY (productId, skuId, timestamp)
