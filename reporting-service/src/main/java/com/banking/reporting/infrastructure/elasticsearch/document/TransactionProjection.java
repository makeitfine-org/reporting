package com.banking.reporting.infrastructure.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "transaction_projections")
@Setting(settingPath = "elasticsearch/transaction-projections-settings.json")
public class TransactionProjection {

    @Id
    private String transactionId;

    @Field(type = FieldType.Keyword)
    private String clientId;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Keyword)
    private String paymentStatus;

    @Field(type = FieldType.Keyword)
    private String loanId;

    @Field(type = FieldType.Date)
    private Instant transactedAt;

    @Field(type = FieldType.Nested)
    private ProductDetails productDetails;

    @Field(type = FieldType.Keyword)
    private String clientName;

    @Field(type = FieldType.Keyword)
    private String region;
}
