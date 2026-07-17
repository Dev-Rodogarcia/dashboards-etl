package com.dashboard.api.client.esl;

public final class EslGraphqlOperations {

    public static final String QUOTE_CREATE = "quoteCreate";
    public static final String PICK_CREATE = "pickCreate";
    public static final String PICK_UPDATE = "pickUpdate";
    public static final String PICK_CANCELLATION = "pickCancellation";
    public static final String PICK_LIST = "pickList";
    public static final String PICK_LIST_RESULT = "pick";
    public static final String INVOICE_LIST = "invoice";
    public static final String COMPANY_LIST = "company";

    public static final String MUTATION_QUOTE_CREATE = """
            mutation quoteCreate($params: QuoteCreateInput!) {
              quoteCreate(params: $params) {
                success
                errors
                resource {
                  id
                  sequenceCode
                  referenceNumber
                  effectiveUntil
                  printUrl
                  bidsPendingCount
                  quoteStretchBids { total }
                }
              }
            }
            """;

    public static final String MUTATION_PICK_CREATE = """
            mutation pickCreate($params: PickMutationInput!) {
              pickCreate(params: $params) {
                success
                errors
                resource {
                  id
                  sequenceCode
                  status
                  requestDate
                  requestHour
                  serviceDate
                  serviceStartHour
                  serviceEndHour
                  referenceNumber
                  comments
                  invoicesValue
                  invoicesVolumes
                  invoicesWeight
                  taxedWeight
                }
              }
            }
            """;

    public static final String MUTATION_PICK_UPDATE = """
            mutation pickUpdate($id: ID!, $params: PickMutationInput!) {
              pickUpdate(id: $id, params: $params) {
                success
                errors
                resource {
                  id
                  sequenceCode
                  status
                  requestDate
                  requestHour
                  serviceDate
                  serviceStartHour
                  serviceEndHour
                  referenceNumber
                  comments
                }
              }
            }
            """;

    public static final String MUTATION_PICK_CANCELLATION = """
            mutation pickCancellation($id: ID!, $params: PickCancellationInput!) {
              pickCancellation(id: $id, params: $params) {
                success
                errors
                resource {
                  id
                  sequenceCode
                  status
                  cancellationReason
                  requestDate
                  serviceDate
                  referenceNumber
                }
              }
            }
            """;

    public static final String QUERY_PICK_LIST = """
            query pickList($params: PickInput!, $after: String, $first: Int!) {
              pick(params: $params, after: $after, first: $first) {
                edges {
                  cursor
                  node {
                    id
                    sequenceCode
                    status
                    requestDate
                    requestHour
                    serviceDate
                    serviceStartHour
                    serviceEndHour
                    referenceNumber
                    cancellationReason
                    comments
                  }
                }
                pageInfo { hasNextPage endCursor }
              }
            }
            """;

    public static final String QUERY_INVOICE_LIST = """
            query invoiceList($params: InvoiceQueryInput, $first: Int) {
              invoice(params: $params, first: $first) {
                edges {
                  node {
                    id
                    key
                    number
                    series
                    issuedAt
                    status
                    value
                    weight
                    volume
                  }
                }
              }
            }
            """;

    public static final String QUERY_COMPANY_LIST = """
            query company($params: CompanyInput!, $after: String, $first: Int!) {
              company(params: $params, after: $after, first: $first) {
                edges {
                  cursor
                  node {
                    cnpj
                    name
                    nickname
                    corporation {
                      id
                      person { cnpj }
                    }
                  }
                }
                pageInfo { hasNextPage endCursor }
              }
            }
            """;

    private EslGraphqlOperations() {
    }
}
