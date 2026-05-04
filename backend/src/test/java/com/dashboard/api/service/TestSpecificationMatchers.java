package com.dashboard.api.service;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;

final class TestSpecificationMatchers {

    private TestSpecificationMatchers() {
    }

    @SuppressWarnings({"unchecked", "null"})
    @NonNull
    static <T> Specification<T> anySpecification() {
        return (Specification<T>) org.mockito.ArgumentMatchers.any(Specification.class);
    }
}
